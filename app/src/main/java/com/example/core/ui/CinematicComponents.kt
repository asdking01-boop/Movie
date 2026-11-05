package com.example.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.database.MediaItemEntity
import com.example.core.model.AppStrings
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasEmerald
import com.example.ui.theme.KrasGold
import com.example.ui.theme.KrasMuted
import com.example.ui.theme.KrasObsidian
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasSurfaceElevated
import com.example.ui.theme.KrasSurfaceGlass
import com.example.ui.theme.KrasTeal
import com.example.ui.theme.KrasWhite

@Composable
fun resolveLocalDrawableId(resName: String): Int {
    if (resName.isBlank()) return 0
    return when {
        resName.contains("img_hero_cosmos") -> R.drawable.img_hero_cosmos_1793910454623
        resName.contains("img_hero_cyber_heist") -> R.drawable.img_hero_cyber_heist_1793910455808
        resName.contains("img_poster_horizon") -> R.drawable.img_poster_horizon_1793910456490
        resName.contains("img_poster_solaris") -> R.drawable.img_poster_solaris_1793910476382
        else -> 0
    }
}

@Composable
fun KrasArtwork(
    imageUrl: String,
    localResName: String = "",
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val localId = resolveLocalDrawableId(localResName)
    if (localId != 0) {
        Image(
            painter = painterResource(id = localId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            },
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            },
            error = {
                Image(
                    painter = painterResource(id = R.drawable.img_poster_horizon_1793910456490),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        )
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            KrasCharcoal,
            KrasSurfaceElevated,
            KrasCharcoal
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )
    Box(modifier = modifier.background(brush))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CinematicHeroCarousel(
    featuredItems: List<MediaItemEntity>,
    strings: AppStrings,
    onPlayClick: (MediaItemEntity) -> Unit,
    onDetailsClick: (MediaItemEntity) -> Unit,
    onToggleMyList: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredItems.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { featuredItems.size })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("hero_banner_pager")
        ) { page ->
            val item = featuredItems[page]
            val displayTitle = if (strings.isArabic && item.titleAr.isNotBlank()) item.titleAr else item.title
            val displayDesc = if (strings.isArabic && item.descriptionAr.isNotBlank()) item.descriptionAr else item.description

            Box(modifier = Modifier.fillMaxSize()) {
                KrasArtwork(
                    imageUrl = item.backdropUrl.ifBlank { item.posterUrl },
                    localResName = item.localBackdropRes.ifBlank { item.localPosterRes },
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize()
                )

                // Deep multi-stop cinematic gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to KrasBlack.copy(alpha = 0.65f),
                                    0.35f to Color.Transparent,
                                    0.72f to KrasBlack.copy(alpha = 0.86f),
                                    1.0f to KrasBlack
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Provenance + Type Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SourceProvenanceBadge(
                            isSample = item.isSampleContent,
                            sourceName = item.sourceName,
                            strings = strings
                        )
                        Surface(
                            color = KrasRed.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, KrasRed.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = if (item.type == "SERIES") {
                                    if (strings.isArabic) "مسلسل • ${item.seasonCount} مواسم" else "SERIES • ${item.seasonCount} SEASONS"
                                } else {
                                    if (strings.isArabic) "فيلم سينمائي" else "FEATURE FILM"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = KrasGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", item.rating),
                                style = MaterialTheme.typography.labelMedium,
                                color = KrasGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${item.releaseYear}",
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasSilver
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.displayMedium,
                        color = KrasWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Genres row
                    val genres = item.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (genres.isNotEmpty()) {
                        Text(
                            text = genres.joinToString("  •  "),
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasSilver
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = displayDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KrasSilver.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Action Buttons: Play, More Info, My List
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasValidStream = item.playbackUrl.isNotBlank() || item.type == "SERIES"
                        if (hasValidStream) {
                            Button(
                                onClick = { onPlayClick(item) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KrasRed,
                                    contentColor = KrasWhite
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .testTag("hero_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = strings.play,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (item.watchedPositionMs > 0 && !item.isCompleted) strings.resume else strings.play,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onDetailsClick(item) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = KrasSurfaceGlass,
                                contentColor = KrasWhite
                            ),
                            border = BorderStroke(1.dp, KrasCardBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hero_more_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = strings.moreInfo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.moreInfo,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = { onToggleMyList(item) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (item.inMyList) KrasRed.copy(alpha = 0.25f) else KrasSurfaceGlass,
                                contentColor = KrasWhite
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (item.inMyList) KrasRed else KrasCardBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("hero_mylist_button")
                        ) {
                            Icon(
                                imageVector = if (item.inMyList) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = strings.addToMyList,
                                tint = if (item.inMyList) KrasRed else KrasWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceProvenanceBadge(
    isSample: Boolean,
    sourceName: String,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSample) KrasObsidian.copy(alpha = 0.85f) else KrasEmerald.copy(alpha = 0.2f)
    val borderColor = if (isSample) KrasGold.copy(alpha = 0.6f) else KrasEmerald.copy(alpha = 0.7f)
    val textColor = if (isSample) KrasGold else KrasEmerald
    val label = if (isSample) strings.sampleBadge else sourceName.take(18).uppercase()

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CinematicSectionRow(
    title: String,
    items: List<MediaItemEntity>,
    strings: AppStrings,
    onItemClick: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false
) {
    if (items.isEmpty()) return
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(KrasRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = KrasWhite
                )
            }
            Text(
                text = "${items.size}",
                style = MaterialTheme.typography.labelMedium,
                color = KrasMuted
            )
        }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${it.id}-${it.externalId}" }) { item ->
                if (showProgress) {
                    ContinueWatchingPosterCard(
                        item = item,
                        strings = strings,
                        onClick = { onItemClick(item) }
                    )
                } else {
                    CinematicPosterCard(
                        item = item,
                        strings = strings,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun CinematicPosterCard(
    item: MediaItemEntity,
    strings: AppStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTitle = if (strings.isArabic && item.titleAr.isNotBlank()) item.titleAr else item.title

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KrasCharcoal),
        border = BorderStroke(1.dp, KrasCardBorder),
        modifier = modifier
            .width(138.dp)
            .clickable(onClick = onClick)
            .testTag("poster_card_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            ) {
                KrasArtwork(
                    imageUrl = item.posterUrl,
                    localResName = item.localPosterRes,
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize()
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = KrasBlack.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = KrasGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", item.rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!item.isSampleContent) {
                        Surface(
                            color = KrasTeal.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SYNC",
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Bottom gradient + Year/Type
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, KrasBlack.copy(alpha = 0.92f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (item.type == "SERIES") "${item.releaseYear} • ${item.seasonCount}S" else "${item.releaseYear} • ${item.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = KrasSilver
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = KrasWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isSampleContent) KrasMuted else KrasEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingPosterCard(
    item: MediaItemEntity,
    strings: AppStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTitle = if (strings.isArabic && item.titleAr.isNotBlank()) item.titleAr else item.title
    val progress = if (item.totalDurationMs > 0) {
        (item.watchedPositionMs.toFloat() / item.totalDurationMs.toFloat()).coerceIn(0.05f, 1f)
    } else 0.25f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KrasCharcoal),
        border = BorderStroke(1.dp, KrasCardBorder),
        modifier = modifier
            .width(220.dp)
            .clickable(onClick = onClick)
            .testTag("continue_watching_card_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
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
                        .background(KrasBlack.copy(alpha = 0.35f))
                )
                Surface(
                    shape = CircleShape,
                    color = KrasRed.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = strings.resume,
                            tint = KrasWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = KrasRed,
                    trackColor = KrasWhite.copy(alpha = 0.25f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = KrasWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val remainingMin = ((item.totalDurationMs - item.watchedPositionMs).coerceAtLeast(0L) / 60000L).coerceAtLeast(1L)
                Text(
                    text = if (strings.isArabic) "متبقي $remainingMin دقيقة" else "${remainingMin}m remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = KrasSilver
                )
            }
        }
    }
}
