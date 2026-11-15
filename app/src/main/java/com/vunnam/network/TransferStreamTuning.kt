package com.vunnam.network

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.Socket
import java.security.MessageDigest

object TransferStreamTuning {

    const val MIN_BUFFER_BYTES = 64 * 1024          // 64 KB
    const val MEDIUM_BUFFER_BYTES = 256 * 1024      // 256 KB
    const val LARGE_BUFFER_BYTES = 512 * 1024       // 512 KB
    const val MAX_BUFFER_BYTES = 1024 * 1024        // 1 MB
    const val PROGRESS_THROTTLE_MS = 200L
    const val PROTOCOL_VERSION = 2

    data class ReceiveStreamResult(
        val bytesReceived: Long,
        val sha256: String,
        val verified: Boolean,
        val completed: Boolean
    )

    /**
     * Computes an adaptive I/O buffer size based on payload size, concurrency, and heap budget.
     */
    fun selectAdaptiveBufferSize(
        fileSize: Long,
        totalSessionBytes: Long = fileSize,
        maxHeapBytes: Long = Runtime.getRuntime().maxMemory(),
        concurrentStreams: Int = 1
    ): Int {
        val effectiveScale = maxOf(fileSize, totalSessionBytes / 2L)
        val desired = when {
            effectiveScale < 1L * 1024L * 1024L -> MIN_BUFFER_BYTES
            effectiveScale < 32L * 1024L * 1024L -> MEDIUM_BUFFER_BYTES
            concurrentStreams <= 1 && maxHeapBytes >= 256L * 1024L * 1024L -> MAX_BUFFER_BYTES
            else -> LARGE_BUFFER_BYTES
        }
        val rawCap = maxHeapBytes / 16L / concurrentStreams.coerceAtLeast(1)
        val alignedCap = (rawCap / MIN_BUFFER_BYTES.toLong()) * MIN_BUFFER_BYTES.toLong()
        val safePerStreamCap = alignedCap
            .coerceIn(MIN_BUFFER_BYTES.toLong(), MAX_BUFFER_BYTES.toLong())
            .toInt()
        return desired.coerceAtMost(safePerStreamCap).coerceAtLeast(MIN_BUFFER_BYTES)
    }

