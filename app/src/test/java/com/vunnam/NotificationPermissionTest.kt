package com.vunnam

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.vunnam.ui.components.NotificationPermissionHelper
import com.vunnam.ui.components.NotificationPermissionRationaleDialog
import com.vunnam.ui.theme.VuShareTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class NotificationPermissionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testNotificationPermissionHelperFlagsAndSettingsIntent() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        // Fresh state: rationale not yet prompted, system prompt not yet requested
        assertFalse(NotificationPermissionHelper.hasPromptedNotificationRationale(context))
        assertFalse(NotificationPermissionHelper.wasSystemPromptRequested(context))
        assertTrue(NotificationPermissionHelper.requiresRuntimePermission())

        // Denied by default on API 34 until granted
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(NotificationPermissionHelper.isNotificationPermissionGranted(context))
        assertFalse(NotificationPermissionHelper.isPermanentlyDenied(context))

        // Once system dialog was requested and rationale isn't required, falls back to Settings
        NotificationPermissionHelper.markNotificationRationalePrompted(context)
        NotificationPermissionHelper.markSystemPromptRequested(context)
        assertTrue(NotificationPermissionHelper.hasPromptedNotificationRationale(context))
        assertTrue(NotificationPermissionHelper.wasSystemPromptRequested(context))
        assertTrue(NotificationPermissionHelper.isPermanentlyDenied(context))

        // Granting permission restores granted=true and permanentlyDenied=false
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .setNotificationsEnabled(true)
        assertTrue(NotificationPermissionHelper.isNotificationPermissionGranted(context))
        assertFalse(NotificationPermissionHelper.isPermanentlyDenied(context))

        // Settings intent targets app notification settings on API 26+
        val settingsIntent = NotificationPermissionHelper.createAppNotificationSettingsIntent(context)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, settingsIntent.action)
        assertEquals(context.packageName, settingsIntent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test
    fun testNotificationRationaleDialogUiAndActions() {
        var allowClicks = 0
        var dismissClicks = 0

        composeRule.setContent {
            VuShareTheme(themePreference = "LIGHT", customAccent = "CYAN") {
                NotificationPermissionRationaleDialog(
                    onAllow = { allowClicks++ },
                    onNotNow = { dismissClicks++ }
                )
            }
        }

        composeRule.onNodeWithTag("notification_permission_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Show Transfer Notifications?").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_permission_allow_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("notification_permission_dismiss_button").assertIsDisplayed().performClick()

        assertEquals(1, allowClicks)
        assertEquals(1, dismissClicks)
    }
}
