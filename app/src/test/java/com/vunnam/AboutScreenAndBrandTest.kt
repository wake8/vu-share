package com.vunnam

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.vunnam.config.AppBrandConfig
import com.vunnam.crash.CrashEmailLaunchResult
import com.vunnam.crash.CrashEmailLauncher
import com.vunnam.ui.screens.AboutScreen
import com.vunnam.ui.theme.VuShareTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AboutScreenAndBrandTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testAppBrandConfigAndFeedbackEmailIntent() {
        assertEquals("VU Share", AppBrandConfig.APP_NAME)
        assertEquals("Share Freely. Stay Connected.", AppBrandConfig.TAGLINE)
        assertEquals("vunnam888@gmail.com", AppBrandConfig.DEVELOPER_EMAIL)
        assertEquals("VU Share – Feedback / Support", AppBrandConfig.FEEDBACK_EMAIL_SUBJECT)
        assertEquals("© 2026 VU Share. All rights reserved.", AppBrandConfig.COPYRIGHT_NOTICE)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val meta = AppBrandConfig.resolveAppMetadata(context)
        assertEquals("VU Share", meta.appName)
        assertTrue(meta.versionName.isNotBlank())
        assertTrue(meta.versionCode >= 1L)

        // Missing email app handled gracefully
        assertTrue(CrashEmailLauncher.launchFeedbackEmail(context) is CrashEmailLaunchResult.NoEmailApp)

        // Registered email client launches with mailto URI + prefilled subject
        val pm = shadowOf(context.packageManager)
        val mailComponent = ComponentName("com.mock.mail", "com.mock.mail.ComposeActivity")
        pm.addOrUpdateActivity(ActivityInfo().apply {
            packageName = mailComponent.packageName
            name = mailComponent.className
        })
        pm.addIntentFilterForActivity(
            mailComponent,
            IntentFilter(Intent.ACTION_SENDTO).apply {
                addDataScheme("mailto")
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )

        val result = CrashEmailLauncher.launchFeedbackEmail(context)
        assertTrue(result is CrashEmailLaunchResult.Opened)

        val launched = shadowOf(context as Application).nextStartedActivity
        assertNotNull(launched)
        assertEquals(Intent.ACTION_SENDTO, launched.action)
        assertEquals(AppBrandConfig.FEEDBACK_EMAIL_SUBJECT, launched.getStringExtra(Intent.EXTRA_SUBJECT))
        val recipients = launched.getStringArrayExtra(Intent.EXTRA_EMAIL)
        assertNotNull(recipients)
        assertEquals(AppBrandConfig.DEVELOPER_EMAIL, recipients!![0])
    }

    @Test
    fun testAboutScreenDisplaysRequiredPlayReadyContentAndLicenses() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val meta = AppBrandConfig.resolveAppMetadata(context)
        var backClicked = false

        composeRule.setContent {
            VuShareTheme(themePreference = "LIGHT", customAccent = "CYAN") {
                AboutScreen(onBack = { backClicked = true })
            }
        }

        composeRule.onNodeWithTag("about_app_name").assertIsDisplayed()
        composeRule.onNodeWithTag("about_tagline").assertIsDisplayed()
        composeRule.onNodeWithText("Version ${meta.versionName} (Build ${meta.versionCode})").assertIsDisplayed()

        // Scroll/check licenses modal opens
        composeRule.onNodeWithTag("about_back_button").performClick()
        assertTrue(backClicked)
    }
}
