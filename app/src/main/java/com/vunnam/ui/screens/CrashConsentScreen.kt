package com.vunnam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.crash.CrashReport
import com.vunnam.crash.CrashReportFormatter
import com.vunnam.crash.CrashReportingConfig
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun CrashConsentScreen(
    report: CrashReport,
    pendingCrashCount: Int,
    emailAppOpened: Boolean,
    emailErrorMessage: String?,
    onSendCrashReport: () -> Unit,
    onNotNow: () -> Unit,
    onDiscardReport: () -> Unit,
    onDismissEmailError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    var showStackTrace by remember(report.id) { mutableStateOf(false) }

    BackHandler(onBack = onNotNow)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("crash_consent_screen"),
        color = colors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Hero Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                if (report.isTestReport) colors.warning else colors.primaryAccent,
                                                colors.secondaryHue
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (report.isTestReport) Icons.Default.Science else Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = colors.onPrimaryAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                if (report.isTestReport) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.warningContainer,
                                        border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.6f)),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = "TEST REPORT • SYNTHETIC SIMULATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .testTag("crash_test_report_badge")
                                        )
                                    }
                                } else if (pendingCrashCount > 1) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.secondaryAccent,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = "$pendingCrashCount unsent reports saved locally",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSecondaryAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Help Us Improve VU Share",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.primaryText,
                                    modifier = Modifier.testTag("crash_consent_heading")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "VU Share encountered an unexpected error. Would you like to help us improve the application by sharing an anonymous crash report with the developers?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Email launch error feedback (e.g., no compatible email app installed)
                if (!emailErrorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                            border = BorderStroke(1.dp, colors.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crash_email_error_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Email Application Required",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = emailErrorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primaryText
                                    )
                                }
                                TextButton(onClick = onDismissEmailError) {
                                    Text("OK", color = colors.primaryText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Post-email-app confirmation card (keeps report until user confirms sent or discards)
                if (emailAppOpened) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.successContainer),
                            border = BorderStroke(1.dp, colors.success),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crash_email_opened_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Email App Opened",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryText
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Your report remains stored locally until you confirm that you finished sending the email or discard it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onDiscardReport,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.success,
                                            contentColor = colors.onSuccess
                                        ),
                                        modifier = Modifier.testTag("crash_confirm_sent_button")
                                    ) {
                                        Text("I Sent It (Remove)", fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = onNotNow,
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Text("Keep for Later", color = colors.primaryText)
                                    }
                                }
                            }
                        }
                    }
                }

                // Card 1: What Happened + Timestamp + Expandable Stack Trace
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (report.isTestReport) colors.warning else colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "What Happened",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = report.exceptionClass,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            if (report.exceptionMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = report.exceptionMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Crash Time: ${report.formattedTimestamp}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = colors.divider)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showStackTrace = !showStackTrace }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .testTag("crash_toggle_stacktrace"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showStackTrace) "Hide Stack Trace & Details" else "Inspect Full Stack Trace",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primaryAccent
                                )
                                Icon(
                                    imageVector = if (showStackTrace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = colors.primaryAccent
                                )
                            }

                            AnimatedVisibility(visible = showStackTrace) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.secondaryCard)
                                        .padding(10.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = report.stackTrace,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Application & Device Information
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Application & Device Information",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                            }

                            DiagnosticRow(
                                label = "Application",
                                value = "${report.appName} v${report.appVersionName} (Build ${report.appVersionCode} • ${report.buildType})"
                            )
                            DiagnosticRow(
                                label = "Device",
                                value = "${report.deviceDisplayName} (${report.deviceManufacturer} / ${report.deviceModel})"
                            )
                            DiagnosticRow(
                                label = "Android OS",
                                value = "Android ${report.androidVersion} (API ${report.apiLevel})"
                            )
                            DiagnosticRow(
                                label = "Email Subject",
                                value = CrashReportFormatter.buildEmailSubject(report)
                            )
                            DiagnosticRow(
                                label = "Recipient",
                                value = CrashReportingConfig.DEFAULT_DEVELOPER_EMAIL
                            )
                        }
                    }
                }

                // Card 3: What Will Be Included + Privacy Guarantee
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colors.secondaryHue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "What Information Will Be Included",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                            }

                            Text(
                                text = "• Application name, version code, and build type\n" +
                                    "• Device manufacturer, model, Android version, and API level\n" +
                                    "• Crash timestamp, exception type, message, and stack trace\n" +
                                    "• Basic diagnostics (ABI, locale, display metrics, heap memory, uptime)\n" +
                                    "• Attached plain-text log (${if (report.isTestReport) CrashReportingConfig.TEST_ATTACHMENT_FILE_NAME else CrashReportingConfig.ATTACHMENT_FILE_NAME})",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryText,
                                lineHeight = 19.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.secondaryCard,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "VU Share does not automatically upload crash reports or send diagnostic information to external servers. Your crash report remains stored locally until you choose to share it. Sending the report requires an email application and an internet connection.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primaryText,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar
            Surface(
                color = colors.primaryCard,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSendCrashReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("crash_send_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yes, Send Crash Report",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNotNow,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("crash_not_now_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "Not Now",
                                color = colors.primaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onDiscardReport,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("crash_discard_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Discard Report",
                                color = colors.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    val colors = VuShareTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.secondaryText,
            modifier = Modifier.width(108.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            modifier = Modifier.weight(1f)
        )
    }
}
