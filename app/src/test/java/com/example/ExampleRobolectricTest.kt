package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.ContentSourceEntity
import com.example.core.database.KrasDatabase
import com.example.core.model.SourceType
import com.example.sync.engine.CatalogSyncEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kras Movies", appName)
    }

    @Test
    fun `sync engine imports movies and series into Room database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KrasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.krasDao()
        val engine = CatalogSyncEngine(dao)

        val sourceId = dao.insertSource(
            ContentSourceEntity(
                name = "Test Open Broadcast JSON",
                url = "kras://open-json-feed",
                sourceType = SourceType.JSON_FEED.name,
                isEnabled = true
            )
        )

        val summary = engine.synchronizeSource(sourceId)
        assertTrue(summary.isSuccess)
        assertTrue(summary.moviesAdded >= 1)
        assertTrue(summary.seriesAdded >= 1)

        val items = dao.observeAllMediaItems().first()
        assertTrue(items.isNotEmpty())
        db.close()
    }
}
