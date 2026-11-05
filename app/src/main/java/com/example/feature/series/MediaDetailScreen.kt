package com.example.feature.series

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.database.EpisodeEntity
import com.example.core.database.MediaItemEntity
import com.example.core.model.AppStrings
import com.example.core.ui.CinematicSectionRow
import com.example.core.ui.KrasArtwork
import com.example.core.ui.SourceProvenanceBadge
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasEmerald
import com.example.ui.theme.KrasGold
import com.example.ui.theme.KrasMuted
import com.example.ui.theme.KrasObsidian
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasSurfaceGlass
import com.example.ui.theme.KrasWhite

@Composable
fun MediaDetailScreen(
    item: MediaItemEntity,
    episodes: List<EpisodeEntity>,
    relatedItems: List<MediaItemEntity>,
    strings: AppStrings,
    onPlayMovieOrEpisode: (episodeId: Long?) -> Unit,
    onToggleMyList: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectRelatedItem: (MediaItemEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val displayTitle = if (strings.isArabic && item.titleAr.isNotBlank()) item.titleAr else item.title
    val displayDesc = if (strings.isArabic && item.descriptionAr.isNotBlank()) item.descriptionAr else item.description
    val isSeries = item.type == "SERIES"

    val seasons = remember(episodes) {
        episodes.map { it.seasonNumber }.distinct().sorted().ifEmpty { listOf(1) }
    }
    var selectedSeason by remember(seasons) { mutableIntStateOf(seasons.first()) }
    val seasonEpisodes = remember(episodes, selectedSeason) {
        episodes.filter { it.seasonNumber == selectedSeason }
    }

    // Determine next or resume episode for series
    val resumeOrNextEpisode = remember(episodes) {
        episodes.firstOrNull { it.watchedPositionMs > 0 && !it.isCompleted }
            ?: episodes.firstOrNull { !it.isCompleted }
            ?: episodes.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                KrasArtwork(
                    imageUrl = item.backdropUrl.ifBlank { item.posterUrl },
                    localResName = item.localBackdropRes.ifBlank { item.localPosterRes },
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to KrasBlack.copy(alpha = 0.6f),
                                    0.4f to Color.Transparent,
                                    0.85f to KrasBlack.copy(alpha = 0.95f),
                                    1.0f to KrasBlack
                                )
                            )
                        )
                )

                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(KrasObsidian.copy(alpha = 0.75f), CircleShape)
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KrasWhite
                    )
                }

                // Bottom Title & Provenance
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceProvenanceBadge(
                            isSample = item.isSampleContent,
                            sourceName = item.sourceName,
                            strings = strings
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = KrasGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", item.rating),
                                style = MaterialTheme.typography.labelLarge,
                                color = KrasGold
                            )
                        }
                        Text(
                            text = "${item.releaseYear}",
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasSilver
                        )
                        Text(
                            text = if (isSeries) "${seasons.size} ${strings.seasons} • ${episodes.size} ${strings.episodes}" else "${item.durationMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasSilver
                        )
                        Surface(
                            color = KrasCharcoal,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, KrasCardBorder)
                        ) {
                            Text(
                                text = item.language.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasWhite,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.displayMedium,
                        color = KrasWhite
                    )

                    if (item.originalTitle.isNotBlank() && item.originalTitle != displayTitle) {
                        Text(
                            text = "Original Title: ${item.originalTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrasMuted
                        )
                    }
                }
            }
        }

        // 2. Action Buttons & Metadata
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                val canPlay = if (isSeries) {
                    resumeOrNextEpisode?.playbackUrl?.isNotBlank() == true
                } else {
                    item.playbackUrl.isNotBlank()
                }

                if (canPlay) {
                    val playLabel = if (isSeries && resumeOrNextEpisode != null) {
                        val prefix = if (resumeOrNextEpisode.watchedPositionMs > 0 && !resumeOrNextEpisode.isCompleted) {
                            strings.resume
                        } else {
                            strings.play
                        }
                        "$prefix S${resumeOrNextEpisode.seasonNumber}:E${resumeOrNextEpisode.episodeNumber} • ${resumeOrNextEpisode.title}"
                    } else if (item.watchedPositionMs > 0 && !item.isCompleted) {
                        strings.resume
                    } else {
                        strings.play
                    }

                    Button(
                        onClick = { onPlayMovieOrEpisode(resumeOrNextEpisode?.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KrasRed,
                            contentColor = KrasWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("detail_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Movie Watch Progress Bar if partially watched
                if (!isSeries && item.watchedPositionMs > 0 && item.totalDurationMs > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val progress = (item.watchedPositionMs.toFloat() / item.totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        color = KrasRed,
                        trackColor = KrasCharcoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleMyList,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (item.inMyList) KrasRed.copy(alpha = 0.2f) else KrasCharcoal,
                            contentColor = KrasWhite
                        ),
                        border = BorderStroke(1.dp, if (item.inMyList) KrasRed else KrasCardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_mylist_button")
                    ) {
                        Icon(
                            imageVector = if (item.inMyList) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (item.inMyList) KrasRed else KrasWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (item.inMyList) strings.inMyList else strings.addToMyList)
                    }

                    OutlinedButton(
                        onClick = onToggleFavorite,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (item.isFavorite) KrasRed.copy(alpha = 0.2f) else KrasCharcoal,
                            contentColor = KrasWhite
                        ),
                        border = BorderStroke(1.dp, if (item.isFavorite) KrasRed else KrasCardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (item.isFavorite) KrasRed else KrasWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.favorites)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Genre Chips
                val genres = item.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genres) { genre ->
                        Surface(
                            color = KrasCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, KrasCardBorder)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = KrasSilver,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = displayDesc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KrasWhite.copy(alpha = 0.92f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Director & Cast Metadata
                if (item.director.isNotBlank()) {
                    Text(
                        text = if (strings.isArabic) "المخرج: ${item.director}" else "Director: ${item.director}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KrasSilver
                    )
                }
                if (item.castMembers.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (strings.isArabic) "البطولة: ${item.castMembers}" else "Cast: ${item.castMembers}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KrasSilver
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Source Attribution: ${item.sourceName} (ID: ${item.externalId})",
                    style = MaterialTheme.typography.labelSmall,
                    color = KrasMuted
                )
            }
        }

        // 3. TV Series Seasons & Episodes Section
        if (isSeries) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text(
                        text = "${strings.seasons} & ${strings.episodes}",
                        style = MaterialTheme.typography.titleLarge,
                        color = KrasWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(seasons) { s ->
                            val selected = s == selectedSeason
                            Surface(
                                color = if (selected) KrasRed else KrasCharcoal,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selected) KrasRed else KrasCardBorder),
                                modifier = Modifier
                                    .clickable { selectedSeason = s }
                                    .testTag("season_selector_$s")
                            ) {
                                Text(
                                    text = if (strings.isArabic) "الموسم $s" else "Season $s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KrasWhite,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(seasonEpisodes, key = { it.id }) { ep ->
                EpisodeRowCard(
                    episode = ep,
                    onClick = { onPlayMovieOrEpisode(ep.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }

        // 4. Related Content Carousel
        if (relatedItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CinematicSectionRow(
                    title = strings.recommendedForYou,
                    items = relatedItems,
                    strings = strings,
                    onItemClick = onSelectRelatedItem
                )
            }
        }
    }
}

@Composable
private fun EpisodeRowCard(
    episode: EpisodeEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (episode.totalDurationMs > 0) {
        (episode.watchedPositionMs.toFloat() / episode.totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = KrasCharcoal),
        border = BorderStroke(1.dp, KrasCardBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("episode_card_${episode.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    KrasArtwork(
                        imageUrl = episode.thumbnailUrl,
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(KrasBlack.copy(alpha = 0.35f))
                    )
                    Surface(
                        shape = CircleShape,
                        color = KrasRed.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Episode",
                                tint = KrasWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (progress > 0f) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = KrasRed,
                            trackColor = KrasWhite.copy(alpha = 0.25f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${episode.episodeNumber}. ${episode.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = KrasWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (episode.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = KrasEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${episode.durationMinutes} min" + if (episode.releaseDate.isNotBlank()) " • ${episode.releaseDate}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = KrasMuted
                    )
                }
            }

            if (episode.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = episode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = KrasSilver,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
