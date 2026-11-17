package com.vunnam.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vunnam.R
import com.vunnam.ui.theme.VuShareTheme

object NotificationPermissionHelper {

    private const val PREFS_NAME = "vu_notification_permission_prefs"
    private const val KEY_RATIONALE_PROMPTED = "rationale_prompted"
    private const val KEY_SYSTEM_PROMPT_REQUESTED = "system_prompt_requested"

    fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isNotificationPermissionGranted(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun hasPromptedNotificationRationale(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_RATIONALE_PROMPTED, false)

    fun markNotificationRationalePrompted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RATIONALE_PROMPTED, true)
            .apply()
    }

    fun wasSystemPromptRequested(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SYSTEM_PROMPT_REQUESTED, false)

    fun markSystemPromptRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SYSTEM_PROMPT_REQUESTED, true)
            .apply()
    }

    fun shouldShowSystemRationale(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val activity = context.findActivity() ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    fun isPermanentlyDenied(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return !NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val runtimeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (runtimeGranted) return false
        return wasSystemPromptRequested(context) && !shouldShowSystemRationale(context)
    }

    fun createAppNotificationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = createAppNotificationSettingsIntent(context).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
fun NotificationPermissionRationaleDialog(
    onAllow: () -> Unit,
    onNotNow: () -> Unit
) {
    val colors = VuShareTheme.colors

    AlertDialog(
        onDismissRequest = onNotNow,
        modifier = Modifier.testTag("notification_permission_dialog"),
        containerColor = colors.elevatedSurface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.primaryAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.notification_permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText
            )
        },
        text = {
            Text(
                text = stringResource(R.string.notification_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.onPrimaryAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("notification_permission_allow_button")
            ) {
                Text(
                    text = stringResource(R.string.notification_permission_allow),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onNotNow,
                border = BorderStroke(1.dp, colors.border),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("notification_permission_dismiss_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.notification_permission_not_now),
                        color = colors.primaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}
