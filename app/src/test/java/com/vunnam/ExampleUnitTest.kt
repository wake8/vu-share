package com.vunnam

import com.vunnam.network.NetworkUtils
import com.vunnam.network.QrPairingHelper
import com.vunnam.preferences.DeviceIdentityManager
import com.vunnam.storage.FileSecuritySanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class ExampleUnitTest {

    @Test
    fun testFilenameSanitizationPathTraversal() {
        val dangerousInput = "../../../etc/passwd"
        val sanitized = FileSecuritySanitizer.sanitizeFilename(dangerousInput)
        assertEquals("passwd", sanitized)

        val windowsTraversal = "..\\..\\windows\\system32\\cmd.exe"
        val sanitizedWin = FileSecuritySanitizer.sanitizeFilename(windowsTraversal)
        assertEquals("cmd.exe", sanitizedWin)

        val invalidChars = "bad:file*name?.txt"
        val sanitizedChars = FileSecuritySanitizer.sanitizeFilename(invalidChars)
        assertFalse(sanitizedChars.contains(":"))
        assertFalse(sanitizedChars.contains("*"))
        assertFalse(sanitizedChars.contains("?"))
    }

    @Test
    fun testSha256ChecksumVerification() {
        val testData = "VU Share Offline P2P Transfer Engine Test".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        val hash = FileSecuritySanitizer.calculateSha256(inputStream)
        assertNotNull(hash)
        assertEquals(64, hash.length)

        // Same data produces same hash
        val hash2 = FileSecuritySanitizer.calculateSha256(ByteArrayInputStream(testData))
        assertEquals(hash, hash2)
    }

    @Test
    fun testFunnyNicknamesPool() {
        assertTrue("Pool should have at least 100 names", DeviceIdentityManager.FUNNY_NICKNAMES.size >= 100)
        val name1 = DeviceIdentityManager.getRandomNickname()
        assertNotNull(name1)
        assertTrue(name1.isNotEmpty())
    }

    @Test
    fun testQrPairingTokenEncodeAndDecode() {
        val tokenData = QrPairingHelper.createPairingToken(
            deviceId = "dev_12345",
            nickname = "Dancing Potato 🥔",
            avatarEmoji = "🥔",
            hostIp = "192.168.1.50",
            port = 8988,
            validityMinutes = 5
        )

        val json = QrPairingHelper.encodeToJson(tokenData)
        val parsed = QrPairingHelper.parseFromJson(json)

        assertNotNull(parsed)
        assertEquals(tokenData.deviceId, parsed!!.deviceId)
        assertEquals(tokenData.nickname, parsed.nickname)
        assertEquals(tokenData.hostIp, parsed.hostIp)
        assertEquals(tokenData.port, parsed.port)
        assertEquals(tokenData.token, parsed.token)
    }

    @Test
    fun testExpiredQrPairingToken() {
        val expiredJson = "{\"v\":1,\"id\":\"dev_1\",\"name\":\"Old Device\",\"emoji\":\"🚀\",\"ip\":\"192.168.1.1\",\"port\":8988,\"tok\":\"123\",\"exp\":${System.currentTimeMillis() - 10000}}"
        val parsed = QrPairingHelper.parseFromJson(expiredJson)
        assertNull("Expired token should be rejected", parsed)
    }

    @Test
    fun testNetworkUtilsFormatting() {
        assertEquals("0 B", NetworkUtils.formatFileSize(0))
        assertEquals("1 KB", NetworkUtils.formatFileSize(1024))
        assertEquals("1 MB", NetworkUtils.formatFileSize(1024 * 1024))
        assertEquals("10 MB/s", NetworkUtils.formatSpeed(10 * 1024 * 1024))
        assertEquals("30s left", NetworkUtils.formatEta(30))
        assertEquals("2m 15s left", NetworkUtils.formatEta(135))
    }
}
