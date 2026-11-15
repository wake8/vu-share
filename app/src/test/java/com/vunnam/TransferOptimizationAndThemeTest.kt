package com.vunnam

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.vunnam.network.TransferStreamTuning
import com.vunnam.storage.FileSecuritySanitizer
import com.vunnam.ui.theme.buildVuColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class TransferOptimizationAndThemeTest {

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = fg.luminance().toDouble()
        val l2 = bg.luminance().toDouble()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testAdaptiveBufferSizing() {
        val small = TransferStreamTuning.selectAdaptiveBufferSize(
            fileSize = 256L * 1024L,
            totalSessionBytes = 256L * 1024L,
            maxHeapBytes = 512L * 1024L * 1024L,
            concurrentStreams = 1
        )
        assertEquals(64 * 1024, small)

        val medium = TransferStreamTuning.selectAdaptiveBufferSize(
            fileSize = 10L * 1024L * 1024L,
            totalSessionBytes = 10L * 1024L * 1024L,
            maxHeapBytes = 512L * 1024L * 1024L,
            concurrentStreams = 1
        )
        assertEquals(256 * 1024, medium)

        val largeSingle = TransferStreamTuning.selectAdaptiveBufferSize(
            fileSize = 100L * 1024L * 1024L,
            totalSessionBytes = 100L * 1024L * 1024L,
            maxHeapBytes = 512L * 1024L * 1024L,
            concurrentStreams = 1
        )
        assertEquals(1024 * 1024, largeSingle)

        val constrainedMemory = TransferStreamTuning.selectAdaptiveBufferSize(
            fileSize = 100L * 1024L * 1024L,
            totalSessionBytes = 100L * 1024L * 1024L,
            maxHeapBytes = 4L * 1024L * 1024L,
            concurrentStreams = 3
        )
        assertEquals(64 * 1024, constrainedMemory)
    }

    @Test
    fun testLoopbackSinglePassStreamingAndBenchmark() {
        val tempDir = Files.createTempDirectory("vushare_bench").toFile()
        try {
            val payloadSize = 16 * 1024 * 1024 // 16 MB real payload
            val sourceFile = File(tempDir, "sample_16mb.bin")
            val chunk = ByteArray(64 * 1024) { (it % 251).toByte() }
            FileOutputStream(sourceFile).use { fos ->
                var written = 0
                while (written < payloadSize) {
                    val toWrite = min(chunk.size, payloadSize - written)
                    fos.write(chunk, 0, toWrite)
                    written += toWrite
                }
            }
            val expectedHash = FileSecuritySanitizer.calculateFileSha256(sourceFile)

            // 1. Measure Legacy Unbuffered + Double-Read Pattern
            val legacyStartNs = System.nanoTime()
            run {
                val legacyDest = File(tempDir, "legacy_out.bin")
                val executor = Executors.newSingleThreadExecutor()
                ServerSocket(0).use { server ->
                    val recvFuture = executor.submit<String> {
                        server.accept().use { client ->
                            val input = DataInputStream(client.getInputStream())
                            val size = input.readLong()
                            val preHash = input.readUTF()
                            val digest = MessageDigest.getInstance("SHA-256")
                            val buf = ByteArray(64 * 1024)
                            var readTotal = 0L
                            FileOutputStream(legacyDest).use { fos ->
                                while (readTotal < size) {
                                    val r = input.read(buf, 0, min(buf.size.toLong(), size - readTotal).toInt())
                                    if (r == -1) break
                                    fos.write(buf, 0, r)
                                    digest.update(buf, 0, r)
                                    readTotal += r
                                }
                                fos.flush()
                            }
                            val actual = TransferStreamTuning.formatSha256(digest.digest())
                            assertEquals(preHash, actual)
                            actual
                        }
                    }

                    Socket("127.0.0.1", server.localPort).use { client ->
                        // Legacy read #1: pre-compute SHA-256 before sending
                        val preHash = FileSecuritySanitizer.calculateFileSha256(sourceFile)
                        val output = DataOutputStream(client.getOutputStream())
                        output.writeLong(sourceFile.length())
                        output.writeUTF(preHash)
                        // Legacy read #2: stream unbuffered
                        FileInputStream(sourceFile).use { fis ->
                            val buf = ByteArray(64 * 1024)
                            var r: Int
                            while (fis.read(buf).also { r = it } != -1) {
                                output.write(buf, 0, r)
                            }
                            output.flush()
                        }
                    }
                    assertEquals(expectedHash, recvFuture.get())
                }
                executor.shutdown()
            }
            val legacyMs = (System.nanoTime() - legacyStartNs) / 1_000_000.0

            // 2. Measure Optimized Single-Pass Buffered + Tuned Socket Pattern
            val optStartNs = System.nanoTime()
            run {
                val optDest = File(tempDir, "opt_out.bin")
                val bufSize = TransferStreamTuning.selectAdaptiveBufferSize(sourceFile.length(), sourceFile.length())
                val executor = Executors.newSingleThreadExecutor()
                ServerSocket(0).use { server ->
                    val recvFuture = executor.submit<TransferStreamTuning.ReceiveStreamResult> {
                        server.accept().use { client ->
                            TransferStreamTuning.configureSocket(client, bufSize)
                            val input = DataInputStream(BufferedInputStream(client.getInputStream(), bufSize))
                            val size = input.readLong()
                            val recvBuf = ByteArray(bufSize)
                            TransferStreamTuning.receiveStreamSinglePassSha256(
                                input = input,
                                partFile = optDest,
                                expectedSize = size,
                                hasV2Trailer = true,
                                buffer = recvBuf
                            )
                        }
                    }

                    Socket("127.0.0.1", server.localPort).use { client ->
                        TransferStreamTuning.configureSocket(client, bufSize)
                        val output = DataOutputStream(BufferedOutputStream(client.getOutputStream(), bufSize))
                        output.writeLong(sourceFile.length())
                        val sendBuf = ByteArray(bufSize)
                        FileInputStream(sourceFile).use { fis ->
                            val sentHash = TransferStreamTuning.sendStreamSinglePassSha256(
                                rawInput = fis,
                                output = output,
                                fileSize = sourceFile.length(),
                                buffer = sendBuf
                            )
                            assertEquals(expectedHash, sentHash)
                        }
                    }
                    val res = recvFuture.get()
                    assertTrue(res.completed)
                    assertTrue(res.verified)
                    assertEquals(expectedHash, res.sha256)
                }
                executor.shutdown()
            }
            val optMs = (System.nanoTime() - optStartNs) / 1_000_000.0
            println("LOOPBACK_BENCHMARK_16MB: legacy=${String.format("%.2f", legacyMs)}ms, optimized=${String.format("%.2f", optMs)}ms")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testInterruptedTransferResumePreservesSha256Integrity() {
        val tempDir = Files.createTempDirectory("vushare_resume").toFile()
        try {
            val totalSize = 2 * 1024 * 1024 // 2 MB
            val sourceFile = File(tempDir, "resume_src.bin")
            val data = ByteArray(totalSize) { ((it * 31) % 256).toByte() }
            sourceFile.writeBytes(data)
            val expectedHash = FileSecuritySanitizer.calculateFileSha256(sourceFile)

            val partFile = File(tempDir, ".vupart_resume_src.bin_$totalSize")
            val resumeOffset = 900_000L
            // Simulate first 900 KB already written before drop
            partFile.writeBytes(data.copyOfRange(0, resumeOffset.toInt()))

            val bufSize = TransferStreamTuning.selectAdaptiveBufferSize(totalSize.toLong(), totalSize.toLong())
            val executor = Executors.newSingleThreadExecutor()
            ServerSocket(0).use { server ->
                val recvFuture = executor.submit<TransferStreamTuning.ReceiveStreamResult> {
                    server.accept().use { client ->
                        val input = DataInputStream(BufferedInputStream(client.getInputStream(), bufSize))
                        TransferStreamTuning.receiveStreamSinglePassSha256(
                            input = input,
                            partFile = partFile,
                            expectedSize = totalSize.toLong(),
                            resumeOffset = resumeOffset,
                            hasV2Trailer = true,
                            buffer = ByteArray(bufSize)
                        )
                    }
                }

                Socket("127.0.0.1", server.localPort).use { client ->
                    val output = DataOutputStream(BufferedOutputStream(client.getOutputStream(), bufSize))
                    FileInputStream(sourceFile).use { fis ->
                        TransferStreamTuning.sendStreamSinglePassSha256(
                            rawInput = fis,
                            output = output,
                            fileSize = totalSize.toLong(),
                            resumeOffset = resumeOffset,
                            buffer = ByteArray(bufSize)
                        )
                    }
                }

                val res = recvFuture.get()
                assertTrue(res.completed)
                assertTrue(res.verified)
                assertEquals(expectedHash, res.sha256)
                assertEquals(totalSize.toLong(), partFile.length())
            }
            executor.shutdown()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testTamperedChecksumFailsVerification() {
        val tempDir = Files.createTempDirectory("vushare_tamper").toFile()
        try {
            val payload = "Authentic VU Share payload".toByteArray()
            val partFile = File(tempDir, "tampered.vupart")
            val executor = Executors.newSingleThreadExecutor()
            ServerSocket(0).use { server ->
                val recvFuture = executor.submit<TransferStreamTuning.ReceiveStreamResult> {
                    server.accept().use { client ->
                        val input = DataInputStream(client.getInputStream())
                        TransferStreamTuning.receiveStreamSinglePassSha256(
                            input = input,
                            partFile = partFile,
                            expectedSize = payload.size.toLong(),
                            hasV2Trailer = true,
                            buffer = ByteArray(4096)
                        )
                    }
                }
                Socket("127.0.0.1", server.localPort).use { client ->
                    val out = DataOutputStream(client.getOutputStream())
                    out.write(payload)
                    out.writeUTF("0000000000000000000000000000000000000000000000000000000000000000")
                    out.flush()
                }
                val res = recvFuture.get()
                assertTrue(res.completed)
                assertFalse("Corrupted checksum must fail verification", res.verified)
            }
            executor.shutdown()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testModernizedLightThemeExactPaletteAndWcagContrastAllThemes() {
        val light = buildVuColors("LIGHT", "CYAN", systemIsDark = false)
        assertTrue(light.isLight)
        assertEquals(Color(0xFFF1F3F8), light.background)
        assertEquals(Color(0xFFFFFFFF), light.primaryCard)
        assertEquals(Color(0xFFF7F8FC), light.secondaryCard)
        assertEquals(Color(0xFFE9EDF5), light.inputBackground)
        assertEquals(Color(0xFF202A3A), light.primaryText)
        assertEquals(Color(0xFF2868E8), light.primaryAccent)
        assertEquals(Color(0xFFDCE8FF), light.secondaryAccent)
        assertEquals(Color(0xFFDFE4EF), light.border)
        assertEquals(Color(0xFFE8EBF2), light.divider)
        assertEquals(Color(0xFFE6E9F0), light.disabledSurface)

        val themes = listOf(
            "LIGHT", "MINIMAL_LIGHT", "DARK", "MIDNIGHT_BLUE",
            "AMOLED_BLACK", "AURORA", "EMERALD", "SYSTEM"
        )
        val accents = listOf("CYAN", "VIOLET", "EMERALD", "CORAL", "GOLD", "ROSE")

        for (theme in themes) {
            for (sysDark in listOf(false, true)) {
                for (accent in accents) {
                    val c = buildVuColors(theme, accent, sysDark)
                    assertTrue(
                        "$theme/$accent primaryText on background contrast < 4.5",
                        contrastRatio(c.primaryText, c.background) >= 4.5
                    )
                    assertTrue(
                        "$theme/$accent primaryText on primaryCard contrast < 4.5",
                        contrastRatio(c.primaryText, c.primaryCard) >= 4.5
                    )
                    assertTrue(
                        "$theme/$accent secondaryText on primaryCard contrast < 4.5",
                        contrastRatio(c.secondaryText, c.primaryCard) >= 4.5
                    )
                    assertTrue(
                        "$theme/$accent secondaryText on background contrast < 4.5",
                        contrastRatio(c.secondaryText, c.background) >= 4.5
                    )
                    assertTrue(
                        "$theme/$accent onPrimaryAccent on primaryAccent contrast < 4.5",
                        contrastRatio(c.onPrimaryAccent, c.primaryAccent) >= 4.5
                    )
                    assertTrue(
                        "$theme/$accent sendCardTitle on sendCardBg contrast < 4.5",
                        contrastRatio(c.sendCardTitle, c.sendCardBg) >= 4.5
                    )
                    assertTrue(
                        "$theme/$accent receiveCardTitle on receiveCardBg contrast < 4.5",
                        contrastRatio(c.receiveCardTitle, c.receiveCardBg) >= 4.5
                    )
                }
            }
        }
    }
}
