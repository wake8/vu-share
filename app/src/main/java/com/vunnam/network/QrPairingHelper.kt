package com.vunnam.network

import android.graphics.Bitmap
import android.graphics.Color
import com.vunnam.model.QrPairingData
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

object QrPairingHelper {

    fun createPairingToken(
        deviceId: String,
        nickname: String,
        avatarEmoji: String,
        hostIp: String,
        port: Int,
        validityMinutes: Long = 10
    ): QrPairingData {
        return QrPairingData(
            version = 1,
            deviceId = deviceId,
            nickname = nickname,
            avatarEmoji = avatarEmoji,
            hostIp = hostIp,
            port = port,
            token = UUID.randomUUID().toString().take(12),
            expiresAt = System.currentTimeMillis() + (validityMinutes * 60 * 1000)
        )
    }

    fun encodeToJson(data: QrPairingData): String {
        return "{\"v\":${data.version},\"id\":\"${data.deviceId}\",\"name\":\"${data.nickname}\",\"emoji\":\"${data.avatarEmoji}\",\"ip\":\"${data.hostIp}\",\"port\":${data.port},\"tok\":\"${data.token}\",\"exp\":${data.expiresAt}}"
    }

    fun parseFromJson(jsonString: String): QrPairingData? {
        return try {
            fun extractString(key: String): String? {
                val match = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(jsonString)
                return match?.groupValues?.get(1)
            }
            fun extractLong(key: String): Long? {
                val match = Regex("\"$key\"\\s*:\\s*(\\d+)").find(jsonString)
                return match?.groupValues?.get(1)?.toLongOrNull()
            }

            val expiresAt = extractLong("exp") ?: 0L
            if (expiresAt > 0 && expiresAt < System.currentTimeMillis()) {
                // Token has expired!
                return null
            }

            val id = extractString("id") ?: return null
            val name = extractString("name") ?: "Peer"
            val emoji = extractString("emoji") ?: "🚀"
            val ip = extractString("ip") ?: return null
            val port = extractLong("port")?.toInt() ?: 8988
            val token = extractString("tok") ?: ""
            val version = extractLong("v")?.toInt() ?: 1

            QrPairingData(
                version = version,
                deviceId = id,
                nickname = name,
                avatarEmoji = emoji,
                hostIp = ip,
                port = port,
                token = token,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
