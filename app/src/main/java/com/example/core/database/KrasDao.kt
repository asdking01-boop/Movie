package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KrasDao {

    // --- CONTENT SOURCES ---
    @Query("SELECT * FROM content_sources ORDER BY isSampleSource ASC, id DESC")
    fun observeAllSources(): Flow<List<ContentSourceEntity>>

    @Query("SELECT * FROM content_sources")
    suspend fun getAllSourcesOnce(): List<ContentSourceEntity>

    @Query("SELECT * FROM content_sources WHERE isEnabled = 1")
    suspend fun getEnabledSourcesOnce(): List<ContentSourceEntity>

    @Query("SELECT * FROM content_sources WHERE id = :id LIMIT 1")
    suspend fun getSourceById(id: Long): ContentSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: ContentSourceEntity): Long

    @Update
    suspend fun updateSource(source: ContentSourceEntity)

    @Query("DELETE FROM content_sources WHERE id = :sourceId")
    suspend fun deleteSourceById(sourceId: Long)

    // --- MEDIA ITEMS ---
    @Query("SELECT * FROM media_items ORDER BY isFeatured DESC, rating DESC, addedTimestamp DESC")
    fun observeAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE type = 'MOVIE' ORDER BY addedTimestamp DESC")
    fun observeAllMovies(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE type = 'SERIES' ORDER BY updatedTimestamp DESC")
    fun observeAllSeries(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE inMyList = 1 ORDER BY addedTimestamp DESC")
    fun observeMyList(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY addedTimestamp DESC")
    fun observeFavorites(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE watchedPositionMs > 0 AND isCompleted = 0 ORDER BY lastWatchedTimestamp DESC")
    fun observeContinueWatching(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC")
    fun observeWatchHistory(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :mediaId LIMIT 1")
    suspend fun getMediaById(mediaId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id = :mediaId LIMIT 1")
    fun observeMediaById(mediaId: Long): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE externalId = :externalId AND sourceId = :sourceId LIMIT 1")
    suspend fun getMediaByExternalId(externalId: String, sourceId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE LOWER(title) = LOWER(:title) AND releaseYear = :year AND type = :type LIMIT 1")
    suspend fun findDuplicateByTitleAndYear(title: String, year: Int, type: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity): Long

    @Update
    suspend fun updateMediaItem(item: MediaItemEntity)

    @Query("UPDATE media_items SET inMyList = :inMyList WHERE id = :mediaId")
    suspend fun updateMyListStatus(mediaId: Long, inMyList: Boolean)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :mediaId")
    suspend fun updateFavoriteStatus(mediaId: Long, isFavorite: Boolean)

    @Query(
        """
        UPDATE media_items 
        SET watchedPositionMs = :positionMs, 
            totalDurationMs = :durationMs, 
            isCompleted = :isCompleted, 
            lastWatchedTimestamp = :timestamp 
        WHERE id = :mediaId
        """
    )
    suspend fun updateMediaPlaybackProgress(
        mediaId: Long,
        positionMs: Long,
        durationMs: Long,
        isCompleted: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM media_items WHERE sourceId = :sourceId")
    suspend fun deleteMediaBySource(sourceId: Long)

    @Query("SELECT COUNT(*) FROM media_items WHERE sourceId = :sourceId AND type = 'MOVIE'")
    suspend fun countMoviesForSource(sourceId: Long): Int

    @Query("SELECT COUNT(*) FROM media_items WHERE sourceId = :sourceId AND type = 'SERIES'")
    suspend fun countSeriesForSource(sourceId: Long): Int

    // --- EPISODES ---
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun observeEpisodesForSeries(seriesId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodesForSeriesOnce(seriesId: Long): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE isCompleted = 1 ORDER BY lastWatchedTimestamp DESC")
    fun observeCompletedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE isCompleted = 0 ORDER BY seriesId ASC, seasonNumber ASC, episodeNumber ASC")
    fun observeUnwatchedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    suspend fun getEpisodeById(episodeId: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId AND seasonNumber = :season AND episodeNumber = :episode LIMIT 1")
    suspend fun getEpisodeByNumber(seriesId: Long, season: Int, episode: Int): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity): Long

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)

    @Query(
        """
        UPDATE episodes 
        SET watchedPositionMs = :positionMs, 
            totalDurationMs = :durationMs, 
            isCompleted = :isCompleted, 
            lastWatchedTimestamp = :timestamp 
        WHERE id = :episodeId
        """
    )
    suspend fun updateEpisodePlaybackProgress(
        episodeId: Long,
        positionMs: Long,
        durationMs: Long,
        isCompleted: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM episodes WHERE sourceId = :sourceId")
    suspend fun deleteEpisodesBySource(sourceId: Long)

    // --- SYNC LOGS ---
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 50")
    fun observeSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    // --- RECENT SEARCHES ---
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 12")
    fun observeRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}
