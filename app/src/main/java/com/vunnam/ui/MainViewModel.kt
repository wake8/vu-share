package com.vunnam.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vunnam.VuShareApp
import com.vunnam.crash.CrashEmailLaunchResult
import com.vunnam.crash.CrashEmailLauncher
import com.vunnam.crash.CrashHandler
import com.vunnam.crash.CrashReport
import com.vunnam.database.TransferRecordEntity
import com.vunnam.model.FileCategory
import com.vunnam.model.NearbyDevice
import com.vunnam.model.QrPairingData
import com.vunnam.model.SelectableFile
import com.vunnam.model.TransferDirection
import com.vunnam.model.TransferItem
import com.vunnam.model.TransferSession
import com.vunnam.model.TransferStatus
import com.vunnam.network.NetworkUtils
import com.vunnam.network.P2pSocketTransferEngine
import com.vunnam.network.QrPairingHelper
import com.vunnam.preferences.UserSettings
import com.vunnam.transfer.TransferForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VuShareApp
    private val socketEngine = app.socketEngine
    private val storageManager = app.storageManager
    private val preferencesRepo = app.preferencesRepo
    private val database = app.database

    private val cancelReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.vunnam.ACTION_CANCEL_TRANSFER") {
                cancelActiveTransfer()
            }
        }
    }

    init {
        val filter = android.content.IntentFilter("com.vunnam.ACTION_CANCEL_TRANSFER")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(cancelReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(cancelReceiver, filter)
        }
    }

    // User settings & identity
    val userSettings: StateFlow<UserSettings> = preferencesRepo.userSettingsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserSettings(
                deviceId = "local_dev",
                nickname = "Dancing Potato 🥔",
                avatarId = 0,
                avatarEmoji = "🚀",
                themeMode = "MIDNIGHT_BLUE",
                customAccent = "CYAN",
                autoAcceptTrusted = false,
                discoveryVisible = true,
                favoriteDeviceIds = emptySet(),
                blockedDeviceIds = emptySet(),
                customPort = 8988
            )
        )

    // File picking state
    private val _currentCategory = MutableStateFlow(FileCategory.PHOTOS)
    val currentCategory: StateFlow<FileCategory> = _currentCategory.asStateFlow()

    private val _categoryFiles = MutableStateFlow<List<SelectableFile>>(emptyList())
    val categoryFiles: StateFlow<List<SelectableFile>> = _categoryFiles.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<SelectableFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectableFile>> = _selectedFiles.asStateFlow()

    private val _isLoadingFiles = MutableStateFlow(false)
    val isLoadingFiles: StateFlow<Boolean> = _isLoadingFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Discovery & Devices
    val discoveredDevices: StateFlow<List<NearbyDevice>> = socketEngine.discoveredDevices
    val isDiscovering: StateFlow<Boolean> = socketEngine.isDiscovering
    val isServerRunning: StateFlow<Boolean> = socketEngine.isServerRunning

    private val _selectedTargetDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val selectedTargetDevices: StateFlow<List<NearbyDevice>> = _selectedTargetDevices.asStateFlow()

    // Active Transfer Session
    val activeSession: StateFlow<TransferSession?> = socketEngine.activeSession

    // Incoming Transfer Proposal Dialog
    val incomingProposal: StateFlow<P2pSocketTransferEngine.SocketTransferProposal?> = socketEngine.incomingProposal

    // Transfer History from Room
    val transferHistory: StateFlow<List<TransferRecordEntity>> = database.transferDao()
        .getAllTransfersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransfers: StateFlow<List<TransferRecordEntity>> = database.transferDao()
        .getRecentTransfersFlow(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // QR Pairing state
    private val _generatedQrData = MutableStateFlow<QrPairingData?>(null)
    val generatedQrData: StateFlow<QrPairingData?> = _generatedQrData.asStateFlow()

    // Clipboard text sharing
    private val _clipboardShareText = MutableStateFlow("")
    val clipboardShareText: StateFlow<String> = _clipboardShareText.asStateFlow()

    // Offline Crash Reporting state
    private var pendingCrashQueue: List<CrashReport> = emptyList()
    private val _activeCrashReport = MutableStateFlow<CrashReport?>(null)
    val activeCrashReport: StateFlow<CrashReport?> = _activeCrashReport.asStateFlow()

    private val _pendingCrashCount = MutableStateFlow(0)
    val pendingCrashCount: StateFlow<Int> = _pendingCrashCount.asStateFlow()

    private val _emailAppOpenedForReportId = MutableStateFlow<String?>(null)
    val emailAppOpenedForReportId: StateFlow<String?> = _emailAppOpenedForReportId.asStateFlow()

    private val _crashEmailError = MutableStateFlow<String?>(null)
    val crashEmailError: StateFlow<String?> = _crashEmailError.asStateFlow()

    init {
        // Check for locally saved crash reports from a prior uncaught exception
        loadPendingCrashReports()
        // Automatically start local TCP server for listening to incoming transfers
        startReceiverServer()
        // Load initial category
        loadCategoryFiles(FileCategory.PHOTOS)

        // Sync user settings (privacy & concurrency) into socket engine
        viewModelScope.launch {
            userSettings.collect { settings ->
                socketEngine.historyRecordingEnabled = settings.historyRecordingEnabled
                socketEngine.maxConcurrentGroupTransfers = settings.groupTransferConcurrency
            }
        }

        var isForegroundServiceActive = false
        // Observe active session to manage foreground service notifications
        viewModelScope.launch {
            activeSession.collect { session ->
                val hideNames = userSettings.value.hideFilenamesInNotifications
                if (session != null && (session.status == TransferStatus.TRANSFERRING || session.status == TransferStatus.CONNECTING || session.status == TransferStatus.PAUSED)) {
                    val title = when {
                        hideNames -> if (session.direction == TransferDirection.SEND) "Sending encrypted transfer..." else "Receiving encrypted transfer..."
                        session.status == TransferStatus.PAUSED -> "Transfer paused"
                        session.direction == TransferDirection.SEND -> "Sending ${session.items.size} file(s)..."
                        else -> "Receiving ${session.items.size} file(s)..."
                    }
                    val progressInt = (session.overallProgress * 100).toInt()
                    
                    if (!isForegroundServiceActive) {
                        TransferForegroundService.startService(app, title)
                        isForegroundServiceActive = true
                    }
                    
                    TransferForegroundService.updateProgress(
                        app,
                        title,
                        progressInt,
                        100,
                        if (session.status == TransferStatus.PAUSED) "Paused" else NetworkUtils.formatSpeed(session.speedBps)
                    )
                } else if (session == null || session.status == TransferStatus.COMPLETED || session.status == TransferStatus.CANCELLED || session.status == TransferStatus.FAILED || session.status == TransferStatus.REJECTED) {
                    if (isForegroundServiceActive) {
                        TransferForegroundService.stopService(app)
                        isForegroundServiceActive = false
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Category & File Selection
    // -------------------------------------------------------------

    fun selectCategory(category: FileCategory) {
        _currentCategory.value = category
        loadCategoryFiles(category)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadCategoryFiles(category: FileCategory) {
        viewModelScope.launch {
            _isLoadingFiles.value = true
            val files = when (category) {
                FileCategory.PHOTOS -> storageManager.queryPhotos()
                FileCategory.VIDEOS -> storageManager.queryVideos()
                FileCategory.MUSIC -> storageManager.queryMusic()
                FileCategory.DOCUMENTS -> storageManager.queryDocuments()
                FileCategory.APPS -> storageManager.queryInstalledApps()
                FileCategory.FILES -> storageManager.queryDocuments()
            }
            _categoryFiles.value = files
            _isLoadingFiles.value = false
        }
    }

    fun toggleFileSelection(file: SelectableFile) {
        val current = _selectedFiles.value.toMutableList()
        val existing = current.find { it.uri == file.uri }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(file.copy(isSelected = true))
        }
        _selectedFiles.value = current
    }

    fun selectAllCurrentCategory() {
        val currentSelected = _selectedFiles.value.toMutableList()
        val currentCategoryList = _categoryFiles.value
        for (f in currentCategoryList) {
            if (currentSelected.none { it.uri == f.uri }) {
                currentSelected.add(f.copy(isSelected = true))
            }
        }
        _selectedFiles.value = currentSelected
    }

    fun clearFileSelection() {
        _selectedFiles.value = emptyList()
    }

    fun addCustomPickedFiles(uris: List<Uri>) {
        val currentSelected = _selectedFiles.value.toMutableList()
        for (uri in uris) {
            val meta = storageManager.resolveUriMetadata(uri)
            if (meta != null && currentSelected.none { it.uri == meta.uri }) {
                currentSelected.add(meta.copy(isSelected = true))
            }
        }
        _selectedFiles.value = currentSelected
    }

    // -------------------------------------------------------------
    // Device Discovery & Pairing
    // -------------------------------------------------------------

    fun startDiscovery() {
        val settings = userSettings.value
        socketEngine.startDiscovery(
            localNickname = settings.nickname,
            avatarId = settings.avatarId,
            avatarEmoji = settings.avatarEmoji,
            localPort = settings.customPort
        )
    }

    fun stopDiscovery() {
        socketEngine.stopDiscovery()
    }

    fun startReceiverServer() {
        val settings = userSettings.value
        socketEngine.startServer(settings.customPort)
    }

    fun toggleDeviceSelection(device: NearbyDevice) {
        val current = _selectedTargetDevices.value.toMutableList()
        if (current.any { it.id == device.id }) {
            current.removeAll { it.id == device.id }
        } else {
            current.add(device)
        }
        _selectedTargetDevices.value = current
    }

    fun clearDeviceSelection() {
        _selectedTargetDevices.value = emptyList()
    }

    fun generateReceiverQrCode() {
        val settings = userSettings.value
        val localIp = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
        _generatedQrData.value = QrPairingHelper.createPairingToken(
            deviceId = settings.deviceId,
            nickname = settings.nickname,
            avatarEmoji = settings.avatarEmoji,
            hostIp = localIp,
            port = settings.customPort
        )
    }

    fun connectFromScannedQr(qrContent: String, onConnectSuccess: () -> Unit, onError: (String) -> Unit) {
        val parsed = QrPairingHelper.parseFromJson(qrContent)
        if (parsed == null) {
            onError("Invalid or expired QR code pairing token")
            return
        }

        val device = NearbyDevice(
            id = "${parsed.hostIp}:${parsed.port}",
            nickname = parsed.nickname,
            avatarEmoji = parsed.avatarEmoji,
            ipAddress = parsed.hostIp,
            port = parsed.port,
            statusText = "Paired via QR"
        )
        _selectedTargetDevices.value = listOf(device)
        onConnectSuccess()
    }

    // -------------------------------------------------------------
    // Transfer Execution & Control
    // -------------------------------------------------------------

    fun sendSelectedFilesToDevices(devices: List<NearbyDevice>) {
        if (devices.isEmpty() || _selectedFiles.value.isEmpty()) return

        val settings = userSettings.value
        val items = _selectedFiles.value.map { file ->
            TransferItem(
                id = UUID.randomUUID().toString(),
                uri = file.uri,
                fileName = file.name,
                fileSize = file.size,
                mimeType = file.mimeType,
                direction = TransferDirection.SEND
            )
        }

        socketEngine.sendFiles(
            targetDevices = devices,
            items = items,
            localNickname = settings.nickname,
            avatarId = settings.avatarId,
            avatarEmoji = settings.avatarEmoji
        ) {
            // Callback when session is created
        }
    }

    fun acceptIncomingTransfer(proposal: P2pSocketTransferEngine.SocketTransferProposal) {
        socketEngine.acceptIncomingTransfer(proposal)
    }

    fun rejectIncomingTransfer(proposal: P2pSocketTransferEngine.SocketTransferProposal) {
        socketEngine.rejectIncomingTransfer(proposal)
    }

    fun pauseActiveTransfer() {
        socketEngine.pauseActiveTransfer()
    }

    fun resumeActiveTransfer() {
        socketEngine.resumeActiveTransfer()
    }

    fun retryLastTransfer() {
        socketEngine.retryLastTransfer()
    }

    fun cancelActiveTransfer() {
        socketEngine.cancelActiveTransfer()
    }

    fun resetTransferSession() {
        socketEngine.resetSession()
    }

    fun getAvailableStorageBytes(): Long {
        return storageManager.getAvailableBytes()
    }

    // -------------------------------------------------------------
    // History Actions
    // -------------------------------------------------------------

    fun openReceivedFile(filePath: String): Boolean {
        return storageManager.openFile(filePath)
    }

    fun shareReceivedFile(filePath: String) {
        storageManager.shareFile(filePath)
    }

    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch {
            database.transferDao().deleteTransferById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            database.transferDao().clearAllHistory()
        }
    }

    // -------------------------------------------------------------
    // Settings & Identity
    // -------------------------------------------------------------

    fun randomizeNicknameAndAvatar() {
        viewModelScope.launch {
            preferencesRepo.randomizeIdentity()
        }
    }

    fun updateNickname(newName: String) {
        viewModelScope.launch {
            preferencesRepo.updateNickname(newName)
        }
    }

    fun updateAvatar(avatarId: Int, emoji: String) {
        viewModelScope.launch {
            preferencesRepo.updateAvatar(avatarId, emoji)
        }
    }

    fun updateThemeMode(theme: String) {
        viewModelScope.launch {
            preferencesRepo.updateThemeMode(theme)
        }
    }

    fun updateCustomAccent(accent: String) {
        viewModelScope.launch {
            preferencesRepo.updateCustomAccent(accent)
        }
    }

    fun updateGroupConcurrency(concurrency: Int) {
        viewModelScope.launch {
            preferencesRepo.updateGroupConcurrency(concurrency)
        }
    }

    fun updateDashboardToggles(
        showRecent: Boolean,
        showRadar: Boolean,
        showFavs: Boolean,
        showActive: Boolean,
        compact: Boolean
    ) {
        viewModelScope.launch {
            preferencesRepo.updateDashboardToggles(showRecent, showRadar, showFavs, showActive, compact)
        }
    }

    fun resetDashboardLayout() {
        viewModelScope.launch {
            preferencesRepo.resetDashboardLayout()
        }
    }

    fun updateHistoryPrivacy(
        recordingEnabled: Boolean,
        retentionDays: Int,
        hideFilenamesNotif: Boolean
    ) {
        viewModelScope.launch {
            preferencesRepo.updateHistoryPrivacy(recordingEnabled, retentionDays, hideFilenamesNotif)
        }
    }

    fun updateHideRecentTransfersOnHome(hide: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateHideRecentTransfersOnHome(hide)
        }
    }

    fun updateDeveloperOptionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateDeveloperOptionsEnabled(enabled)
        }
    }

    // -------------------------------------------------------------
    // Offline Crash Reporting & Consent Lifecycle
    // -------------------------------------------------------------

    private fun loadPendingCrashReports() {
        val reports = app.crashReportStore.loadPendingReports()
        // App survived initialization cleanly; reset tight crash-loop guard for future sessions
        app.crashReportStore.clearCrashLoopGuard()
        pendingCrashQueue = reports
        _pendingCrashCount.value = reports.size
        _activeCrashReport.value = reports.firstOrNull()
    }

    fun simulateSyntheticCrashReport() {
        val synthetic = CrashHandler.buildSyntheticTestReport(app, app.appStartTimeMs)
        _emailAppOpenedForReportId.value = null
        _crashEmailError.value = null
        _activeCrashReport.value = synthetic
    }

    fun sendActiveCrashReport() {
        val report = _activeCrashReport.value ?: return
        _crashEmailError.value = null
        when (val result = CrashEmailLauncher.launchEmailClient(app, report)) {
            is CrashEmailLaunchResult.Opened -> {
                // Retain the local report until user confirms email was sent or explicitly discards it
                _emailAppOpenedForReportId.value = report.id
            }
            is CrashEmailLaunchResult.NoEmailApp -> {
                _crashEmailError.value =
                    "No compatible email application is installed on this device. An email app is required to review and send the crash report."
            }
            is CrashEmailLaunchResult.Failed -> {
                _crashEmailError.value = result.reason
            }
        }
    }

    fun deferActiveCrashReport() {
        // "Not Now": keep real pending reports saved on disk for next app launch, just dismiss UI now
        _emailAppOpenedForReportId.value = null
        _crashEmailError.value = null
        _activeCrashReport.value = null
    }

    fun discardActiveCrashReport() {
        val current = _activeCrashReport.value ?: return
        _emailAppOpenedForReportId.value = null
        _crashEmailError.value = null
        if (current.isTestReport) {
            // Do not delete any real pending reports when discarding a synthetic test report
            _activeCrashReport.value = null
        } else {
            app.crashReportStore.deleteReport(current.id)
            pendingCrashQueue = pendingCrashQueue.filterNot { it.id == current.id }
            _pendingCrashCount.value = pendingCrashQueue.size
            _activeCrashReport.value = pendingCrashQueue.firstOrNull()
        }
    }

    fun dismissCrashEmailError() {
        _crashEmailError.value = null
    }

    fun toggleFavoriteDevice(deviceId: String) {
        viewModelScope.launch {
            preferencesRepo.toggleFavorite(deviceId)
        }
    }

    fun toggleBlockDevice(deviceId: String) {
        viewModelScope.launch {
            preferencesRepo.toggleBlockDevice(deviceId)
        }
    }

    fun setClipboardShareText(text: String) {
        _clipboardShareText.value = text
    }

    override fun onCleared() {
        super.onCleared()
        socketEngine.stopDiscovery()
        socketEngine.stopServer()
        try {
            app.unregisterReceiver(cancelReceiver)
        } catch (e: Exception) {
            // Ignored if already unregistered
        }
    }
}
