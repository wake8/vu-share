package com.vunnam

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vunnam.crash.CrashEmailLaunchResult
import com.vunnam.crash.CrashEmailLauncher
import com.vunnam.crash.CrashHandler
import com.vunnam.crash.CrashReportFormatter
import com.vunnam.crash.CrashReportStore
import com.vunnam.crash.CrashReportingConfig
import com.vunnam.database.AppDatabase
import com.vunnam.database.TransferRecordEntity
import com.vunnam.model.TransferDirection
import com.vunnam.model.TransferStatus
import com.vunnam.preferences.UserPreferencesRepository
import com.vunnam.ui.screens.CrashConsentScreen
import com.vunnam.ui.theme.VuShareTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class CrashReportingAndPrivacyTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testEmailRecipientSubjectBodyAndSanitization() {
        assertEquals("vunnam888@gmail.com", CrashReportingConfig.DEFAULT_DEVELOPER_EMAIL)

        val deviceDisplay = CrashReportFormatter.formatDeviceNameAndModel("samsung", "Samsung Galaxy S24")
        assertEquals("Samsung Galaxy S24", deviceDisplay)

        val fallbackDisplay = CrashReportFormatter.formatDeviceNameAndModel("Google", "Pixel 9")
        assertEquals("Google Pixel 9", fallbackDisplay)

        val sanitizedTrace = CrashReportFormatter.sanitizeText(
            "Error opening /storage/emulated/0/Download/secret_passport.pdf for user@example.org at 192.168.1.45\n" +
                "\tat com.vunnam.network.Engine.send(Engine.kt:42)",
            CrashReportingConfig.MAX_STACK_TRACE_CHARS
        )
        assertFalse(sanitizedTrace.contains("secret_passport.pdf"))
        assertFalse(sanitizedTrace.contains("user@example.org"))
        assertFalse(sanitizedTrace.contains("192.168.1.45"))
        assertTrue(sanitizedTrace.contains("[REDACTED_STORAGE_PATH]"))
        assertTrue(sanitizedTrace.contains("[REDACTED_EMAIL]"))
        assertTrue(sanitizedTrace.contains("[REDACTED_IP]"))
        assertTrue(sanitizedTrace.contains("com.vunnam.network.Engine.send"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val realCrash = CrashHandler.buildReport(
            context = context,
            thread = Thread.currentThread(),
            throwable = IllegalStateException("Test runtime error")
        ).copy(deviceDisplayName = "Samsung Galaxy S24")

        assertEquals("App Crash report VU Share, Samsung Galaxy S24", CrashReportFormatter.buildEmailSubject(realCrash))
        val body = CrashReportFormatter.buildEmailBody(realCrash)
        assertTrue(body.contains("Application Name: VU Share"))
        assertTrue(body.contains("Android Version:"))
        assertTrue(body.contains("Exception Class: java.lang.IllegalStateException"))
        assertTrue(body.contains("--- Stack Trace ---"))

        val synthetic = CrashHandler.buildSyntheticTestReport(context)
        assertTrue(synthetic.isTestReport)
        assertTrue(CrashReportFormatter.buildEmailSubject(synthetic).contains("[TEST REPORT]"))
        assertTrue(CrashReportFormatter.buildEmailBody(synthetic).contains("TEST REPORT"))
    }

    @Test
    fun testCrashStoreMultipleReportsCrashLoopGuardAndUncaughtRecovery() {
        val tempDir = Files.createTempDirectory("vushare_crash_store").toFile()
        try {
            val store = CrashReportStore(tempDir)
            val context = ApplicationProvider.getApplicationContext<Context>()

            val firstReport = CrashHandler.buildReport(context, Thread.currentThread(), RuntimeException("First crash"), timestampMs = 1_000L)
            val secondReport = CrashHandler.buildReport(context, Thread.currentThread(), IllegalArgumentException("Second crash"), timestampMs = 2_000L)

            assertTrue(store.saveReportSync(firstReport))
            assertTrue(store.saveReportSync(secondReport))

            // Both unsent reports remain persisted (non-overwriting)
            val loaded = store.loadPendingReports()
            assertEquals(2, loaded.size)
            assertEquals(secondReport.id, loaded[0].id)
            assertEquals(firstReport.id, loaded[1].id)

            // Discard second report; first report stays queued
            assertTrue(store.deleteReport(secondReport.id))
            val remaining = store.loadPendingReports()
            assertEquals(1, remaining.size)
            assertEquals(firstReport.id, remaining[0].id)

            // Crash-loop guard activates on >= 3 rapid crashes
            assertFalse(store.recordAndCheckCrashLoop(10_000L))
            assertFalse(store.recordAndCheckCrashLoop(11_000L))
            assertTrue(store.recordAndCheckCrashLoop(12_000L))
            assertTrue(store.isCurrentlyInCrashLoop(12_500L))
            store.clearCrashLoopGuard()
            assertFalse(store.isCurrentlyInCrashLoop(12_500L))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testEmailLauncherMissingClientAndPrepopulatedIntentAttachment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = CrashHandler.buildSyntheticTestReport(context)

        // With no email handler registered, returns NoEmailApp gracefully
        val missingResult = CrashEmailLauncher.launchEmailClient(context, report)
        assertTrue(missingResult is CrashEmailLaunchResult.NoEmailApp)

        // Register a mock email app in Robolectric PackageManager
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
        pm.addIntentFilterForActivity(
            mailComponent,
            IntentFilter(Intent.ACTION_SEND).apply {
                addDataType("text/plain")
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )

        val openedResult = CrashEmailLauncher.launchEmailClient(context, report)
        assertTrue(openedResult is CrashEmailLaunchResult.Opened)

        val launchedIntent = shadowOf(context as Application).nextStartedActivity
        assertNotNull(launchedIntent)

        // Attachment file created and populated
        val attachDir = File(context.cacheDir, "crash_attachments")
        val attachFile = File(attachDir, CrashReportingConfig.TEST_ATTACHMENT_FILE_NAME)
        assertTrue(attachFile.exists())
        val attachText = attachFile.readText()
        assertTrue(attachText.contains("TEST REPORT"))
        assertTrue(attachText.contains("Stack Trace"))
    }

    @Test
    fun testHideRecentTransfersPersistenceAndOverride() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)

        // Default is false
        assertFalse(repo.userSettingsFlow.first().hideRecentTransfersOnHome)

        // Enable "Completely Hide Recent Transfers on Homepage"
        repo.updateHideRecentTransfersOnHome(true)
        assertTrue(repo.userSettingsFlow.first().hideRecentTransfersOnHome)

        // Verify persistence with a fresh repository instance (simulating app restart)
        val restartedRepo = UserPreferencesRepository(context)
        val settingsAfterRestart = restartedRepo.userSettingsFlow.first()
        assertTrue(settingsAfterRestart.hideRecentTransfersOnHome)

        // Resetting dashboard layout keeps showRecentSharesOnHome=true but MUST preserve hideRecentTransfersOnHome=true
        restartedRepo.resetDashboardLayout()
        val afterReset = restartedRepo.userSettingsFlow.first()
        assertTrue(afterReset.showRecentSharesOnHome)
        assertTrue(afterReset.hideRecentTransfersOnHome)
    }

    @Test
    fun testClearAllTransferHistoryClearsAllStatusesWithoutErrors() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.transferDao()
            dao.insertTransfers(
                listOf(
                    TransferRecordEntity(id = "1", sessionId = "s1", fileName = "photo.jpg", filePath = null, fileSize = 1024L, mimeType = "image/jpeg", remoteDeviceName = "Peer A", remoteDeviceId = "devA", direction = TransferDirection.SEND, timestamp = 100L, status = TransferStatus.COMPLETED, sha256Checksum = "abc"),
                    TransferRecordEntity(id = "2", sessionId = "s2", fileName = "doc.pdf", filePath = "/tmp/doc.pdf", fileSize = 2048L, mimeType = "application/pdf", remoteDeviceName = "Peer B", remoteDeviceId = "devB", direction = TransferDirection.RECEIVE, timestamp = 200L, status = TransferStatus.COMPLETED, sha256Checksum = "def"),
                    TransferRecordEntity(id = "3", sessionId = "s3", fileName = "vid.mp4", filePath = null, fileSize = 4096L, mimeType = "video/mp4", remoteDeviceName = "Peer C", remoteDeviceId = "devC", direction = TransferDirection.SEND, timestamp = 300L, status = TransferStatus.FAILED, sha256Checksum = null),
                    TransferRecordEntity(id = "4", sessionId = "s4", fileName = "song.mp3", filePath = null, fileSize = 512L, mimeType = "audio/mpeg", remoteDeviceName = "Peer D", remoteDeviceId = "devD", direction = TransferDirection.RECEIVE, timestamp = 400L, status = TransferStatus.CANCELLED, sha256Checksum = null)
                )
            )
            assertEquals(4, dao.getAllTransfersFlow().first().size)
            dao.clearAllHistory()
            assertEquals(0, dao.getAllTransfersFlow().first().size)
            assertEquals(0, dao.getRecentTransfersFlow(5).first().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun testCrashConsentScreenUiAndActions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testReport = CrashHandler.buildSyntheticTestReport(context)
        var sendClicks = 0
        var notNowClicks = 0
        var discardClicks = 0

        composeRule.setContent {
            VuShareTheme(themePreference = "LIGHT", customAccent = "CYAN") {
                CrashConsentScreen(
                    report = testReport,
                    pendingCrashCount = 1,
                    emailAppOpened = false,
                    emailErrorMessage = null,
                    onSendCrashReport = { sendClicks++ },
                    onNotNow = { notNowClicks++ },
                    onDiscardReport = { discardClicks++ },
                    onDismissEmailError = {}
                )
            }
        }

        composeRule.onNodeWithText("Help Us Improve VU Share").assertIsDisplayed()
        composeRule.onNodeWithTag("crash_test_report_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("crash_send_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("crash_not_now_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("crash_discard_button").assertIsDisplayed().performClick()

        assertEquals(1, sendClicks)
        assertEquals(1, notNowClicks)
        assertEquals(1, discardClicks)
    }
}
