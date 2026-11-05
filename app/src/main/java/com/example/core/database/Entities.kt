package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "content_sources")
data class ContentSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val sourceType: String, // SourceType.name
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val syncIntervalHours: Int = 6,
    val preferredLanguage: String = "en",
    val allowedCategories: String = "Movies,Series,Action,Sci-Fi,Drama,Animation,Documentary",
    val isEnabled: Boolean = true,
    val isSampleSource: Boolean = false,
    val connectionStatus: String = "UNTESTED", // SourceConnectionStatus.name
    val detectedCapabilities: String = "", // comma-separated SourceCapability.name
    val importedMovieCount: Int = 0,
    val importedSeriesCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncError: String? = null,
    val syncProgress: Float = 0f
)

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["externalId", "sourceId"], unique = true),
        Index(value = ["type"]),
        Index(value = ["sourceId"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val sourceId: Long,
    val sourceName: String,
    val isSampleContent: Boolean,
    val type: String, // MOVIE or SERIES
    val title: String,
    val titleAr: String = "",
    val originalTitle: String = "",
    val description: String,
    val descriptionAr: String = "",
    val posterUrl: String,
    val backdropUrl: String,
    val localPosterRes: String = "",
    val localBackdropRes: String = "",
    val releaseYear: Int,
    val genres: String, // comma-separated
    val durationMinutes: Int,
    val language: String,
    val rating: Double,
    val director: String = "",
    val castMembers: String = "", // comma-separated
    val playbackUrl: String = "",
    val subtitlesJson: String = "", // serialized subtitle tracks
    val addedTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isPopular: Boolean = false,
    val seasonCount: Int = 0,
    val episodeCount: Int = 0,
    val inMyList: Boolean = false,
    val isFavorite: Boolean = false,
    val watchedPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val lastWatchedTimestamp: Long = 0L
)

@Entity(
    tableName = "episodes",
    indices = [
        Index(value = ["seriesId", "seasonNumber", "episodeNumber"], unique = true),
        Index(value = ["sourceId"])
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val seriesId: Long,
    val sourceId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val durationMinutes: Int,
    val playbackUrl: String,
    val subtitlesJson: String = "",
    val releaseDate: String = "",
    val watchedPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val lastWatchedTimestamp: Long = 0L
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val sourceName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val moviesAdded: Int = 0,
    val seriesAdded: Int = 0,
    val episodesAdded: Int = 0,
    val durationMs: Long = 0L,
    val message: String
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
