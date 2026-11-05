package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContentSourceEntity::class,
        MediaItemEntity::class,
        EpisodeEntity::class,
        SyncLogEntity::class,
        RecentSearchEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KrasDatabase : RoomDatabase() {
    abstract fun krasDao(): KrasDao

    companion object {
        @Volatile
        private var INSTANCE: KrasDatabase? = null

        fun getInstance(context: Context): KrasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KrasDatabase::class.java,
                    "kras_movies_catalog.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
