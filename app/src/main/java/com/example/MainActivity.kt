package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.model.AppStrings
import com.example.feature.home.HomeScreen
import com.example.feature.movies.MoviesScreen
import com.example.feature.player.VideoPlayerScreen
import com.example.feature.search.SearchScreen
import com.example.feature.series.MediaDetailScreen
import com.example.feature.series.SeriesCatalogScreen
import com.example.feature.settings.PersonalLibraryScreen
import com.example.feature.sources.ContentSourcesScreen
import com.example.feature.sources.OnboardingFlowScreen
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasMoviesTheme
import com.example.ui.theme.KrasMuted
import com.example.ui.theme.KrasObsidian
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasWhite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KrasMoviesApp

        setContent {
            val viewModel: KrasViewModel = viewModel(
                factory = KrasViewModel.Factory(app.repository)
            )
            val languageCode by viewModel.appLanguage.collectAsStateWithLifecycle()

            KrasMoviesTheme(languageCode = languageCode) {
                KrasStreamingRoot(viewModel = viewModel)
            }
        }
    }
}

private data class NavDestinationItem(
    val tab: MainNavTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun KrasStreamingRoot(viewModel: KrasViewModel) {
    val languageCode by viewModel.appLanguage.collectAsStateWithLifecycle()
    val strings = remember(languageCode) {
        AppStrings(isArabic = languageCode.startsWith("ar", ignoreCase = true))
    }

    val showOnboarding by viewModel.showOnboardingOverlay.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allSources by viewModel.allSources.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val allMovies by viewModel.allMovies.collectAsStateWithLifecycle()
    val allSeries by viewModel.allSeries.collectAsStateWithLifecycle()
    val myList by viewModel.myList.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val completedEpisodes by viewModel.completedEpisodes.collectAsStateWithLifecycle()
    val unwatchedEpisodes by viewModel.unwatchedEpisodes.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val isSyncingAll by viewModel.isSyncingAll.collectAsStateWithLifecycle()
    val lastSyncBanner by viewModel.lastSyncBanner.collectAsStateWithLifecycle()
    val validationFeedback by viewModel.validationFeedback.collectAsStateWithLifecycle()
    val autoPlayNext by viewModel.autoPlayNext.collectAsStateWithLifecycle()

    val selectedDetailMedia by viewModel.selectedDetailMedia.collectAsStateWithLifecycle()
    val selectedSeriesEpisodes by viewModel.selectedSeriesEpisodes.collectAsStateWithLifecycle()
    val activePlayerSession by viewModel.activePlayerSession.collectAsStateWithLifecycle()
    val activePlayerEpisodes by viewModel.activePlayerEpisodes.collectAsStateWithLifecycle()

    // 1. Active Full-Screen Video Player Experience
    if (activePlayerSession != null) {
        val session = activePlayerSession!!
        VideoPlayerScreen(
            mediaItem = session.mediaItem,
            episodes = activePlayerEpisodes,
            initialEpisodeId = session.initialEpisodeId,
            autoPlayNextEnabled = autoPlayNext,
            strings = strings,
            onSaveProgress = { mId, epId, pos, dur ->
                viewModel.savePlaybackProgress(mId, epId, pos, dur)
            },
            onBack = { viewModel.closePlayer() }
        )
        return
    }

    // 2. First-Launch Animated Splash & Onboarding Experience
    if (showOnboarding) {
        OnboardingFlowScreen(
            strings = strings,
            onToggleLanguage = { viewModel.toggleLanguage() },
            onCompleteWithSources = { includeLegal, customSource ->
                viewModel.completeOnboarding(includeLegal, customSource)
            }
        )
        return
    }

    // 3. Movie or TV Series Detail Screen Overlay
    if (selectedDetailMedia != null) {
        val detailItem = selectedDetailMedia!!
        val related = remember(allMedia, detailItem) {
            allMedia.filter { it.id != detailItem.id }.take(10)
        }
        MediaDetailScreen(
            item = detailItem,
            episodes = selectedSeriesEpisodes,
            relatedItems = related,
            strings = strings,
            onPlayMovieOrEpisode = { epId ->
                viewModel.startPlayback(detailItem, epId)
            },
            onToggleMyList = { viewModel.toggleMyList(detailItem) },
            onToggleFavorite = { viewModel.toggleFavorite(detailItem) },
            onSelectRelatedItem = { viewModel.openMediaDetails(it) },
            onBack = { viewModel.closeMediaDetails() }
        )
        return
    }

    val navItems = remember(strings) {
        listOf(
            NavDestinationItem(MainNavTab.HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
            NavDestinationItem(MainNavTab.MOVIES, strings.navMovies, Icons.Filled.Movie, Icons.Outlined.Movie, "nav_tab_movies"),
            NavDestinationItem(MainNavTab.SERIES, strings.navSeries, Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_tab_series"),
            NavDestinationItem(MainNavTab.SEARCH, strings.navSearch, Icons.Filled.Search, Icons.Outlined.Search, "nav_tab_search"),
            NavDestinationItem(MainNavTab.LIBRARY, strings.navLibrary, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_tab_library"),
            NavDestinationItem(MainNavTab.SOURCES, strings.navSources, Icons.Filled.CloudQueue, Icons.Outlined.CloudQueue, "nav_tab_sources")
        )
    }

    // 4. Adaptive Responsive Main Scaffold (NavigationRail on Tablets/TV >= 640.dp, NavigationBar on Smartphones)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack)
    ) {
        val isWideScreen = maxWidth >= 640.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = KrasObsidian,
                    contentColor = KrasWhite,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    navItems.forEach { dest ->
                        val selected = currentTab == dest.tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(dest.tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = KrasWhite,
                                selectedTextColor = KrasRed,
                                indicatorColor = KrasRed.copy(alpha = 0.25f),
                                unselectedIconColor = KrasMuted,
                                unselectedTextColor = KrasMuted
                            ),
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ActiveTabContent(
                        currentTab = currentTab,
                        viewModel = viewModel,
                        strings = strings,
                        allSources = allSources,
                        allMedia = allMedia,
                        allMovies = allMovies,
                        allSeries = allSeries,
                        myList = myList,
                        favorites = favorites,
                        continueWatching = continueWatching,
                        watchHistory = watchHistory,
                        completedEpisodes = completedEpisodes,
                        unwatchedEpisodes = unwatchedEpisodes,
                        syncLogs = syncLogs,
                        recentSearches = recentSearches,
                        isSyncingAll = isSyncingAll,
                        lastSyncBanner = lastSyncBanner,
                        validationFeedback = validationFeedback,
                        autoPlayNext = autoPlayNext
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = KrasBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = KrasObsidian.copy(alpha = 0.96f),
                        contentColor = KrasWhite,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { dest ->
                            val selected = currentTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KrasWhite,
                                    selectedTextColor = KrasRed,
                                    indicatorColor = KrasRed.copy(alpha = 0.25f),
                                    unselectedIconColor = KrasMuted,
                                    unselectedTextColor = KrasMuted
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding() * 0f)
                ) {
                    ActiveTabContent(
                        currentTab = currentTab,
                        viewModel = viewModel,
                        strings = strings,
                        allSources = allSources,
                        allMedia = allMedia,
                        allMovies = allMovies,
                        allSeries = allSeries,
                        myList = myList,
                        favorites = favorites,
                        continueWatching = continueWatching,
                        watchHistory = watchHistory,
                        completedEpisodes = completedEpisodes,
                        unwatchedEpisodes = unwatchedEpisodes,
                        syncLogs = syncLogs,
                        recentSearches = recentSearches,
                        isSyncingAll = isSyncingAll,
                        lastSyncBanner = lastSyncBanner,
                        validationFeedback = validationFeedback,
                        autoPlayNext = autoPlayNext
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    currentTab: MainNavTab,
    viewModel: KrasViewModel,
    strings: AppStrings,
    allSources: List<com.example.core.database.ContentSourceEntity>,
    allMedia: List<com.example.core.database.MediaItemEntity>,
    allMovies: List<com.example.core.database.MediaItemEntity>,
    allSeries: List<com.example.core.database.MediaItemEntity>,
    myList: List<com.example.core.database.MediaItemEntity>,
    favorites: List<com.example.core.database.MediaItemEntity>,
    continueWatching: List<com.example.core.database.MediaItemEntity>,
    watchHistory: List<com.example.core.database.MediaItemEntity>,
    completedEpisodes: List<com.example.core.database.EpisodeEntity>,
    unwatchedEpisodes: List<com.example.core.database.EpisodeEntity>,
    syncLogs: List<com.example.core.database.SyncLogEntity>,
    recentSearches: List<com.example.core.database.RecentSearchEntity>,
    isSyncingAll: Boolean,
    lastSyncBanner: String?,
    validationFeedback: Map<Long, com.example.core.model.ConnectionValidationResult>,
    autoPlayNext: Boolean
) {
    when (currentTab) {
        MainNavTab.HOME -> {
            HomeScreen(
                allMedia = allMedia,
                continueWatching = continueWatching,
                myList = myList,
                sources = allSources,
                strings = strings,
                onPlayMedia = { viewModel.startPlayback(it) },
                onOpenDetails = { viewModel.openMediaDetails(it) },
                onToggleMyList = { viewModel.toggleMyList(it) },
                onNavigateToSources = { viewModel.selectTab(MainNavTab.SOURCES) },
                onEnableLegalDemoCatalog = { viewModel.enableLegalDemoCatalog() },
                onToggleLanguage = { viewModel.toggleLanguage() }
            )
        }
        MainNavTab.MOVIES -> {
            MoviesScreen(
                movies = allMovies,
                sources = allSources,
                strings = strings,
                onMovieClick = { viewModel.openMediaDetails(it) }
            )
        }
        MainNavTab.SERIES -> {
            SeriesCatalogScreen(
                seriesList = allSeries,
                sources = allSources,
                strings = strings,
                onSeriesClick = { viewModel.openMediaDetails(it) }
            )
        }
        MainNavTab.SEARCH -> {
            SearchScreen(
                allMedia = allMedia,
                sources = allSources,
                recentSearches = recentSearches,
                strings = strings,
                onItemClick = { viewModel.openMediaDetails(it) },
                onSaveRecentSearch = { viewModel.saveRecentSearch(it) },
                onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                onClearRecentSearches = { viewModel.clearRecentSearches() }
            )
        }
        MainNavTab.LIBRARY -> {
            PersonalLibraryScreen(
                myList = myList,
                favorites = favorites,
                continueWatching = continueWatching,
                watchHistory = watchHistory,
                completedEpisodes = completedEpisodes,
                unwatchedEpisodes = unwatchedEpisodes,
                strings = strings,
                onMediaClick = { viewModel.openMediaDetails(it) },
                onEpisodeClick = { viewModel.startEpisodePlaybackDirect(it) }
            )
        }
        MainNavTab.SOURCES -> {
            ContentSourcesScreen(
                sources = allSources,
                syncLogs = syncLogs,
                isSyncingAll = isSyncingAll,
                lastStatusBanner = lastSyncBanner,
                validationFeedback = validationFeedback,
                autoPlayNext = autoPlayNext,
                strings = strings,
                onAddSource = { viewModel.addSource(it) },
                onUpdateSource = { viewModel.updateSource(it) },
                onToggleSourceEnabled = { src, enabled -> viewModel.toggleSourceEnabled(src, enabled) },
                onTestConnection = { viewModel.testSourceConnection(it) },
                onSyncSource = { viewModel.syncSource(it) },
                onSyncAll = { viewModel.syncAllSources() },
                onRemoveSource = { viewModel.removeSource(it) },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onToggleAutoPlayNext = { viewModel.setAutoPlayNext(it) }
            )
        }
    }
}
