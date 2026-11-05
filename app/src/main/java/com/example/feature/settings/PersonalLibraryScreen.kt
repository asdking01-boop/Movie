package com.example.feature.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.database.EpisodeEntity
import com.example.core.database.MediaItemEntity
import com.example.core.model.AppStrings
import com.example.core.ui.CinematicPosterCard
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasEmerald
import com.example.ui.theme.KrasMuted
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasWhite

@Composable
fun PersonalLibraryScreen(
    myList: List<MediaItemEntity>,
    favorites: List<MediaItemEntity>,
    continueWatching: List<MediaItemEntity>,
    watchHistory: List<MediaItemEntity>,
    completedEpisodes: List<EpisodeEntity>,
    unwatchedEpisodes: List<EpisodeEntity>,
    strings: AppStrings,
    onMediaClick: (MediaItemEntity) -> Unit,
    onEpisodeClick: (EpisodeEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "${strings.navLibrary} (${myList.size})",
        "${strings.favorites} (${favorites.size})",
        "${strings.continueWatching} (${continueWatching.size})",
        "${strings.watchHistory} (${watchHistory.size})",
        "${strings.completedEpisodes} (${completedEpisodes.size})",
        "${strings.unwatchedEpisodes} (${unwatchedEpisodes.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack)
            .statusBarsPadding()
    ) {
        Text(
            text = strings.navLibrary,
            style = MaterialTheme.typography.displaySmall,
            color = KrasWhite,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
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
                    modifier = Modifier
                        .clickable { selectedTab = idx }
                        .testTag("library_tab_$idx")
                ) {
                    Text(
                        text = tabs[idx],
                        style = MaterialTheme.typography.labelLarge,
                        color = KrasWhite,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0, 1, 2, 3 -> {
                val activeList = when (selectedTab) {
                    0 -> myList
                    1 -> favorites
                    2 -> continueWatching
                    else -> watchHistory
                }

                if (activeList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = KrasMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (strings.isArabic) "لا توجد عناصر محفوظة في هذا القسم بعد" else "No titles saved in this section yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = KrasSilver
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 115.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(activeList, key = { it.id }) { media ->
                            CinematicPosterCard(
                                item = media,
                                strings = strings,
                                onClick = { onMediaClick(media) }
                            )
                        }
                    }
                }
            }
            else -> {
                val epList = if (selectedTab == 4) completedEpisodes else unwatchedEpisodes
                if (epList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (strings.isArabic) "لا توجد حلقات في هذه القائمة" else "No episodes in this list",
                            style = MaterialTheme.typography.bodyLarge,
                            color = KrasSilver
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(epList, key = { it.id }) { ep ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = KrasCharcoal),
                                border = BorderStroke(1.dp, KrasCardBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEpisodeClick(ep) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Icon(
                                        imageVector = if (ep.isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircleOutline,
                                        contentDescription = null,
                                        tint = if (ep.isCompleted) KrasEmerald else KrasRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "S${ep.seasonNumber}:E${ep.episodeNumber} • ${ep.title}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = KrasWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ep.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = KrasSilver,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${ep.durationMinutes}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KrasMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
