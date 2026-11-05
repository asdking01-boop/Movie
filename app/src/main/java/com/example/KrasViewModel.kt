package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.ContentSourceEntity
import com.example.core.database.EpisodeEntity
import com.example.core.database.KrasRepository
import com.example.core.database.MediaItemEntity
import com.example.core.database.RecentSearchEntity
import com.example.core.database.SyncLogEntity
import com.example.core.model.ConnectionValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainNavTab {
    HOME,
    MOVIES,
    SERIES,
    SEARCH,
    LIBRARY,
    SOURCES
}

data class ActivePlayerSession(
    val mediaItem: MediaItemEntity,
    val initialEpisodeId: Long? = null
)

class KrasViewModel(private val repository: KrasRepository) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean> = repository.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appLanguage: StateFlow<String> = repository.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val autoPlayNext: StateFlow<Boolean> = repository.autoPlayNextFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allSources: StateFlow<List<ContentSourceEntity>> = repository.allSourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedia: StateFlow<List<MediaItemEntity>> = repository.allMediaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovies: StateFlow<List<MediaItemEntity>> = repository.allMoviesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSeries: StateFlow<List<MediaItemEntity>> = repository.allSeriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myList: StateFlow<List<MediaItemEntity>> = repository.myListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<MediaItemEntity>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<MediaItemEntity>> = repository.continueWatchingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<MediaItemEntity>> = repository.watchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedEpisodes: StateFlow<List<EpisodeEntity>> = repository.completedEpisodesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unwatchedEpisodes: StateFlow<List<EpisodeEntity>> = repository.unwatchedEpisodesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.syncLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<RecentSearchEntity>> = repository.recentSearchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(MainNavTab.HOME)
    val currentTab: StateFlow<MainNavTab> = _currentTab.asStateFlow()

    private val _selectedDetailMediaId = MutableStateFlow<Long?>(null)
    val selectedDetailMediaId: StateFlow<Long?> = _selectedDetailMediaId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedDetailMedia: StateFlow<MediaItemEntity?> = _selectedDetailMediaId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeMediaById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedSeriesEpisodes: StateFlow<List<EpisodeEntity>> = _selectedDetailMediaId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeEpisodesForSeries(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePlayerSession = MutableStateFlow<ActivePlayerSession?>(null)
    val activePlayerSession: StateFlow<ActivePlayerSession?> = _activePlayerSession.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activePlayerEpisodes: StateFlow<List<EpisodeEntity>> = _activePlayerSession
        .flatMapLatest { session ->
            if (session == null || session.mediaItem.type != "SERIES") {
                flowOf(emptyList())
            } else {
                repository.observeEpisodesForSeries(session.mediaItem.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncingAll = MutableStateFlow(false)
    val isSyncingAll: StateFlow<Boolean> = _isSyncingAll.asStateFlow()

    private val _lastSyncBanner = MutableStateFlow<String?>(null)
    val lastSyncBanner: StateFlow<String?> = _lastSyncBanner.asStateFlow()

    private val _validationFeedback = MutableStateFlow<Map<Long, ConnectionValidationResult>>(emptyMap())
    val validationFeedback: StateFlow<Map<Long, ConnectionValidationResult>> = _validationFeedback.asStateFlow()

    private val _showOnboardingOverlay = MutableStateFlow(false)
    val showOnboardingOverlay: StateFlow<Boolean> = _showOnboardingOverlay.asStateFlow()

    init {
        // Ensure initial legal catalog is seeded so the app is immediately functional while also offering first-launch setup
        viewModelScope.launch {
            val isFirst = repository.isFirstLaunch()
            if (isFirst) {
                _showOnboardingOverlay.value = true
                repository.ensureLegalSampleSourceSeededIfRequested()
            }
        }
    }

    fun selectTab(tab: MainNavTab) {
        _selectedDetailMediaId.value = null
        _currentTab.value = tab
    }

    fun openMediaDetails(item: MediaItemEntity) {
        _selectedDetailMediaId.value = item.id
    }

    fun closeMediaDetails() {
        _selectedDetailMediaId.value = null
    }

    fun startPlayback(item: MediaItemEntity, episodeId: Long? = null) {
        viewModelScope.launch {
            if (item.type == "SERIES" && episodeId == null) {
                val eps = repository.getEpisodesForSeriesOnce(item.id)
                val targetEp = eps.firstOrNull { it.watchedPositionMs > 0 && !it.isCompleted }
                    ?: eps.firstOrNull { !it.isCompleted }
                    ?: eps.firstOrNull()
                _activePlayerSession.value = ActivePlayerSession(item, targetEp?.id)
            } else {
                _activePlayerSession.value = ActivePlayerSession(item, episodeId)
            }
        }
    }

    fun startEpisodePlaybackDirect(episode: EpisodeEntity) {
        val parentSeries = allMedia.value.firstOrNull { it.id == episode.seriesId } ?: return
        _activePlayerSession.value = ActivePlayerSession(parentSeries, episode.id)
    }

    fun closePlayer() {
        _activePlayerSession.value = null
    }

    fun savePlaybackProgress(mediaId: Long, episodeId: Long?, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.savePlaybackProgress(mediaId, episodeId, positionMs, durationMs)
        }
    }

    fun toggleMyList(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.toggleMyList(item.id, item.inMyList)
        }
    }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, item.isFavorite)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (appLanguage.value == "ar") "en" else "ar"
            repository.setAppLanguage(next)
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoPlayNext(enabled)
        }
    }

    fun completeOnboarding(includeLegalDemo: Boolean, customSource: ContentSourceEntity?) {
        viewModelScope.launch {
            if (includeLegalDemo) {
                repository.ensureLegalSampleSourceSeededIfRequested()
            }
            if (customSource != null) {
                repository.addAndSyncSource(customSource)
            }
            repository.setOnboardingCompleted(true)
            _showOnboardingOverlay.value = false
        }
    }

    fun enableLegalDemoCatalog() {
        viewModelScope.launch {
            _isSyncingAll.value = true
            repository.ensureLegalSampleSourceSeededIfRequested()
            _isSyncingAll.value = false
            _lastSyncBanner.value = "Legal Open Cinema Catalog imported and synchronized."
        }
    }

    fun addSource(source: ContentSourceEntity) {
        viewModelScope.launch {
            _isSyncingAll.value = true
            val (_, summary) = repository.addAndSyncSource(source)
            _lastSyncBanner.value = "${summary.sourceName}: ${summary.message}"
            _isSyncingAll.value = false
        }
    }

    fun updateSource(source: ContentSourceEntity) {
        viewModelScope.launch {
            repository.updateSource(source)
            val summary = repository.syncSource(source.id)
            _lastSyncBanner.value = "${summary.sourceName}: ${summary.message}"
        }
    }

    fun toggleSourceEnabled(source: ContentSourceEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSourceEnabled(source, enabled)
            if (enabled) {
                repository.syncSource(source.id)
            }
        }
    }

    fun testSourceConnection(source: ContentSourceEntity) {
        viewModelScope.launch {
            val res = repository.testSourceConnection(source)
            val updated = _validationFeedback.value.toMutableMap()
            updated[source.id] = res
            _validationFeedback.value = updated
        }
    }

    fun syncSource(sourceId: Long) {
        viewModelScope.launch {
            val summary = repository.syncSource(sourceId)
            _lastSyncBanner.value = "${summary.sourceName}: ${summary.message}"
        }
    }

    fun syncAllSources() {
        viewModelScope.launch {
            _isSyncingAll.value = true
            val summaries = repository.syncAllSources()
            val okCount = summaries.count { it.isSuccess }
            _lastSyncBanner.value = "Synchronized $okCount/${summaries.size} enabled content sources."
            _isSyncingAll.value = false
        }
    }

    fun removeSource(source: ContentSourceEntity) {
        viewModelScope.launch {
            repository.removeSourceAndCatalog(source.id)
            _lastSyncBanner.value = "Removed '${source.name}' and its imported catalog items."
        }
    }

    fun saveRecentSearch(query: String) {
        viewModelScope.launch {
            repository.saveRecentSearch(query)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            repository.removeRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    class Factory(private val repository: KrasRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KrasViewModel(repository) as T
        }
    }
}
