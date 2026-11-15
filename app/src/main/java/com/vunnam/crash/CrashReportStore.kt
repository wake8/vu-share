package com.vunnam.crash

import android.content.Context
import com.squareup.moshi.Moshi
import java.io.File

/**
 * Local file-backed repository for pending offline crash reports.
 * Stores each crash as its own JSON file so repeated crashes never overwrite earlier unsent reports.
 */
class CrashReportStore(private val reportsDir: File) {

    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(CrashReport::class.java)
    private val lock = Any()

    init {
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
    }

    /**
     * Checks crash-loop timestamps and returns true if the crash frequency exceeds
     * [CrashReportingConfig.CRASH_LOOP_THRESHOLD] inside [CrashReportingConfig.CRASH_LOOP_WINDOW_MS].
     */
    fun recordAndCheckCrashLoop(nowMs: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        val guardFile = File(reportsDir, CrashReportingConfig.CRASH_GUARD_FILE)
        val recent = if (guardFile.exists()) {
            runCatching {
                guardFile.readText()
                    .lineSequence()
                    .mapNotNull { it.trim().toLongOrNull() }
                    .filter { nowMs - it in 0L..CrashReportingConfig.CRASH_LOOP_WINDOW_MS }
                    .toMutableList()
            }.getOrElse { mutableListOf() }
        } else {
            mutableListOf()
        }
        recent.add(nowMs)
        runCatching {
            guardFile.writeText(recent.joinToString("\n"))
        }
        recent.size >= CrashReportingConfig.CRASH_LOOP_THRESHOLD
    }

    fun isCurrentlyInCrashLoop(nowMs: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        val guardFile = File(reportsDir, CrashReportingConfig.CRASH_GUARD_FILE)
        if (!guardFile.exists()) return@synchronized false
        val count = runCatching {
            guardFile.readText()
                .lineSequence()
                .mapNotNull { it.trim().toLongOrNull() }
                .count { nowMs - it in 0L..CrashReportingConfig.CRASH_LOOP_WINDOW_MS }
        }.getOrDefault(0)
        count >= CrashReportingConfig.CRASH_LOOP_THRESHOLD
    }

    fun clearCrashLoopGuard() = synchronized(lock) {
        val guardFile = File(reportsDir, CrashReportingConfig.CRASH_GUARD_FILE)
        if (guardFile.exists()) {
            guardFile.delete()
        }
    }

    fun saveReportSync(report: CrashReport): Boolean = synchronized(lock) {
        return try {
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val file = File(reportsDir, "crash_${report.timestampMs}_${report.id}.json")
            val json = adapter.toJson(report)
            file.writeText(json, Charsets.UTF_8)
            pruneOverflowReportsLocked()
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun loadPendingReports(): List<CrashReport> = synchronized(lock) {
        if (!reportsDir.exists()) return@synchronized emptyList()
        val files = reportsDir.listFiles { file ->
            file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".json")
        } ?: return@synchronized emptyList()

        files.mapNotNull { file ->
            try {
                adapter.fromJson(file.readText(Charsets.UTF_8))
            } catch (_: Throwable) {
                // Remove corrupted partial crash file so it never triggers repeated parse crashes
                runCatching { file.delete() }
                null
            }
        }.sortedByDescending { it.timestampMs }
    }

    fun deleteReport(reportId: String): Boolean = synchronized(lock) {
        if (!reportsDir.exists()) return@synchronized false
        var deleted = false
        reportsDir.listFiles { file ->
            file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".json")
        }?.forEach { file ->
            val matches = try {
                file.name.contains("_$reportId.json") ||
                    adapter.fromJson(file.readText(Charsets.UTF_8))?.id == reportId
            } catch (_: Throwable) {
                false
            }
            if (matches && file.delete()) {
                deleted = true
            }
        }
        deleted
    }

    fun deleteAllReports() = synchronized(lock) {
        reportsDir.listFiles { file ->
            file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".json")
        }?.forEach { it.delete() }
    }

    private fun pruneOverflowReportsLocked() {
        val files = reportsDir.listFiles { file ->
            file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".json")
        }?.sortedBy { it.lastModified() } ?: return
        val excess = files.size - CrashReportingConfig.MAX_PENDING_REPORTS
        if (excess > 0) {
            files.take(excess).forEach { it.delete() }
        }
    }

    companion object {
        fun fromContext(context: Context): CrashReportStore {
            val dir = File(context.applicationContext.filesDir, CrashReportingConfig.CRASH_REPORTS_DIR)
            return CrashReportStore(dir)
        }
    }
}
