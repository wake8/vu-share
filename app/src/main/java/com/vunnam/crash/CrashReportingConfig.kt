package com.vunnam.crash

import com.vunnam.config.AppBrandConfig

/**
 * Centralized configuration for VU Share's offline, consent-based crash reporting.
 * Update [AppBrandConfig.DEVELOPER_EMAIL] to change the support recipient across the app.
 */
object CrashReportingConfig {
    const val DEFAULT_DEVELOPER_EMAIL = AppBrandConfig.DEVELOPER_EMAIL
    const val SUBJECT_PREFIX = "App Crash report"
    const val CRASH_REPORTS_DIR = "crash_reports"
    const val CRASH_GUARD_FILE = "crash_loop_guard.txt"
    const val ATTACHMENT_FILE_NAME = "vushare_crash_report.txt"
    const val TEST_ATTACHMENT_FILE_NAME = "vushare_test_crash_report.txt"

    const val MAX_STACK_TRACE_CHARS = 12_000
    const val MAX_EXCEPTION_MSG_CHARS = 800
    const val MAX_MAILTO_BODY_CHARS = 6_000
    const val MAX_PENDING_REPORTS = 5

    // Crash-loop protection: if 3 crashes happen within 12 seconds, suppress repeated writes
    const val CRASH_LOOP_WINDOW_MS = 12_000L
    const val CRASH_LOOP_THRESHOLD = 3
}
