package com.example.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.ContentSourceEntity
import com.example.core.database.MediaItemEntity
import com.example.core.model.AppStrings
import com.example.core.ui.CinematicHeroCarousel
import com.example.core.ui.CinematicSectionRow
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasEmerald
import com.example.ui.theme.KrasObsidian
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasSurfaceGlass
import com.example.ui.theme.KrasWhite

@Composable
fun HomeScreen(
    allMedia: List<MediaItemEntity>,
    continueWatching: List<MediaItemEntity>,
    myList: List<MediaItemEntity>,
    sources: List<ContentSourceEntity>,
    strings: AppStrings,
    onPlayMedia: (MediaItemEntity) -> Unit,
    onOpenDetails: (MediaItemEntity) -> Unit,
    onToggleMyList: (MediaItemEntity) -> Unit,
    onNavigateToSources: () -> Unit,
    onEnableLegalDemoCatalog: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val featured = remember(allMedia) {
        allMedia.filter { it.isFeatured }.ifEmpty { allMedia.take(5) }
    }
    val recentMovies = remember(allMedia) {
        allMedia.filter { it.type == "MOVIE" }.sortedByDescending { it.addedTimestamp }.take(15)
    }
    val recentSeries = remember(allMedia) {
        allMedia.filter { it.type == "SERIES" }.sortedByDescending { it.updatedTimestamp }.take(15)
    }
    val trending = remember(allMedia) {
        allMedia.filter { it.isTrending }.sortedByDescending { it.rating }
    }
    val popularMovies = remember(allMedia) {
        allMedia.filter { it.type == "MOVIE" && it.isPopular }.sortedByDescending { it.rating }
    }
    val popularSeries = remember(allMedia) {
        allMedia.filter { it.type == "SERIES" && it.isPopular }.sortedByDescending { it.rating }
    }
    val actionItems = remember(allMedia) {
        allMedia.filter { it.genres.contains("Action", ignoreCase = true) }
    }
    val sciFiItems = remember(allMedia) {
        allMedia.filter { it.genres.contains("Science Fiction", ignoreCase = true) || it.genres.contains("Sci-Fi", ignoreCase = true) }
    }
    val dramaItems = remember(allMedia) {
        allMedia.filter { it.genres.contains("Drama", ignoreCase = true) }
    }
    val animationItems = remember(allMedia) {
        allMedia.filter { it.genres.contains("Animation", ignoreCase = true) }
    }
    val comedyItems = remember(allMedia) {
        allMedia.filter { it.genres.contains("Comedy", ignoreCase = true) }
    }
    val horrorItems = remember(allMedia) {
        allMedia.filter { it.genres.contains("Horror", ignoreCase = true) }
    }
    val docItems = remember(allMedia) {
        allMedia.filter { it.genres.contains("Documentar", ignoreCase = true) }
    }
    val recommended = remember(allMedia) {
        allMedia.sortedByDescending { it.rating }.take(12)
    }

    Box(modifier = Modifier.fillMaxSize().background(KrasBlack)) {
        if (allMedia.isEmpty()) {
            EmptyCatalogState(
                strings = strings,
                onAddSourceClick = onNavigateToSources,
                onLoadLegalDemo = onEnableLegalDemoCatalog,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("home_lazy_column"),
                contentPadding = PaddingValues(bottom = 104.dp)
            ) {
                // Hero Banner
                item {
                    CinematicHeroCarousel(
                        featuredItems = featured,
                        strings = strings,
                        onPlayClick = onPlayMedia,
                        onDetailsClick = onOpenDetails,
                        onToggleMyList = onToggleMyList
                    )
                }

                // Continue Watching
                if (continueWatching.isNotEmpty()) {
                    item {
                        CinematicSectionRow(
                            title = strings.continueWatching,
                            items = continueWatching,
                            strings = strings,
                            onItemClick = onPlayMedia,
                            showProgress = true
                        )
                    }
                }

                // My List
                if (myList.isNotEmpty()) {
                    item {
                        CinematicSectionRow(
                            title = strings.navLibrary,
                            items = myList,
                            strings = strings,
                            onItemClick = onOpenDetails
                        )
                    }
                }

                // Featured Movies & Series
                item {
                    CinematicSectionRow(
                        title = strings.featuredContent,
                        items = featured,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }

                // Recently Added Movies
                item {
                    CinematicSectionRow(
                        title = strings.recentlyAddedMovies,
                        items = recentMovies,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }

                // Recently Added Series
                item {
                    CinematicSectionRow(
                        title = strings.recentlyAddedSeries,
                        items = recentSeries,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }

                // Trending Content
                item {
                    CinematicSectionRow(
                        title = strings.trendingNow,
                        items = trending,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }

                // Popular Movies
                item {
                    CinematicSectionRow(
                        title = strings.popularMovies,
                        items = popularMovies,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }

                // Popular Series
                item {
                    CinematicSectionRow(
                        title = strings.popularSeries,
                        items = popularSeries,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }

                // Genre Carousels
                item {
                    CinematicSectionRow(
                        title = strings.genreSciFi,
                        items = sciFiItems,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }
                item {
                    CinematicSectionRow(
                        title = strings.genreAction,
                        items = actionItems,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }
                item {
                    CinematicSectionRow(
                        title = strings.genreDrama,
                        items = dramaItems,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }
                item {
                    CinematicSectionRow(
                        title = strings.genreAnimation,
                        items = animationItems,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }
                item {
                    CinematicSectionRow(
                        title = strings.genreComedy,
                        items = comedyItems,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }
                item {
                    CinematicSectionRow(
                        title = strings.genreHorror,
                        items = horrorItems,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }
                item {
                    CinematicSectionRow(
                        title = strings.genreDocumentary,
                        items = docItems,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }

                // Recommended For You
                item {
                    CinematicSectionRow(
                        title = strings.recommendedForYou,
                        items = recommended,
                        strings = strings,
                        onItemClick = onOpenDetails
                    )
                }
            }
        }

        // Translucent Top App Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(KrasBlack.copy(alpha = 0.88f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = KrasRed,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = KrasWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "KRAS MOVIES",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp),
                    color = KrasWhite
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Sources indicator chip
                val activeCount = sources.count { it.isEnabled }
                Surface(
                    color = KrasSurfaceGlass,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, KrasCardBorder),
                    modifier = Modifier
                        .clickable { onNavigateToSources() }
                        .testTag("top_sources_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = KrasEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (strings.isArabic) "$activeCount مصادر" else "$activeCount Sources",
                            style = MaterialTheme.typography.labelSmall,
                            color = KrasWhite
                        )
                    }
                }

                // Arabic RTL / English LTR quick switch
                Surface(
                    color = KrasSurfaceGlass,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, KrasCardBorder),
                    modifier = Modifier
                        .clickable { onToggleLanguage() }
                        .testTag("language_toggle_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Language",
                            tint = KrasSilver,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (strings.isArabic) "EN" else "عربي",
                            style = MaterialTheme.typography.labelSmall,
                            color = KrasWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogState(
    strings: AppStrings,
    onAddSourceClick: () -> Unit,
    onLoadLegalDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Surface(
            color = KrasCharcoal,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, KrasCardBorder),
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MovieFilter,
                    contentDescription = null,
                    tint = KrasRed,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (strings.isArabic) "لم يتم تكوين أي مصدر محتوى بعد" else "No Content Sources Configured",
            style = MaterialTheme.typography.headlineSmall,
            color = KrasWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (strings.isArabic) {
                "أضف واجهة برمجة تطبيقات (REST / GraphQL) أو خلاصة JSON / M3U مصرحة أو قم بتفعيل كتالوج السينما المفتوحة القانوني للبدء."
            } else {
                "Add an authorized REST API, JSON feed, M3U playlist, or enable the built-in Open Cinema Legal Catalog to transform your media into a unified streaming theater."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = KrasSilver,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddSourceClick,
            colors = ButtonDefaults.buttonColors(containerColor = KrasRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.addSource, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLoadLegalDemo,
            border = BorderStroke(1.dp, KrasCardBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = if (strings.isArabic) "تفعيل كتالوج السينما المفتوحة القانوني" else "Import Legal Open Cinema Catalog",
                color = KrasWhite
            )
        }
    }
}
