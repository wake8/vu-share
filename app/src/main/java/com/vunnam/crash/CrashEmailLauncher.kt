package com.vunnam.crash

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

sealed class CrashEmailLaunchResult {
    data object Opened : CrashEmailLaunchResult()
    data object NoEmailApp : CrashEmailLaunchResult()
    data class Failed(val reason: String) : CrashEmailLaunchResult()
}

/**
 * Builds and launches an email intent with prepopulated recipient, subject, body,
 * and plain-text crash attachment via FileProvider without sending anything automatically.
 */
object CrashEmailLauncher {

    fun createAttachmentFile(context: Context, report: CrashReport): File? {
        return try {
            val attachDir = File(context.cacheDir, "crash_attachments")
            if (!attachDir.exists()) attachDir.mkdirs()
            // Clean previous temp attachment files
            attachDir.listFiles()?.forEach { it.delete() }
            val name = if (report.isTestReport) {
                CrashReportingConfig.TEST_ATTACHMENT_FILE_NAME
            } else {
                CrashReportingConfig.ATTACHMENT_FILE_NAME
            }
            val file = File(attachDir, name)
            file.writeText(CrashReportFormatter.buildAttachmentContent(report), Charsets.UTF_8)
            file
        } catch (_: Throwable) {
            null
        }
    }

    fun launchEmailClient(
        context: Context,
        report: CrashReport,
        recipient: String = CrashReportingConfig.DEFAULT_DEVELOPER_EMAIL
    ): CrashEmailLaunchResult {
        val subject = CrashReportFormatter.buildEmailSubject(report)
        val fullBody = CrashReportFormatter.buildEmailBody(report, includeFullStackTrace = true)
        val attachmentFile = createAttachmentFile(context, report)
        val attachmentUri: Uri? = attachmentFile?.let { file ->
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.getOrNull()
        }

        // Preferred Android pattern: ACTION_SEND with mailto selector restricts target strictly to email apps
        // while preserving EXTRA_STREAM attachment support.
        val emailSelector = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
        }
        val sendWithAttachmentIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, fullBody)
            if (attachmentUri != null) {
                putExtra(Intent.EXTRA_STREAM, attachmentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("Crash Report Log", attachmentUri)
            }
            selector = emailSelector
        }

        val pm = context.packageManager
        if (sendWithAttachmentIntent.resolveActivity(pm) != null) {
            return try {
                val chooser = Intent.createChooser(sendWithAttachmentIntent, "Send Crash Report via Email").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (attachmentUri != null) {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(chooser)
                CrashEmailLaunchResult.Opened
            } catch (_: ActivityNotFoundException) {
                tryMailtoFallback(context, recipient, subject, report)
            } catch (e: Exception) {
                CrashEmailLaunchResult.Failed(e.message ?: "Unable to open email application")
            }
        }

        return tryMailtoFallback(context, recipient, subject, report)
    }

    private fun tryMailtoFallback(
        context: Context,
        recipient: String,
        subject: String,
        report: CrashReport
    ): CrashEmailLaunchResult {
        val compactBody = CrashReportFormatter.buildMailtoCompactBody(report)
        val mailtoUri = Uri.parse(
            "mailto:${Uri.encode(recipient)}?subject=${Uri.encode(subject)}&body=${Uri.encode(compactBody)}"
        )
        val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = mailtoUri
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, compactBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (sendToIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(sendToIntent)
                CrashEmailLaunchResult.Opened
            } else {
                CrashEmailLaunchResult.NoEmailApp
            }
        } catch (_: ActivityNotFoundException) {
            CrashEmailLaunchResult.NoEmailApp
        } catch (e: Exception) {
            CrashEmailLaunchResult.Failed(e.message ?: "Unable to open email application")
        }
    }

    fun launchFeedbackEmail(
        context: Context,
        recipient: String = com.vunnam.config.AppBrandConfig.DEVELOPER_EMAIL,
        subject: String = com.vunnam.config.AppBrandConfig.FEEDBACK_EMAIL_SUBJECT
    ): CrashEmailLaunchResult {
        val mailtoUri = Uri.parse(
            "mailto:${Uri.encode(recipient)}?subject=${Uri.encode(subject)}"
        )
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = mailtoUri
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                CrashEmailLaunchResult.Opened
            } else {
                CrashEmailLaunchResult.NoEmailApp
            }
        } catch (_: ActivityNotFoundException) {
            CrashEmailLaunchResult.NoEmailApp
        } catch (e: Exception) {
            CrashEmailLaunchResult.Failed(e.message ?: "Unable to open email application")
        }
    }
}
