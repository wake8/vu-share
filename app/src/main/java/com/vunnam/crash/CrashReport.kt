package com.vunnam.crash

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CrashReport(
    val id: String,
    val appName: String,
    val appVersionName: String,
    val appVersionCode: Long,
    val buildType: String,
    val androidVersion: String,
    val apiLevel: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val deviceDisplayName: String,
    val timestampMs: Long,
    val formattedTimestamp: String,
    val exceptionClass: String,
    val exceptionMessage: String,
    val stackTrace: String,
    val threadName: String,
    val environmentInfo: String,
    val isTestReport: Boolean = false
)
