package com.vunnam.crash

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Pure-Kotlin formatting, sanitization, size-limiting, and synthetic test report utilities.
 */
object CrashReportFormatter {

    private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val IPV4_REGEX = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")
    private val EXTERNAL_PATH_REGEX = Regex("/storage/emulated/\\d+/[^\\s:,)\\]]+")
    private val CONTENT_URI_REGEX = Regex("content://[^\\s,)\"]+")
    private val FILE_URI_REGEX = Regex("file:///[^\\s,)\"]+")

    fun formatDeviceNameAndModel(manufacturer: String, model: String): String {
        val cleanMfg = manufacturer.trim()
        val cleanModel = model.trim()
        if (cleanMfg.isBlank()) return cleanModel.ifBlank { "Unknown Device" }
        if (cleanModel.isBlank()) return cleanMfg

        val normalizedMfg = cleanMfg.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
        return if (cleanModel.startsWith(cleanMfg, ignoreCase = true)) {
            if (cleanModel.firstOrNull()?.isLowerCase() == true) {
                normalizedMfg + cleanModel.substring(cleanMfg.length)
            } else {
                cleanModel
            }
        } else {
            "$normalizedMfg $cleanModel"
        }
    }

    fun formatTimestamp(timestampMs: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(timestampMs))
    }

    fun sanitizeText(raw: String?, maxChars: Int): String {
        if (raw.isNullOrBlank()) return ""
        var sanitized = raw
            .replace(EXTERNAL_PATH_REGEX, "[REDACTED_STORAGE_PATH]")
            .replace(CONTENT_URI_REGEX, "[REDACTED_URI]")
            .replace(FILE_URI_REGEX, "[REDACTED_FILE_URI]")
            .replace(IPV4_REGEX, "[REDACTED_IP]")
            .replace(EMAIL_REGEX) { match ->
                if (match.value.equals(CrashReportingConfig.DEFAULT_DEVELOPER_EMAIL, ignoreCase = true)) {
                    match.value
                } else {
                    "[REDACTED_EMAIL]"
                }
            }
        if (sanitized.length > maxChars) {
            sanitized = sanitized.take(maxChars) + "\n... [truncated at $maxChars chars]"
        }
        return sanitized
    }

    fun buildEmailSubject(report: CrashReport): String {
        val base = "${CrashReportingConfig.SUBJECT_PREFIX} ${report.appName}, ${report.deviceDisplayName}"
        return if (report.isTestReport) "$base [TEST REPORT]" else base
    }

    fun buildEmailBody(report: CrashReport, includeFullStackTrace: Boolean = true): String {
        val sb = StringBuilder()
        if (report.isTestReport) {
            sb.appendLine("====================================================")
            sb.appendLine("TEST REPORT - SYNTHETIC DIAGNOSTIC DATA (NO REAL CRASH)")
            sb.appendLine("====================================================")
            sb.appendLine()
        }
        sb.appendLine("=== VU Share Crash Report ===")
        sb.appendLine("Report Type: ${if (report.isTestReport) "TEST REPORT (Synthetic)" else "Uncaught Application Crash"}")
        sb.appendLine("Report ID: ${report.id}")
        sb.appendLine()
        sb.appendLine("--- Application Information ---")
        sb.appendLine("Application Name: ${report.appName}")
        sb.appendLine("Application Version: ${report.appVersionName}")
        sb.appendLine("Version Code: ${report.appVersionCode}")
        sb.appendLine("Build Type: ${report.buildType}")
        sb.appendLine()
        sb.appendLine("--- Device & OS Information ---")
        sb.appendLine("Device Manufacturer: ${report.deviceManufacturer}")
        sb.appendLine("Device Model: ${report.deviceModel}")
        sb.appendLine("Device Display Name: ${report.deviceDisplayName}")
        sb.appendLine("Android Version: Android ${report.androidVersion} (API ${report.apiLevel})")
        sb.appendLine()
        sb.appendLine("--- Crash Details ---")
        sb.appendLine("Crash Date & Time: ${report.formattedTimestamp}")
        sb.appendLine("Thread: ${report.threadName}")
        sb.appendLine("Exception Class: ${report.exceptionClass}")
        sb.appendLine("Exception Message: ${report.exceptionMessage.ifBlank { "(none)" }}")
        sb.appendLine()
        sb.appendLine("--- Environment Diagnostics ---")
        sb.appendLine(report.environmentInfo)
        sb.appendLine()
        if (includeFullStackTrace) {
            sb.appendLine("--- Stack Trace ---")
            sb.appendLine(report.stackTrace)
        }
        return sb.toString().trimEnd()
    }

    fun buildAttachmentContent(report: CrashReport): String {
        return buildEmailBody(report, includeFullStackTrace = true)
    }

    fun buildMailtoCompactBody(report: CrashReport): String {
        val full = buildEmailBody(report, includeFullStackTrace = true)
        return if (full.length <= CrashReportingConfig.MAX_MAILTO_BODY_CHARS) {
            full
        } else {
            full.take(CrashReportingConfig.MAX_MAILTO_BODY_CHARS) +
                "\n... [Body shortened for mailto limit; full stack trace is in attached ${CrashReportingConfig.ATTACHMENT_FILE_NAME}]"
        }
    }

    fun createSyntheticTestReport(
        appName: String,
        appVersionName: String,
        appVersionCode: Long,
        buildType: String,
        androidVersion: String,
        apiLevel: Int,
        deviceManufacturer: String,
        deviceModel: String,
        environmentInfo: String,
        timestampMs: Long = System.currentTimeMillis()
    ): CrashReport {
        val displayName = formatDeviceNameAndModel(deviceManufacturer, deviceModel)
        val syntheticTrace = buildString {
            appendLine("java.lang.IllegalStateException: [TEST REPORT] Synthetic transfer socket handshake timeout simulation")
            appendLine("\tat com.vunnam.network.P2pSocketTransferEngine.handleTestHandshake(P2pSocketTransferEngine.kt:214)")
            appendLine("\tat com.vunnam.ui.MainViewModel.simulateDiagnosticFlow(MainViewModel.kt:412)")
            appendLine("\tat com.vunnam.ui.screens.SettingsScreenKt.DeveloperOptionsSection(SettingsScreen.kt:640)")
            appendLine("\tat androidx.compose.runtime.Recomposer.runRecomposeAndApplyChanges(Recomposer.kt:580)")
            appendLine("Caused by: java.io.IOException: [TEST REPORT] Simulated local Wi-Fi peer socket reset")
            appendLine("\tat com.vunnam.network.TransferStreamTuning.configureSocket(TransferStreamTuning.kt:61)")
            appendLine("\t... 12 more")
        }
        return CrashReport(
            id = "test-${UUID.randomUUID().toString().take(8)}",
            appName = appName,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            buildType = buildType,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            deviceManufacturer = deviceManufacturer,
            deviceModel = deviceModel,
            deviceDisplayName = displayName,
            timestampMs = timestampMs,
            formattedTimestamp = formatTimestamp(timestampMs),
            exceptionClass = "java.lang.IllegalStateException",
            exceptionMessage = "[TEST REPORT] Synthetic diagnostic crash report generated from Developer Options (no real crash occurred).",
            stackTrace = syntheticTrace.trimEnd(),
            threadName = "main (synthetic)",
            environmentInfo = sanitizeText(environmentInfo, 1_500),
            isTestReport = true
        )
    }
}
