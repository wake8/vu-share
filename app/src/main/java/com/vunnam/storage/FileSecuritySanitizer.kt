package com.vunnam.storage

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object FileSecuritySanitizer {

    /**
     * Sanitizes incoming filenames from remote devices to prevent directory traversal
     * or malicious file naming.
     */
    fun sanitizeFilename(rawName: String): String {
        // Strip path traversal attempts and directory separators
        var clean = rawName.replace("\\", "/")
        clean = File(clean).name // Extracts just the filename, stripping any preceding paths

        // Remove dangerous characters and control chars
        clean = clean.replace(Regex("[\\x00-\\x1F\\x7F/<>:\"|?*]"), "_")
        clean = clean.trim()

        // Prevent hidden files and empty strings
        if (clean.startsWith(".")) {
            clean = "received_$clean"
        }
        if (clean.isBlank()) {
            clean = "received_file_${System.currentTimeMillis()}"
        }

        // Limit maximum filename length
        if (clean.length > 200) {
            val ext = clean.substringAfterLast(".", "")
            val base = clean.substringBeforeLast(".")
            clean = if (ext.isNotEmpty() && ext.length <= 10) {
                "${base.take(190)}.$ext"
            } else {
                clean.take(200)
            }
        }

        return clean
    }

    /**
     * Computes the SHA-256 checksum of an InputStream while reading it.
     */
    fun calculateSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes the SHA-256 checksum of a local File.
     */
    fun calculateFileSha256(file: File): String {
        return file.inputStream().use { calculateSha256(it) }
    }

    /**
     * Resolves a unique destination file to prevent overwriting existing files.
     */
    fun getUniqueDestinationFile(targetDir: File, desiredFilename: String): File {
        val sanitized = sanitizeFilename(desiredFilename)
        var candidate = File(targetDir, sanitized)
        if (!candidate.exists()) {
            return candidate
        }

        val nameWithoutExt = sanitized.substringBeforeLast(".")
        val extension = if (sanitized.contains(".")) ".${sanitized.substringAfterLast(".")}" else ""

        var counter = 1
        while (candidate.exists()) {
            candidate = File(targetDir, "${nameWithoutExt}_$counter$extension")
            counter++
        }
        return candidate
    }
}