    /**
     * Tunes TCP socket options for sustained high-throughput local P2P transfers.
     */
    fun configureSocket(socket: Socket, bufferSize: Int) {
        val targetSockBuf = maxOf(bufferSize, MEDIUM_BUFFER_BYTES)
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.soTimeout = 30_000
            socket.sendBufferSize = targetSockBuf
            socket.receiveBufferSize = targetSockBuf
            socket.setPerformancePreferences(0, 1, 2)
        } catch (_: Exception) {
            // Some OEM/virtual sockets ignore optional tuning hints; safe to continue
        }
    }

    fun formatSha256(digestBytes: ByteArray): String {
        val hexChars = CharArray(digestBytes.size * 2)
        val digits = "0123456789abcdef"
        for (i in digestBytes.indices) {
            val v = digestBytes[i].toInt() and 0xFF
            hexChars[i * 2] = digits[v ushr 4]
            hexChars[i * 2 + 1] = digits[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Streams a file in a single pass while simultaneously computing its SHA-256 checksum
     * and writing the checksum trailer immediately after the file payload.
     */
    fun sendStreamSinglePassSha256(
        rawInput: InputStream,
        output: DataOutputStream,
        fileSize: Long,
        resumeOffset: Long = 0L,
        buffer: ByteArray,
        isPaused: () -> Boolean = { false },
        isCancelled: () -> Boolean = { false },
        onProgress: (bytesSent: Long, speedBps: Long, etaSeconds: Long) -> Unit = { _, _, _ -> }
    ): String? {
        val bufferedInput = if (rawInput is BufferedInputStream) rawInput else BufferedInputStream(rawInput, buffer.size)
        val digest = MessageDigest.getInstance("SHA-256")
        var totalRead = 0L

        // Hash skipped prefix if resuming from an interrupted transfer so full-file SHA-256 still matches
        while (totalRead < resumeOffset && !isCancelled()) {
            val toRead = minOf(resumeOffset - totalRead, buffer.size.toLong()).toInt()
            val read = bufferedInput.read(buffer, 0, toRead)
            if (read == -1) break
            digest.update(buffer, 0, read)
            totalRead += read
        }

        var lastReportTime = System.currentTimeMillis()
        var lastReportedBytes = totalRead
        var smoothedSpeed = 0L

        while (!isCancelled()) {
            while (isPaused() && !isCancelled()) {
                Thread.sleep(100)
                lastReportTime = System.currentTimeMillis()
                lastReportedBytes = totalRead
            }
            if (isCancelled()) break

            val bytesRead = bufferedInput.read(buffer)
            if (bytesRead == -1) break

            output.write(buffer, 0, bytesRead)
            digest.update(buffer, 0, bytesRead)
            totalRead += bytesRead

            val now = System.currentTimeMillis()
            val elapsed = now - lastReportTime
            if (elapsed >= PROGRESS_THROTTLE_MS || (fileSize > 0 && totalRead >= fileSize)) {
                val intervalMs = elapsed.coerceAtLeast(1L)
                val deltaBytes = (totalRead - lastReportedBytes).coerceAtLeast(0L)
                val instantSpeed = (deltaBytes * 1000L) / intervalMs
                smoothedSpeed = if (smoothedSpeed == 0L) {
                    instantSpeed
                } else {
                    ((instantSpeed * 35L) + (smoothedSpeed * 65L)) / 100L
                }
                val remaining = (fileSize - totalRead).coerceAtLeast(0L)
                val eta = if (smoothedSpeed > 0L) remaining / smoothedSpeed else 0L
                onProgress(totalRead, smoothedSpeed, eta)
                lastReportTime = now
                lastReportedBytes = totalRead
            }
        }

        output.flush()
        if (isCancelled()) return null

        val sha256Hex = formatSha256(digest.digest())
        output.writeUTF(sha256Hex)
        output.flush()
        return sha256Hex
    }

    /**
     * Receives a streamed file into a buffered `.vupart` file while computing SHA-256 in a single pass,
     * reading the sender's SHA-256 trailer (protocol v2), and verifying integrity.
     */
    fun receiveStreamSinglePassSha256(
        input: DataInputStream,
        partFile: File,
        expectedSize: Long,
        resumeOffset: Long = 0L,
        hasV2Trailer: Boolean = true,
        legacyExpectedSha256: String? = null,
        buffer: ByteArray,
        isPaused: () -> Boolean = { false },
        isCancelled: () -> Boolean = { false },
        onProgress: (bytesReceived: Long, speedBps: Long, etaSeconds: Long) -> Unit = { _, _, _ -> }
    ): ReceiveStreamResult {
        val digest = MessageDigest.getInstance("SHA-256")
        var fileBytesRead = 0L
        var appendMode = false

        if (resumeOffset > 0L && partFile.exists() && partFile.length() >= resumeOffset) {
            if (partFile.length() > resumeOffset) {
                RandomAccessFile(partFile, "rw").use { raf ->
                    raf.setLength(resumeOffset)
                }
            }
            BufferedInputStream(FileInputStream(partFile), buffer.size).use { existingIn ->
                var hashed = 0L
                while (hashed < resumeOffset) {
                    val toRead = minOf(resumeOffset - hashed, buffer.size.toLong()).toInt()
                    val r = existingIn.read(buffer, 0, toRead)
                    if (r == -1) break
                    digest.update(buffer, 0, r)
                    hashed += r
                }
                fileBytesRead = hashed
            }
            appendMode = true
        }

        var lastReportTime = System.currentTimeMillis()
        var lastReportedBytes = fileBytesRead
        var smoothedSpeed = 0L

        BufferedOutputStream(FileOutputStream(partFile, appendMode), buffer.size).use { bos ->
            while (fileBytesRead < expectedSize && !isCancelled()) {
                while (isPaused() && !isCancelled()) {
                    Thread.sleep(100)
                    lastReportTime = System.currentTimeMillis()
                    lastReportedBytes = fileBytesRead
                }
                if (isCancelled()) break

                val toRead = minOf(expectedSize - fileBytesRead, buffer.size.toLong()).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read == -1) break

                bos.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                fileBytesRead += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastReportTime
                if (elapsed >= PROGRESS_THROTTLE_MS || fileBytesRead >= expectedSize) {
                    val intervalMs = elapsed.coerceAtLeast(1L)
                    val deltaBytes = (fileBytesRead - lastReportedBytes).coerceAtLeast(0L)
                    val instantSpeed = (deltaBytes * 1000L) / intervalMs
                    smoothedSpeed = if (smoothedSpeed == 0L) {
                        instantSpeed
                    } else {
                        ((instantSpeed * 35L) + (smoothedSpeed * 65L)) / 100L
                    }
                    val remaining = (expectedSize - fileBytesRead).coerceAtLeast(0L)
                    val eta = if (smoothedSpeed > 0L) remaining / smoothedSpeed else 0L
                    onProgress(fileBytesRead, smoothedSpeed, eta)
                    lastReportTime = now
                    lastReportedBytes = fileBytesRead
                }
            }
            bos.flush()
        }

        val completed = !isCancelled() && fileBytesRead == expectedSize
        val actualHash = formatSha256(digest.digest())
        val expectedHash = if (completed && hasV2Trailer) {
            input.readUTF()
        } else {
            legacyExpectedSha256
        }
        val verified = completed && (expectedHash.isNullOrBlank() || expectedHash.equals(actualHash, ignoreCase = true))

        return ReceiveStreamResult(
            bytesReceived = fileBytesRead,
            sha256 = actualHash,
            verified = verified,
            completed = completed
        )
    }
}
