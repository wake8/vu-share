package com.vunnam.crash

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.vunnam.BuildConfig
import com.vunnam.R
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captures uncaught exceptions across all threads, builds sanitized diagnostic logs,
 * writes them synchronously to internal app storage, and delegates to the system handler.
 */
class CrashHandler private constructor(
    private val appContext: Context,
    private val store: CrashReportStore,
    private val appStartTimeMs: Long,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private val handlingCrash = AtomicBoolean(false)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (handlingCrash.compareAndSet(false, true)) {
            try {
                val now = System.currentTimeMillis()
                val isLoop = store.recordAndCheckCrashLoop(now)
                if (!isLoop) {
                    val report = buildReport(appContext, thread, throwable, now, appStartTimeMs)
                    store.saveReportSync(report)
                }
            } catch (_: Throwable) {
                // Never throw from inside an uncaught exception handler
            }
        }
        previousHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        @Volatile
        private var installed = false

        fun install(context: Context, store: CrashReportStore, appStartTimeMs: Long = System.currentTimeMillis()) {
            if (installed) return
            val appCtx = context.applicationContext
            val existing = Thread.getDefaultUncaughtExceptionHandler()
            if (existing is CrashHandler) {
                installed = true
                return
            }
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(appCtx, store, appStartTimeMs, existing)
            )
            installed = true
        }

        fun collectEnvironmentInfo(context: Context, appStartTimeMs: Long = System.currentTimeMillis()): String {
            val runtime = Runtime.getRuntime()
            val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
            val maxMemMb = runtime.maxMemory() / (1024L * 1024L)
            val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
            val uptimeSec = ((System.currentTimeMillis() - appStartTimeMs).coerceAtLeast(0L)) / 1000L
            val localeTag = Locale.getDefault().toLanguageTag()
            val dm = context.resources.displayMetrics
            return buildString {
                appendLine("Primary ABI: $primaryAbi")
                appendLine("Locale: $localeTag")
                appendLine("Display: ${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi} dpi)")
                appendLine("Heap Memory: ${usedMemMb}MB used / ${maxMemMb}MB max")
                append("Session Uptime: ${uptimeSec}s")
            }
        }

        fun getAppMetadata(context: Context): Triple<String, String, Long> {
            val appName = runCatching { context.getString(R.string.app_name) }.getOrDefault("VU Share")
            var versionName = BuildConfig.VERSION_NAME
            var versionCode = BuildConfig.VERSION_CODE.toLong()
            runCatching {
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                versionName = pkgInfo.versionName ?: versionName
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode.toLong()
                }
            }
            return Triple(appName, versionName, versionCode)
        }

        fun buildReport(
            context: Context,
            thread: Thread,
            throwable: Throwable,
            timestampMs: Long = System.currentTimeMillis(),
            appStartTimeMs: Long = timestampMs
        ): CrashReport {
            val (appName, versionName, versionCode) = getAppMetadata(context)
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))

            val manufacturer = Build.MANUFACTURER ?: "Unknown"
            val model = Build.MODEL ?: "Unknown"
            val displayName = CrashReportFormatter.formatDeviceNameAndModel(manufacturer, model)

            return CrashReport(
                id = UUID.randomUUID().toString().take(8),
                appName = appName,
                appVersionName = versionName,
                appVersionCode = versionCode,
                buildType = BuildConfig.BUILD_TYPE,
                androidVersion = Build.VERSION.RELEASE ?: "Unknown",
                apiLevel = Build.VERSION.SDK_INT,
                deviceManufacturer = manufacturer,
                deviceModel = model,
                deviceDisplayName = displayName,
                timestampMs = timestampMs,
                formattedTimestamp = CrashReportFormatter.formatTimestamp(timestampMs),
                exceptionClass = throwable::class.java.name,
                exceptionMessage = CrashReportFormatter.sanitizeText(
                    throwable.message ?: throwable.cause?.message ?: "",
                    CrashReportingConfig.MAX_EXCEPTION_MSG_CHARS
                ),
                stackTrace = CrashReportFormatter.sanitizeText(
                    sw.toString(),
                    CrashReportingConfig.MAX_STACK_TRACE_CHARS
                ),
                threadName = thread.name.ifBlank { "unknown" },
                environmentInfo = CrashReportFormatter.sanitizeText(
                    collectEnvironmentInfo(context, appStartTimeMs),
                    1_500
                ),
                isTestReport = false
            )
        }

        fun buildSyntheticTestReport(context: Context, appStartTimeMs: Long = System.currentTimeMillis()): CrashReport {
            val (appName, versionName, versionCode) = getAppMetadata(context)
            return CrashReportFormatter.createSyntheticTestReport(
                appName = appName,
                appVersionName = versionName,
                appVersionCode = versionCode,
                buildType = BuildConfig.BUILD_TYPE,
                androidVersion = Build.VERSION.RELEASE ?: "Unknown",
                apiLevel = Build.VERSION.SDK_INT,
                deviceManufacturer = Build.MANUFACTURER ?: "Unknown",
                deviceModel = Build.MODEL ?: "Unknown",
                environmentInfo = collectEnvironmentInfo(context, appStartTimeMs)
            )
        }
    }
}
