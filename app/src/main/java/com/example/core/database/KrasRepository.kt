package com.example.core.database

import android.content.Context
import androidx.datastore.preferences.core. booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.ConnectionValidationResult
import com.example.core.model.SourceType
import com.example.datasource.connectors.SourceConnectorRegistry
import com.example.sync.engine.CatalogSyncEngine
import com.example.sync.engine.SyncSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kras_movies_prefs")

class KrasRepository(
    private val context: Context,
    private val dao: KrasDao,
    private val syncEngine: CatalogSyncEngine
) {
    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next_episode")
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "en"
    }

    val autoPlayNextFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_PLAY_NEXT] ?: true
    }

    val allSourcesFlow: Flow<List<ContentSourceEntity>> = dao.observeAllSources()
    val allMediaFlow: Flow<List<MediaItemEntity>> = dao.observeAllMediaItems()
    val allMoviesFlow: Flow<List<MediaItemEntity>> = dao.observeAllMovies()
    val allSeriesFlow: Flow<List<MediaItemEntity>> = dao.observeAllSeries()
    val myListFlow: Flow<List<MediaItemEntity>> = dao.observeMyList()
    val favoritesFlow: Flow<List<MediaItemEntity>> = dao.observeFavorites()
    val continueWatchingFlow: Flow<List<MediaItemEntity>> = dao.observeContinueWatching()
    val watchHistoryFlow: Flow<List<MediaItemEntity>> = dao.observeWatchHistory()
    val completedEpisodesFlow: Flow<List<EpisodeEntity>> = dao.observeCompletedEpisodes()
    val unwatchedEpisodesFlow: Flow<List<EpisodeEntity>> = dao.observeUnwatchedEpisodes()
    val syncLogsFlow: Flow<List<SyncLogEntity>> = dao.observeSyncLogs()
    val recentSearchesFlow: Flow<List<RecentSearchEntity>> = dao.observeRecentSearches()

    fun observeMediaById(mediaId: Long): Flow<MediaItemEntity?> = dao.observeMediaById(mediaId)

    fun observeEpisodesForSeries(seriesId: Long): Flow<List<EpisodeEntity>> =
        dao.observeEpisodesForSeries(seriesId)

    suspend fun getEpisodesForSeriesOnce(seriesId: Long): List<EpisodeEntity> =
        dao.getEpisodesForSeriesOnce(seriesId)

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { it[KEY_APP_LANGUAGE] = languageCode }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun ensureLegalSampleSourceSeededIfRequested(): Long {
        val existing = dao.getAllSourcesOnce()
        val sample = existing.firstOrNull { it.isSampleSource }
        if (sample != null) {
            return sample.id
        }
        val newId = dao.insertSource(
            ContentSourceEntity(
                name = "Open Cinema Studio (CC-BY Legal Catalog)",
                url = "kras://demo-open-cinema",
                sourceType = SourceType.LEGAL_DEMO_CATALOG.name,
                syncIntervalHours = 12,
                preferredLanguage = "en",
                isEnabled = true,
                isSampleSource = true
            )
        )
        syncEngine.synchronizeSource(newId)
        return newId
    }

    suspend fun testSourceConnection(source: ContentSourceEntity): ConnectionValidationResult {
        val connector = SourceConnectorRegistry.getConnector(source.sourceType)
        return connector.validateConnection(source)
    }

    suspend fun addAndSyncSource(source: ContentSourceEntity): Pair<Long, SyncSummary> {
        val id = dao.insertSource(source)
        val summary = syncEngine.synchronizeSource(id)
        return id to summary
    }

    suspend fun updateSource(source: ContentSourceEntity) {
        dao.updateSource(source)
    }

    suspend fun toggleSourceEnabled(source: ContentSourceEntity, enabled: Boolean) {
        val status = if (enabled) "CONNECTED" else "DISABLED"
        dao.updateSource(source.copy(isEnabled = enabled, connectionStatus = status))
    }

    suspend fun removeSourceAndCatalog(sourceId: Long) {
        dao.deleteEpisodesBySource(sourceId)
        dao.deleteMediaBySource(sourceId)
        dao.deleteSourceById(sourceId)
    }

    suspend fun syncSource(sourceId: Long): SyncSummary {
        return syncEngine.synchronizeSource(sourceId)
    }

    suspend fun syncAllSources(): List<SyncSummary> {
        return syncEngine.synchronizeAllEnabledSources()
    }

    suspend fun toggleMyList(mediaId: Long, currentInList: Boolean) {
        dao.updateMyListStatus(mediaId, !currentInList)
    }

    suspend fun toggleFavorite(mediaId: Long, currentFav: Boolean) {
        dao.updateFavoriteStatus(mediaId, !currentFav)
    }

    suspend fun savePlaybackProgress(
        mediaId: Long,
        episodeId: Long?,
        positionMs: Long,
        durationMs: Long
    ) {
        val safeDuration = durationMs.coerceAtLeast(1L)
        val isCompleted = positionMs.toFloat() / safeDuration.toFloat() >= 0.90f
        dao.updateMediaPlaybackProgress(
            mediaId = mediaId,
            positionMs = if (isCompleted) safeDuration else positionMs,
            durationMs = safeDuration,
            isCompleted = isCompleted
        )
        if (episodeId != null && episodeId > 0L) {
            dao.updateEpisodePlaybackProgress(
                episodeId = episodeId,
                positionMs = if (isCompleted) safeDuration else positionMs,
                durationMs = safeDuration,
                isCompleted = isCompleted
            )
        }
    }

    suspend fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            dao.insertRecentSearch(RecentSearchEntity(query = trimmed))
        }
    }

    suspend fun removeRecentSearch(query: String) {
        dao.deleteRecentSearch(query)
    }

    suspend fun clearRecentSearches() {
        dao.clearRecentSearches()
    }

    suspend fun isFirstLaunch(): Boolean {
        return !onboardingCompletedFlow.first()
    }
}
