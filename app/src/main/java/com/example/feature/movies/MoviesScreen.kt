package com.example.feature.movies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.database.ContentSourceEntity
import com.example.core.database.MediaItemEntity
import com.example.core.model.AppStrings
import com.example.core.model.SearchNormalizer
import com.example.core.model.SortOption
import com.example.core.ui.CinematicPosterCard
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasMuted
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasWhite

@Composable
fun MoviesScreen(
    movies: List<MediaItemEntity>,
    sources: List<ContentSourceEntity>,
    strings: AppStrings,
    onMovieClick: (MediaItemEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=All, 1=Latest 20, 2=Recently Released
    var selectedGenre by remember { mutableStateOf("ALL") }
    var selectedYear by remember { mutableIntStateOf(0) } // 0 = All years
    var selectedSourceId by remember { mutableStateOf<Long?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_ADDED) }

    val availableGenres = remember(movies) {
        val set = linkedSetOf("ALL")
        movies.forEach { m ->
            m.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { set.add(it) }
        }
        set.toList()
    }

    val availableYears = remember(movies) {
        listOf(0) + movies.map { it.releaseYear }.distinct().sortedDescending()
    }

    val filteredMovies = remember(
        movies,
        searchQuery,
        selectedTab,
        selectedGenre,
        selectedYear,
        selectedSourceId,
        sortOption
    ) {
        var list = movies.filter { movie ->
            val matchesQuery = SearchNormalizer.matches(
                searchQuery,
                movie.title,
                movie.titleAr,
                movie.originalTitle,
                movie.director,
                movie.castMembers,
                movie.genres,
                movie.releaseYear.toString()
            )
            val matchesGenre = selectedGenre == "ALL" || movie.genres.contains(selectedGenre, ignoreCase = true)
            val matchesYear = selectedYear == 0 || movie.releaseYear == selectedYear
            val matchesSource = selectedSourceId == null || movie.sourceId == selectedSourceId
            matchesQuery && matchesGenre && matchesYear && matchesSource
        }

        list = when (selectedTab) {
            1 -> list.sortedByDescending { it.addedTimestamp }.take(20)
            2 -> list.sortedByDescending { it.releaseYear }
            else -> list
        }

        when (sortOption) {
            SortOption.DATE_ADDED -> list.sortedByDescending { it.addedTimestamp }
            SortOption.RATING -> list.sortedByDescending { it.rating }
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.RELEASE_YEAR -> list.sortedByDescending { it.releaseYear }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack)
            .statusBarsPadding()
    ) {
        // Header & Search Bar
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.navMovies,
                    style = MaterialTheme.typography.displaySmall,
                    color = KrasWhite
                )
                // Sort selector pill
                Surface(
                    color = KrasCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, KrasCardBorder),
                    modifier = Modifier.clickable {
                        val values = SortOption.entries
                        sortOption = values[(sortOption.ordinal + 1) % values.size]
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = KrasRed
                        )
                        Text(
                            text = if (strings.isArabic) sortOption.labelAr else sortOption.labelEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasWhite,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder, color = KrasMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KrasSilver) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = KrasSilver)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = KrasCharcoal,
                    unfocusedContainerColor = KrasCharcoal,
                    focusedBorderColor = KrasRed,
                    unfocusedBorderColor = KrasCardBorder,
                    focusedTextColor = KrasWhite,
                    unfocusedTextColor = KrasWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movies_search_input")
            )
        }

        // Category Mode Tabs: All Imported | Latest 20 | Recently Released
        val tabs = listOf(
            if (strings.isArabic) "كل الأفلام (${movies.size})" else "All Movies (${movies.size})",
            strings.latest20,
            strings.recentlyReleased
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.indices.toList()) { idx ->
                val active = selectedTab == idx
                Surface(
                    color = if (active) KrasRed else KrasCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) KrasRed else KrasCardBorder),
                    modifier = Modifier.clickable { selectedTab = idx }
                ) {
                    Text(
                        text = tabs[idx],
                        style = MaterialTheme.typography.labelLarge,
                        color = KrasWhite,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Genre + Year + Source Filters Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableGenres) { genre ->
                val active = selectedGenre == genre
                Surface(
                    color = if (active) KrasRed.copy(alpha = 0.25f) else KrasCharcoal,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (active) KrasRed else KrasCardBorder),
                    modifier = Modifier.clickable { selectedGenre = genre }
                ) {
                    Text(
                        text = if (genre == "ALL") strings.allGenres else genre,
                        style = MaterialTheme.typography.labelMedium,
                        color = KrasWhite,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            items(availableYears) { yr ->
                val active = selectedYear == yr
                Surface(
                    color = if (active) KrasRed.copy(alpha = 0.25f) else KrasCharcoal,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (active) KrasRed else KrasCardBorder),
                    modifier = Modifier.clickable { selectedYear = yr }
                ) {
                    Text(
                        text = if (yr == 0) strings.allYears else yr.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = KrasSilver,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            items(sources, key = { it.id }) { src ->
                val active = selectedSourceId == src.id
                Surface(
                    color = if (active) KrasRed.copy(alpha = 0.25f) else KrasCharcoal,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (active) KrasRed else KrasCardBorder),
                    modifier = Modifier.clickable {
                        selectedSourceId = if (active) null else src.id
                    }
                ) {
                    Text(
                        text = src.name.take(18),
                        style = MaterialTheme.typography.labelMedium,
                        color = KrasSilver,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Responsive Poster Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 115.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().testTag("movies_poster_grid")
        ) {
            items(filteredMovies, key = { it.id }) { movie ->
                CinematicPosterCard(
                    item = movie,
                    strings = strings,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}
