package com.vunnam

import android.app.Application
import com.vunnam.crash.CrashHandler
import com.vunnam.crash.CrashReportStore
import com.vunnam.database.AppDatabase
import com.vunnam.network.NearbyConnectionsManager
import com.vunnam.network.P2pSocketTransferEngine
import com.vunnam.preferences.UserPreferencesRepository
import com.vunnam.storage.StorageManager

class VuShareApp : Application() {

    val appStartTimeMs: Long = System.currentTimeMillis()

    lateinit var crashReportStore: CrashReportStore
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepo: UserPreferencesRepository
        private set

    lateinit var storageManager: StorageManager
        private set

    lateinit var socketEngine: P2pSocketTransferEngine
        private set

    lateinit var nearbyManager: NearbyConnectionsManager
        private set

    override fun onCreate() {
        super.onCreate()
        crashReportStore = CrashReportStore.fromContext(this)
        CrashHandler.install(this, crashReportStore, appStartTimeMs)

        database = AppDatabase.getInstance(this)
        preferencesRepo = UserPreferencesRepository(this)
        storageManager = StorageManager(this)
        socketEngine = P2pSocketTransferEngine(this, storageManager, database)
        nearbyManager = NearbyConnectionsManager(this)
    }
}
