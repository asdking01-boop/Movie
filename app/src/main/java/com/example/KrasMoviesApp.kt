package com.example

import android.app.Application
import com.example.core.database.KrasDatabase
import com.example.core.database.KrasRepository
import com.example.sync.engine.CatalogSyncEngine
import com.example.sync.engine.CatalogSyncWorker

class KrasMoviesApp : Application() {

    lateinit var database: KrasDatabase
        private set

    lateinit var syncEngine: CatalogSyncEngine
        private set

    lateinit var repository: KrasRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = KrasDatabase.getInstance(this)
        syncEngine = CatalogSyncEngine(database.krasDao())
        repository = KrasRepository(this, database.krasDao(), syncEngine)
        CatalogSyncWorker.schedulePeriodicSync(this, intervalHours = 6L)
    }
}
