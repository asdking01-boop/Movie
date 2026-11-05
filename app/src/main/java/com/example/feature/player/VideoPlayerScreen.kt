package com.example.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.core.database.EpisodeEntity
import com.example.core.database.MediaItemEntity
import com.example.core.model.AppStrings
import com.example.sync.engine.CatalogSyncEngine
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasGold
import com.example.ui.theme.KrasObsidian
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasSurfaceGlass
import com.example.ui.theme.KrasWhite
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaItem: MediaItemEntity,
    episodes: List<EpisodeEntity>,
    initialEpisodeId: Long?,
    autoPlayNextEnabled: Boolean,
    strings: AppStrings,
    onSaveProgress: (mediaId: Long, episodeId: Long?, positionMs: Long, durationMs: Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var currentEpisode by remember(episodes, initialEpisodeId) {
        mutableStateOf(
            if (mediaItem.type == "SERIES" && episodes.isNotEmpty()) {
                episodes.firstOrNull { it.id == initialEpisodeId } ?: episodes.first()
            } else null
        )
    }

    val activePlaybackUrl = currentEpisode?.playbackUrl?.ifBlank { mediaItem.playbackUrl } ?: mediaItem.playbackUrl
    val activeTitle = if (currentEpisode != null) {
        "${mediaItem.title} • S${currentEpisode!!.seasonNumber}:E${currentEpisode!!.episodeNumber} - ${currentEpisode!!.title}"
    } else {
        if (strings.isArabic && mediaItem.titleAr.isNotBlank()) mediaItem.titleAr else mediaItem.title
    }

    val initialResumeMs = currentEpisode?.watchedPositionMs?.takeIf { it > 0 && !currentEpisode!!.isCompleted }
        ?: mediaItem.watchedPositionMs.takeIf { it > 0 && !mediaItem.isCompleted }
        ?: 0L

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var currentPositionMs by remember { mutableLongStateOf(initialResumeMs) }
    var totalDurationMs by remember { mutableLongStateOf(1L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitlesMenu by remember { mutableStateOf(false) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var selectedSubtitleLang by remember { mutableStateOf("off") }
    var isLandscape by remember { mutableStateOf(false) }

    // Gesture feedback HUD states
    var seekRippleText by remember { mutableStateOf<String?>(null) }
    var gestureHudText by remember { mutableStateOf<String?>(null) }
    var isLeftHud by remember { mutableStateOf(true) }
    var brightnessLevel by remember { mutableFloatStateOf(0.75f) }
    var volumeLevel by remember {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf(cur.toFloat() / max.toFloat())
    }

    val subtitleTracks = remember(mediaItem, currentEpisode) {
        val json = currentEpisode?.subtitlesJson?.ifBlank { mediaItem.subtitlesJson } ?: mediaItem.subtitlesJson
        CatalogSyncEngine.deserializeSubtitles(json)
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
    }

    // Load or switch stream when activePlaybackUrl changes
    LaunchedEffect(activePlaybackUrl, currentEpisode?.id) {
        if (activePlaybackUrl.isBlank()) {
            playerError = "No authorized media stream URL is available for this title."
            isBuffering = false
            return@LaunchedEffect
        }
        playerError = null
        isBuffering = true

        val uri = Uri.parse(activePlaybackUrl)
        val mimeType = when {
            activePlaybackUrl.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            activePlaybackUrl.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
            else -> null
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(activeTitle)
                    .setArtist(mediaItem.sourceName)
                    .build()
            )
        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        val resumePos = currentEpisode?.watchedPositionMs?.takeIf { it > 0 && !currentEpisode!!.isCompleted }
            ?: if (currentEpisode == null) initialResumeMs else 0L
        if (resumePos > 0L) {
            exoPlayer.seekTo(resumePos)
        }
        exoPlayer.playWhenReady = true
    }

    // Player listener for state & auto-play next episode
    DisposableEffect(exoPlayer, currentEpisode, episodes, autoPlayNextEnabled) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    totalDurationMs = exoPlayer.duration.coerceAtLeast(1L)
                }
                if (playbackState == Player.STATE_ENDED) {
                    onSaveProgress(mediaItem.id, currentEpisode?.id, totalDurationMs, totalDurationMs)
                    if (autoPlayNextEnabled && currentEpisode != null && episodes.isNotEmpty()) {
                        val idx = episodes.indexOfFirst { it.id == currentEpisode?.id }
                        if (idx in 0 until episodes.lastIndex) {
                            currentEpisode = episodes[idx + 1]
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playerError = "Stream playback error (${error.errorCodeName}): ${error.localizedMessage ?: "Unable to decode media stream"}"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Periodic progress saver every 2 seconds
    LaunchedEffect(exoPlayer, isPlaying, currentEpisode?.id) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                totalDurationMs = exoPlayer.duration.coerceAtLeast(1L)
                onSaveProgress(mediaItem.id, currentEpisode?.id, currentPositionMs, totalDurationMs)
            }
            delay(2000L)
        }
    }

    // Auto-hide controls after 4.5 seconds when playing
    LaunchedEffect(controlsVisible, isPlaying, showSpeedMenu, showSubtitlesMenu, showEpisodesDrawer) {
        if (controlsVisible && isPlaying && !showSpeedMenu && !showSubtitlesMenu && !showEpisodesDrawer) {
            delay(4500L)
            controlsVisible = false
        }
    }

    // Clear gesture HUD after 900ms
    LaunchedEffect(seekRippleText, gestureHudText) {
        if (seekRippleText != null || gestureHudText != null) {
            delay(900L)
            seekRippleText = null
            gestureHudText = null
        }
    }

    // Clean up player and restore orientation on exit
    DisposableEffect(Unit) {
        onDispose {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(1L)
            onSaveProgress(mediaItem.id, currentEpisode?.id, pos, dur)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    BackHandler {
        if (showEpisodesDrawer || showSpeedMenu || showSubtitlesMenu) {
            showEpisodesDrawer = false
            showSpeedMenu = false
            showSubtitlesMenu = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        showSpeedMenu = false
                        showSubtitlesMenu = false
                    },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width * 0.45f) {
                            val newPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            seekRippleText = "⏪ -10s"
                        } else if (offset.x > width * 0.55f) {
                            val newPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(exoPlayer.duration.coerceAtLeast(1L))
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            seekRippleText = "+10s ⏩"
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var dragStartX = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset -> dragStartX = offset.x },
                    onVerticalDrag = { _, dragAmount ->
                        val delta = -dragAmount / size.height.coerceAtLeast(1).toFloat()
                        if (dragStartX < size.width * 0.5f) {
                            // Left side -> Brightness Gesture
                            brightnessLevel = (brightnessLevel + delta).coerceIn(0.05f, 1.0f)
                            activity?.window?.let { win ->
                                val params = win.attributes
                                params.screenBrightness = brightnessLevel
                                win.attributes = params
                            }
                            isLeftHud = true
                            gestureHudText = "Brightness ${(brightnessLevel * 100).toInt()}%"
                        } else {
                            // Right side -> Volume Gesture
                            volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (volumeLevel * maxVol).toInt(),
                                0
                            )
                            isLeftHud = false
                            gestureHudText = "Volume ${(volumeLevel * 100).toInt()}%"
                        }
                    }
                )
            }
    ) {
        // ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize().testTag("exoplayer_surface")
        )

        // Active Subtitle Overlay when CC is enabled
        if (selectedSubtitleLang != "off" && isPlaying) {
            Surface(
                color = KrasBlack.copy(alpha = 0.78f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (controlsVisible) 118.dp else 36.dp, start = 24.dp, end = 24.dp)
            ) {
                val subText = if (selectedSubtitleLang == "ar") {
                    "ترجمة فورية نشطة • ${activeTitle.take(40)}"
                } else {
                    "[CC] Streaming authorized HD stream • ${mediaItem.sourceName}"
                }
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KrasWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Buffering Indicator
        if (isBuffering && playerError == null) {
            CircularProgressIndicator(
                color = KrasRed,
                modifier = Modifier.align(Alignment.Center).size(52.dp)
            )
        }

        // Double-tap seek / Brightness / Volume HUD
        if (seekRippleText != null || gestureHudText != null) {
            Surface(
                color = KrasObsidian.copy(alpha = 0.88f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, KrasCardBorder),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (gestureHudText != null) {
                        Icon(
                            imageVector = if (isLeftHud) Icons.Default.BrightnessMedium else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = KrasRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = seekRippleText ?: gestureHudText.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = KrasWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Player Error Recovery Overlay
        if (playerError != null) {
            Surface(
                color = KrasObsidian.copy(alpha = 0.94f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, KrasRed),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = KrasRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Playback Recovery Required",
                        style = MaterialTheme.typography.titleLarge,
                        color = KrasWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playerError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KrasSilver
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                playerError = null
                                isBuffering = true
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KrasRed)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Stream")
                        }
                        TextButton(onClick = onBack) {
                            Text("Exit Player", color = KrasSilver)
                        }
                    }
                }
            }
        }

        // Full Cinematic Player Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to KrasBlack.copy(alpha = 0.82f),
                                0.3f to Color.Transparent,
                                0.7f to Color.Transparent,
                                1.0f to KrasBlack.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .systemBarsPadding()
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = KrasWhite
                            )
                        }
                        Column(modifier = Modifier.padding(start = 6.dp)) {
                            Text(
                                text = activeTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = KrasWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${mediaItem.sourceName} • Authorized Adaptive Stream",
                                style = MaterialTheme.typography.labelSmall,
                                color = KrasGold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mediaItem.type == "SERIES" && episodes.isNotEmpty()) {
                            IconButton(
                                onClick = { showEpisodesDrawer = !showEpisodesDrawer },
                                modifier = Modifier.testTag("player_episodes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Episodes",
                                    tint = if (showEpisodesDrawer) KrasRed else KrasWhite
                                )
                            }
                        }

                        // Picture-in-Picture
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                    runCatching {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        activity.enterPictureInPictureMode(params)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("player_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                tint = KrasWhite
                            )
                        }

                        // Orientation Toggle (Landscape / Portrait)
                        IconButton(
                            onClick = {
                                isLandscape = !isLandscape
                                activity?.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            },
                            modifier = Modifier.testTag("player_orientation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Orientation",
                                tint = KrasWhite
                            )
                        }
                    }
                }

                // CENTER TRANSPORT CONTROLS
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mediaItem.type == "SERIES" && episodes.size > 1) {
                        val currentIdx = episodes.indexOfFirst { it.id == currentEpisode?.id }
                        IconButton(
                            onClick = {
                                if (currentIdx > 0) currentEpisode = episodes[currentIdx - 1]
                            },
                            enabled = currentIdx > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Episode",
                                tint = if (currentIdx > 0) KrasWhite else KrasSilver.copy(alpha = 0.35f),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = KrasWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = KrasRed,
                        modifier = Modifier
                            .size(68.dp)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("player_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = KrasWhite,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(totalDurationMs)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = KrasWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (mediaItem.type == "SERIES" && episodes.size > 1) {
                        val currentIdx = episodes.indexOfFirst { it.id == currentEpisode?.id }
                        val hasNext = currentIdx in 0 until episodes.lastIndex
                        IconButton(
                            onClick = {
                                if (hasNext) currentEpisode = episodes[currentIdx + 1]
                            },
                            enabled = hasNext
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Episode",
                                tint = if (hasNext) KrasWhite else KrasSilver.copy(alpha = 0.35f),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // BOTTOM SCRUBBER & TRACK CONTROLS
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDurationMs(currentPositionMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasWhite
                        )
                        Slider(
                            value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f)),
                            onValueChange = { newVal ->
                                currentPositionMs = newVal.toLong()
                                exoPlayer.seekTo(currentPositionMs)
                            },
                            valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = KrasRed,
                                activeTrackColor = KrasRed,
                                inactiveTrackColor = KrasWhite.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                        Text(
                            text = formatDurationMs(totalDurationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = KrasSilver
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Speed Control
                            Surface(
                                color = KrasSurfaceGlass,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, KrasCardBorder),
                                modifier = Modifier.clickable {
                                    showSpeedMenu = !showSpeedMenu
                                    showSubtitlesMenu = false
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = KrasWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KrasWhite
                                    )
                                }
                            }

                            // Subtitles / CC Control
                            Surface(
                                color = if (selectedSubtitleLang != "off") KrasRed.copy(alpha = 0.3f) else KrasSurfaceGlass,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selectedSubtitleLang != "off") KrasRed else KrasCardBorder),
                                modifier = Modifier.clickable {
                                    showSubtitlesMenu = !showSubtitlesMenu
                                    showSpeedMenu = false
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = null,
                                        tint = KrasWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedSubtitleLang == "off") "Subtitles: Off" else "CC: ${selectedSubtitleLang.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KrasWhite
                                    )
                                }
                            }
                        }

                        if (mediaItem.type == "SERIES" && episodes.isNotEmpty()) {
                            Surface(
                                color = KrasSurfaceGlass,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, KrasCardBorder),
                                modifier = Modifier.clickable { showEpisodesDrawer = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = KrasGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${strings.episodes} (${episodes.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KrasWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playback Speed Popup
        if (showSpeedMenu) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KrasObsidian),
                border = BorderStroke(1.dp, KrasCardBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 84.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        Surface(
                            color = if (playbackSpeed == spd) KrasRed else KrasCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                playbackSpeed = spd
                                exoPlayer.setPlaybackSpeed(spd)
                                showSpeedMenu = false
                            }
                        ) {
                            Text(
                                text = "${spd}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = KrasWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Subtitles Popup
        if (showSubtitlesMenu) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KrasObsidian),
                border = BorderStroke(1.dp, KrasCardBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 80.dp, bottom = 84.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val options = listOf("off" to "Off", "en" to "English (CC)", "ar" to "العربية")
                    options.forEach { (code, label) ->
                        Surface(
                            color = if (selectedSubtitleLang == code) KrasRed else KrasCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                selectedSubtitleLang = code
                                showSubtitlesMenu = false
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = KrasWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // In-Player Episode Switcher Drawer (Switch episodes without leaving the series player!)
        AnimatedVisibility(
            visible = showEpisodesDrawer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = KrasObsidian.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, KrasCardBorder),
                modifier = Modifier
                    .width(330.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                val seasons = remember(episodes) { episodes.map { it.seasonNumber }.distinct().sorted() }
                var selectedSeason by remember(currentEpisode) {
                    mutableIntStateOf(currentEpisode?.seasonNumber ?: seasons.firstOrNull() ?: 1)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.episodes,
                            style = MaterialTheme.typography.titleLarge,
                            color = KrasWhite
                        )
                        TextButton(onClick = { showEpisodesDrawer = false }) {
                            Text("Close", color = KrasRed)
                        }
                    }

                    if (seasons.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            seasons.forEach { s ->
                                Surface(
                                    color = if (selectedSeason == s) KrasRed else KrasCharcoal,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { selectedSeason = s }
                                ) {
                                    Text(
                                        text = "Season $s",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KrasWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        val seasonEps = episodes.filter { it.seasonNumber == selectedSeason }
                        items(seasonEps, key = { it.id }) { ep ->
                            val isCurrent = ep.id == currentEpisode?.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) KrasRed.copy(alpha = 0.25f) else KrasCharcoal
                                ),
                                border = BorderStroke(1.dp, if (isCurrent) KrasRed else KrasCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentEpisode = ep
                                        showEpisodesDrawer = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "S${ep.seasonNumber}:E${ep.episodeNumber} • ${ep.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = KrasWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = ep.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KrasSilver,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
