package com.vunnam.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.R
import com.vunnam.config.AppBrandConfig
import com.vunnam.crash.CrashEmailLaunchResult
import com.vunnam.crash.CrashEmailLauncher
import com.vunnam.ui.theme.VuShareTheme

private data class OpenSourceLibraryNotice(
    val name: String,
    val author: String,
    val license: String
)

private val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibraryNotice("Kotlin & Kotlinx Coroutines", "JetBrains s.r.o.", "Apache License 2.0"),
    OpenSourceLibraryNotice("AndroidX Jetpack Compose & Material 3", "The Android Open Source Project", "Apache License 2.0"),
    OpenSourceLibraryNotice("AndroidX Navigation, Room & DataStore", "The Android Open Source Project", "Apache License 2.0"),
    OpenSourceLibraryNotice("AndroidX CameraX & Core KTX", "The Android Open Source Project", "Apache License 2.0"),
    OpenSourceLibraryNotice("Moshi & OkHttp", "Square, Inc.", "Apache License 2.0"),
    OpenSourceLibraryNotice("Coil Compose", "Coil Contributors", "Apache License 2.0"),
    OpenSourceLibraryNotice("ZXing Core (QR Code Encoding)", "ZXing Authors", "Apache License 2.0")
)

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val context = LocalContext.current
    val appMeta = remember(context) { AppBrandConfig.resolveAppMetadata(context) }

    var showLicensesDialog by remember { mutableStateOf(false) }
    var emailStatusMessage by remember { mutableStateOf<String?>(null) }
    var emailStatusIsError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("about_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("about_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primaryText
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About ${appMeta.appName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            }
        }

        // Hero Brand Identity Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // VU Share App Icon
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vu_share_logo),
                            contentDescription = "${appMeta.appName} App Icon",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = appMeta.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.primaryText,
                        modifier = Modifier.testTag("about_app_name")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = AppBrandConfig.TAGLINE,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.primaryAccent,
                        modifier = Modifier.testTag("about_tagline")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.secondaryCard,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "Version ${appMeta.versionName} (Build ${appMeta.versionCode})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.secondaryText,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("about_version_badge")
                        )
                    }
                }
            }
        }

        // About Description Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About VU Share",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Text(
                        text = "VU Share is a modern, offline-first file-sharing application designed to make sharing files between nearby devices fast, simple and convenient.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.primaryText,
                        lineHeight = 21.sp
                    )

                    Text(
                        text = "Inspired by the simplicity and nostalgia of classic SHAREit, VU Share brings back the joy of effortless device-to-device sharing with a modern experience, privacy-focused design and powerful group-sharing capabilities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 20.sp
                    )

                    Text(
                        text = "Whether you're sharing photos, videos, music, documents or other files, VU Share makes local file sharing straightforward.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 20.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.secondaryAccent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Built for simplicity. Designed for speed. Made for sharing.",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSecondaryAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Our Inspiration & Trademark Notice (Separate subtle card)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.secondaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_inspiration_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Our Inspiration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Text(
                        text = "VU Share is an independent project inspired by the simplicity and convenience of classic file-sharing applications such as SHAREit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 19.sp
                    )

                    Text(
                        text = "SHAREit is a trademark of its respective owner. VU Share is independently developed and is not affiliated with, endorsed by or sponsored by SHAREit or its owners.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Privacy Information Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.success.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_privacy_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Text(
                        text = "Your files stay between your devices. VU Share is designed for local peer-to-peer file sharing without cloud-based file uploads or advertising trackers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 19.sp
                    )

                    Text(
                        text = "Transfers are initiated by users and require appropriate connection and receiving-device approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 19.sp
                    )

                    Text(
                        text = "Crash reports, when available, are stored locally and can only be shared with developers through your chosen email application after your explicit consent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Developer Contact & Support Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_contact_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Developer & Support",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.secondaryCard)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Contact Email",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText
                            )
                            Text(
                                text = AppBrandConfig.DEVELOPER_EMAIL,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("VU Share Developer Email", AppBrandConfig.DEVELOPER_EMAIL)
                                )
                                emailStatusIsError = false
                                emailStatusMessage = "Copied ${AppBrandConfig.DEVELOPER_EMAIL} to clipboard."
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Email",
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            when (val result = CrashEmailLauncher.launchFeedbackEmail(context)) {
                                is CrashEmailLaunchResult.Opened -> {
                                    emailStatusIsError = false
                                    emailStatusMessage = null
                                }
                                is CrashEmailLaunchResult.NoEmailApp -> {
                                    emailStatusIsError = true
                                    emailStatusMessage =
                                        "No compatible email application was found. You can copy ${AppBrandConfig.DEVELOPER_EMAIL} and send feedback from any email client."
                                }
                                is CrashEmailLaunchResult.Failed -> {
                                    emailStatusIsError = true
                                    emailStatusMessage = result.reason
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("about_contact_email_button"),
                        shape = RoundedCornerShape(12.dp),
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
                            text = "Contact Developer via Email",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!emailStatusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (emailStatusIsError) colors.errorContainer else colors.successContainer,
                            border = BorderStroke(1.dp, if (emailStatusIsError) colors.error else colors.success),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (emailStatusIsError) colors.error else colors.success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = emailStatusMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.primaryText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Open-Source Licenses Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLicensesDialog = true }
                    .testTag("about_licenses_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Open-Source Licenses",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            Text(
                                text = "View third-party open-source software notices",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryText
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Licenses",
                        tint = colors.secondaryText
                    )
                }
            }
        }

        // Copyright & Legal Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = AppBrandConfig.COPYRIGHT_NOTICE,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                    modifier = Modifier.testTag("about_copyright_text")
                )
                Text(
                    text = "All third-party trademarks and brand names belong to their respective owners.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            containerColor = colors.elevatedSurface,
            title = {
                Text(
                    text = "Open-Source Licenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "VU Share uses the following open-source libraries:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText
                    )
                    OPEN_SOURCE_LIBRARIES.forEachIndexed { index, lib ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = lib.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryText
                            )
                            Text(
                                text = "${lib.author} • ${lib.license}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText
                            )
                        }
                        if (index < OPEN_SOURCE_LIBRARIES.lastIndex) {
                            HorizontalDivider(color = colors.divider)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
