package com.example.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.database.ContentSourceEntity
import com.example.core.database.MediaItemEntity
import com.example.core.database.RecentSearchEntity
import com.example.core.model.AppStrings
import com.example.core.model.SearchNormalizer
import com.example.core.ui.CinematicPosterCard
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasGold
import com.example.ui.theme.KrasMuted
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    allMedia: List<MediaItemEntity>,
    sources: List<ContentSourceEntity>,
    recentSearches: List<RecentSearchEntity>,
    strings: AppStrings,
    onItemClick: (MediaItemEntity) -> Unit,
    onSaveRecentSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, MOVIE, SERIES
    var selectedGenre by remember { mutableStateOf("ALL") }
    var selectedSourceId by remember { mutableStateOf<Long?>(null) }

    val suggestions = remember(allMedia) {
        val list = mutableSetOf<String>()
        allMedia.forEach { item ->
            if (item.director.isNotBlank()) list.add(item.director)
            item.castMembers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(2).forEach { list.add(it) }
            item.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { list.add(it) }
        }
        list.take(10).toList()
    }

    val genres = remember(allMedia) {
        val set = linkedSetOf("ALL")
        allMedia.forEach { m ->
            m.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { set.add(it) }
        }
        set.toList()
    }

    val results = remember(allMedia, query, selectedTypeFilter, selectedGenre, selectedSourceId) {
        allMedia.filter { item ->
            val matchesText = SearchNormalizer.matches(
                query,
                item.title,
                item.titleAr,
                item.originalTitle,
                item.director,
                item.castMembers,
                item.genres,
                item.releaseYear.toString(),
                item.sourceName
            )
            val matchesType = selectedTypeFilter == "ALL" || item.type == selectedTypeFilter
            val matchesGenre = selectedGenre == "ALL" || item.genres.contains(selectedGenre, ignoreCase = true)
            val matchesSource = selectedSourceId == null || item.sourceId == selectedSourceId
            matchesText && matchesType && matchesGenre && matchesSource
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = strings.navSearch,
                style = MaterialTheme.typography.displaySmall,
                color = KrasWhite
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.searchPlaceholder, color = KrasMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = KrasRed)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = KrasSilver)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSaveRecentSearch(query)
                        focusManager.clearFocus()
                    }
                ),
                shape = RoundedCornerShape(14.dp),
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
                    .testTag("unified_search_input")
            )
        }

        // Content Type + Source + Genre Filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val typeChips = listOf(
                "ALL" to (if (strings.isArabic) "الكل" else "All"),
                "MOVIE" to strings.navMovies,
                "SERIES" to strings.navSeries
            )
            items(typeChips) { (code, label) ->
                val active = selectedTypeFilter == code
                Surface(
                    color = if (active) KrasRed else KrasCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) KrasRed else KrasCardBorder),
                    modifier = Modifier.clickable { selectedTypeFilter = code }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = KrasWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            items(sources, key = { it.id }) { src ->
                val active = selectedSourceId == src.id
                Surface(
                    color = if (active) KrasRed.copy(alpha = 0.25f) else KrasCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) KrasRed else KrasCardBorder),
                    modifier = Modifier.clickable {
                        selectedSourceId = if (active) null else src.id
                    }
                ) {
                    Text(
                        text = src.name.take(16),
                        style = MaterialTheme.typography.labelMedium,
                        color = KrasSilver,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            items(genres) { g ->
                val active = selectedGenre == g
                Surface(
                    color = if (active) KrasRed.copy(alpha = 0.25f) else KrasCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) KrasRed else KrasCardBorder),
                    modifier = Modifier.clickable { selectedGenre = g }
                ) {
                    Text(
                        text = if (g == "ALL") strings.allGenres else g,
                        style = MaterialTheme.typography.labelMedium,
                        color = KrasSilver,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Recent Searches & Smart Suggestions when query is empty
        if (query.isBlank()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isArabic) "عمليات البحث الأخيرة" else "Recent Searches",
                            style = MaterialTheme.typography.titleSmall,
                            color = KrasSilver
                        )
                        TextButton(onClick = onClearRecentSearches) {
                            Text(if (strings.isArabic) "مسح الكل" else "Clear", color = KrasRed)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentSearches, key = { it.query }) { rec ->
                            Surface(
                                color = KrasCharcoal,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, KrasCardBorder),
                                modifier = Modifier.clickable { query = rec.query }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = KrasMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rec.query,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KrasWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = KrasMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveRecentSearch(rec.query) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Suggestions
                if (suggestions.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = KrasGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (strings.isArabic) "اقتراحات البحث (ممثلين، مخرجين، تصنيفات)" else "Quick Suggestions (Directors, Cast, Genres)",
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasSilver
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { sug ->
                            Surface(
                                color = KrasCharcoal,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, KrasCardBorder),
                                modifier = Modifier.clickable {
                                    query = sug
                                    onSaveRecentSearch(sug)
                                }
                            ) {
                                Text(
                                    text = sug,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KrasSilver,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Results Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 115.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().testTag("search_results_grid")
        ) {
            items(results, key = { it.id }) { item ->
                CinematicPosterCard(
                    item = item,
                    strings = strings,
                    onClick = {
                        if (query.isNotBlank()) onSaveRecentSearch(query)
                        onItemClick(item)
                    }
                )
            }
        }
    }
}
