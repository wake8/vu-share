package com.vunnam.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "vu_share_preferences")

data class UserSettings(
    val deviceId: String,
    val nickname: String,
    val avatarId: Int,
    val avatarEmoji: String,
    val themeMode: String, // "LIGHT", "MINIMAL_LIGHT", "DARK", "MIDNIGHT_BLUE", "AMOLED_BLACK", "AURORA", "EMERALD", "SYSTEM"
    val customAccent: String, // "CYAN", "VIOLET", "EMERALD", "CORAL", "GOLD", "ROSE"
    val autoAcceptTrusted: Boolean,
    val discoveryVisible: Boolean,
    val favoriteDeviceIds: Set<String>,
    val blockedDeviceIds: Set<String>,
    val customPort: Int,
    val groupTransferConcurrency: Int = 3, // 1..3 concurrent peers for group transfers
    // Home Dashboard Customization
    val showRecentSharesOnHome: Boolean = true,
    val showNearbyRadarOnHome: Boolean = true,
    val showFavoriteDevicesOnHome: Boolean = true,
    val showActiveTransfersOnHome: Boolean = true,
    val compactCardsMode: Boolean = false,
    val hideRecentTransfersOnHome: Boolean = false,
    // History & Privacy Settings
    val historyRecordingEnabled: Boolean = true,
    val historyRetentionDays: Int = 0, // 0 = never, 7, 30, 90
    val hideFilenamesInNotifications: Boolean = false,
    // TEMPORARY DEVELOPER OPTIONS - REMOVE BEFORE PRODUCTION
    val developerOptionsEnabled: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR_ID = intPreferencesKey("avatar_id")
        val AVATAR_EMOJI = stringPreferencesKey("avatar_emoji")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_ACCENT = stringPreferencesKey("custom_accent")
        val AUTO_ACCEPT_TRUSTED = booleanPreferencesKey("auto_accept_trusted")
        val DISCOVERY_VISIBLE = booleanPreferencesKey("discovery_visible")
        val FAVORITE_DEVICES = stringSetPreferencesKey("favorite_devices")
        val BLOCKED_DEVICES = stringSetPreferencesKey("blocked_devices")
        val CUSTOM_PORT = intPreferencesKey("custom_port")
        val GROUP_CONCURRENCY = intPreferencesKey("group_concurrency")
        // Dashboard customization
        val SHOW_RECENT_SHARES = booleanPreferencesKey("show_recent_shares")
        val SHOW_NEARBY_RADAR = booleanPreferencesKey("show_nearby_radar")
        val SHOW_FAVORITES = booleanPreferencesKey("show_favorites")
        val SHOW_ACTIVE_TRANSFERS = booleanPreferencesKey("show_active_transfers")
        val COMPACT_CARDS = booleanPreferencesKey("compact_cards")
        val HIDE_RECENT_TRANSFERS_ON_HOME = booleanPreferencesKey("hide_recent_transfers_on_home")
        // Privacy
        val HISTORY_RECORDING = booleanPreferencesKey("history_recording")
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val HIDE_FILENAMES_NOTIF = booleanPreferencesKey("hide_filenames_notif")
        // Developer options (temporary)
        val DEVELOPER_OPTIONS_ENABLED = booleanPreferencesKey("developer_options_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val deviceId = preferences[PreferencesKeys.DEVICE_ID] ?: run {
            UUID.randomUUID().toString().take(8)
        }
        val nickname = preferences[PreferencesKeys.NICKNAME] ?: DeviceIdentityManager.getRandomNickname()
        val avatarId = preferences[PreferencesKeys.AVATAR_ID] ?: 0
        val avatarEmoji = preferences[PreferencesKeys.AVATAR_EMOJI] ?: "🚀"
        val themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "MIDNIGHT_BLUE"
        val customAccent = preferences[PreferencesKeys.CUSTOM_ACCENT] ?: "CYAN"
        val autoAcceptTrusted = preferences[PreferencesKeys.AUTO_ACCEPT_TRUSTED] ?: false
        val discoveryVisible = preferences[PreferencesKeys.DISCOVERY_VISIBLE] ?: true
        val favorites = preferences[PreferencesKeys.FAVORITE_DEVICES] ?: emptySet()
        val blocked = preferences[PreferencesKeys.BLOCKED_DEVICES] ?: emptySet()
        val port = preferences[PreferencesKeys.CUSTOM_PORT] ?: 8988
        val groupConcurrency = (preferences[PreferencesKeys.GROUP_CONCURRENCY] ?: 3).coerceIn(1, 3)

        val showRecent = preferences[PreferencesKeys.SHOW_RECENT_SHARES] ?: true
        val showRadar = preferences[PreferencesKeys.SHOW_NEARBY_RADAR] ?: true
        val showFavs = preferences[PreferencesKeys.SHOW_FAVORITES] ?: true
        val showActive = preferences[PreferencesKeys.SHOW_ACTIVE_TRANSFERS] ?: true
        val compact = preferences[PreferencesKeys.COMPACT_CARDS] ?: false
        val hideRecentOnHome = preferences[PreferencesKeys.HIDE_RECENT_TRANSFERS_ON_HOME] ?: false

        val historyEnabled = preferences[PreferencesKeys.HISTORY_RECORDING] ?: true
        val retention = preferences[PreferencesKeys.HISTORY_RETENTION_DAYS] ?: 0
        val hideNamesNotif = preferences[PreferencesKeys.HIDE_FILENAMES_NOTIF] ?: false
        val devOptionsEnabled = preferences[PreferencesKeys.DEVELOPER_OPTIONS_ENABLED] ?: false

        UserSettings(
            deviceId = deviceId,
            nickname = nickname,
            avatarId = avatarId,
            avatarEmoji = avatarEmoji,
            themeMode = themeMode,
            customAccent = customAccent,
            autoAcceptTrusted = autoAcceptTrusted,
            discoveryVisible = discoveryVisible,
            favoriteDeviceIds = favorites,
            blockedDeviceIds = blocked,
            customPort = port,
            groupTransferConcurrency = groupConcurrency,
            showRecentSharesOnHome = showRecent,
            showNearbyRadarOnHome = showRadar,
            showFavoriteDevicesOnHome = showFavs,
            showActiveTransfersOnHome = showActive,
            compactCardsMode = compact,
            hideRecentTransfersOnHome = hideRecentOnHome,
            historyRecordingEnabled = historyEnabled,
            historyRetentionDays = retention,
            hideFilenamesInNotifications = hideNamesNotif,
            developerOptionsEnabled = devOptionsEnabled
        )
    }

    suspend fun updateNickname(nickname: String) {
        context.dataStore.edit { it[PreferencesKeys.NICKNAME] = nickname }
    }

    suspend fun updateAvatar(avatarId: Int, emoji: String) {
        context.dataStore.edit {
            it[PreferencesKeys.AVATAR_ID] = avatarId
            it[PreferencesKeys.AVATAR_EMOJI] = emoji
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun updateCustomAccent(accent: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_ACCENT] = accent }
    }

    suspend fun updateGroupConcurrency(concurrency: Int) {
        context.dataStore.edit { it[PreferencesKeys.GROUP_CONCURRENCY] = concurrency.coerceIn(1, 3) }
    }

    suspend fun updateDashboardToggles(
        showRecent: Boolean,
        showRadar: Boolean,
        showFavs: Boolean,
        showActive: Boolean,
        compact: Boolean
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.SHOW_RECENT_SHARES] = showRecent
            it[PreferencesKeys.SHOW_NEARBY_RADAR] = showRadar
            it[PreferencesKeys.SHOW_FAVORITES] = showFavs
            it[PreferencesKeys.SHOW_ACTIVE_TRANSFERS] = showActive
            it[PreferencesKeys.COMPACT_CARDS] = compact
        }
    }

    suspend fun resetDashboardLayout() {
        context.dataStore.edit {
            it[PreferencesKeys.SHOW_RECENT_SHARES] = true
            it[PreferencesKeys.SHOW_NEARBY_RADAR] = true
            it[PreferencesKeys.SHOW_FAVORITES] = true
            it[PreferencesKeys.SHOW_ACTIVE_TRANSFERS] = true
            it[PreferencesKeys.COMPACT_CARDS] = false
        }
    }

    suspend fun updateHistoryPrivacy(
        recordingEnabled: Boolean,
        retentionDays: Int,
        hideFilenamesNotif: Boolean
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.HISTORY_RECORDING] = recordingEnabled
            it[PreferencesKeys.HISTORY_RETENTION_DAYS] = retentionDays
            it[PreferencesKeys.HIDE_FILENAMES_NOTIF] = hideFilenamesNotif
        }
    }

    suspend fun updateHideRecentTransfersOnHome(hide: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.HIDE_RECENT_TRANSFERS_ON_HOME] = hide
        }
    }

    suspend fun updateDeveloperOptionsEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.DEVELOPER_OPTIONS_ENABLED] = enabled
        }
    }

    suspend fun toggleFavorite(deviceId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.FAVORITE_DEVICES] ?: emptySet()
            if (current.contains(deviceId)) {
                prefs[PreferencesKeys.FAVORITE_DEVICES] = current - deviceId
            } else {
                prefs[PreferencesKeys.FAVORITE_DEVICES] = current + deviceId
            }
        }
    }

    suspend fun toggleBlockDevice(deviceId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.BLOCKED_DEVICES] ?: emptySet()
            if (current.contains(deviceId)) {
                prefs[PreferencesKeys.BLOCKED_DEVICES] = current - deviceId
            } else {
                prefs[PreferencesKeys.BLOCKED_DEVICES] = current + deviceId
            }
        }
    }

    suspend fun randomizeIdentity() {
        val randomName = DeviceIdentityManager.getRandomNickname()
        val randomAvatar = DeviceIdentityManager.getRandomAvatar()
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NICKNAME] = randomName
            prefs[PreferencesKeys.AVATAR_ID] = randomAvatar.id
            prefs[PreferencesKeys.AVATAR_EMOJI] = randomAvatar.emoji
        }
    }
}
