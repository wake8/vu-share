package com.vunnam.config

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.vunnam.BuildConfig
import com.vunnam.R

data class AppVersionMetadata(
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val buildType: String
)

/**
 * Centralized brand identity, support contact, and legal notice configuration for VU Share.
 * Edit [DEVELOPER_EMAIL] here to update developer contact across About and Crash Reporting.
 */
object AppBrandConfig {
    const val APP_NAME = "VU Share"
    const val TAGLINE = "Share Freely. Stay Connected."
    const val DEVELOPER_EMAIL = "vunnam888@gmail.com"
    const val FEEDBACK_EMAIL_SUBJECT = "VU Share – Feedback / Support"
    const val COPYRIGHT_NOTICE = "© 2026 VU Share. All rights reserved."

    fun resolveAppMetadata(context: Context): AppVersionMetadata {
        val appName = runCatching { context.getString(R.string.app_name) }.getOrDefault(APP_NAME)
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
        return AppVersionMetadata(
            appName = appName,
            versionName = versionName,
            versionCode = versionCode,
            buildType = BuildConfig.BUILD_TYPE
        )
    }
}
