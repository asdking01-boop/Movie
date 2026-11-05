package com.example.sync.engine

import com.example.core.database.ContentSourceEntity
import com.example.core.database.EpisodeEntity
import com.example.core.database.KrasDao
import com.example.core.database.MediaItemEntity
import com.example.core.database.SyncLogEntity
import com.example.core.model.ContentType
import com.example.core.model.SourceConnectionStatus
import com.example.core.model.SubtitleTrack
import com.example.datasource.connectors.SourceConnectorRegistry
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

data class SyncSummary(
    val sourceId: Long,
    val sourceName: String,
    val isSuccess: Boolean,
    val moviesAdded: Int = 0,
    val seriesAdded: Int = 0,
    val episodesAdded: Int = 0,
    val message: String
)

class CatalogSyncEngine(private val dao: KrasDao) {

    /**
     * Synchronizes a single ContentSourceEntity with exponential backoff retry for transient or rate-limit errors.
     * Never deletes existing catalog items if synchronization fails.
     */
    suspend fun synchronizeSource(sourceId: Long, maxRetries: Int = 2): SyncSummary {
        val startTime = System.currentTimeMillis()
        val source = dao.getSourceById(sourceId)
            ?: return SyncSummary(sourceId, "Unknown", false, message = "Source not found.")

        if (!source.isEnabled) {
            return SyncSummary(source.id, source.name, false, message = "Source is currently disabled.")
        }

        val connector = SourceConnectorRegistry.getConnector(source.sourceType)

        // Mark source as SYNCING
        dao.updateSource(
            source.copy(
                connectionStatus = SourceConnectionStatus.SYNCING.name,
                syncProgress = 0.12f,
                lastSyncError = null
            )
        )

        var attempt = 0
        var fetchResult = connector.fetchCatalog(source)
        while (!fetchResult.isSuccess && fetchResult.isRateLimited && attempt < maxRetries) {
            attempt++
            val backoffMs = 1000L * (1 shl attempt)
            delay(backoffMs)
            fetchResult = connector.fetchCatalog(source)
        }

        if (!fetchResult.isSuccess) {
            val errMsg = fetchResult.errorMessage ?: "Failed to synchronize source."
            val updatedSource = source.copy(
                connectionStatus = SourceConnectionStatus.ERROR.name,
                syncProgress = 0f,
                lastSyncError = errMsg
            )
            dao.updateSource(updatedSource)
            dao.insertSyncLog(
                SyncLogEntity(
                    sourceId = source.id,
                    sourceName = source.name,
                    timestamp = System.currentTimeMillis(),
                    isSuccess = false,
                    durationMs = System.currentTimeMillis() - startTime,
                    message = "Sync failed (existing catalog preserved): $errMsg"
                )
            )
            return SyncSummary(source.id, source.name, false, message = errMsg)
        }

        val items = fetchResult.items
        var newMovies = 0
        var newSeries = 0
        var newEpisodes = 0

        val total = items.size.coerceAtLeast(1)
        items.forEachIndexed { index, dto ->
            val progress = 0.2f + (0.75f * ((index + 1).toFloat() / total.toFloat()))
            dao.updateSource(
                source.copy(
                    connectionStatus = SourceConnectionStatus.SYNCING.name,
                    syncProgress = progress
                )
            )

            // 1. Check existing by externalId + sourceId
            var existing = dao.getMediaByExternalId(dto.externalId, source.id)
            // 2. Fallback conservative duplicate check by Title + Release Year + Type
            if (existing == null) {
                existing = dao.findDuplicateByTitleAndYear(dto.title, dto.releaseYear, dto.type.name)
            }

            val seasonsCount = if (dto.type == ContentType.SERIES) {
                dto.episodes.map { it.seasonNumber }.distinct().size.coerceAtLeast(1)
            } else 0

            val subtitlesJson = serializeSubtitles(dto.subtitles)

            val mediaEntity = if (existing != null) {
                existing.copy(
                    title = dto.title,
                    titleAr = dto.titleAr.ifBlank { existing.titleAr },
                    originalTitle = dto.originalTitle.ifBlank { existing.originalTitle },
                    description = dto.description,
                    descriptionAr = dto.descriptionAr.ifBlank { existing.descriptionAr },
                    posterUrl = dto.posterUrl,
                    backdropUrl = dto.backdropUrl,
                    localPosterRes = dto.localPosterRes.ifBlank { existing.localPosterRes },
                    localBackdropRes = dto.localBackdropRes.ifBlank { existing.localBackdropRes },
                    releaseYear = dto.releaseYear,
                    genres = dto.genres.joinToString(","),
                    durationMinutes = dto.durationMinutes,
                    language = dto.language,
                    rating = dto.rating,
                    director = dto.director,
                    castMembers = dto.cast.joinToString(","),
                    playbackUrl = dto.playbackUrl.ifBlank { existing.playbackUrl },
                    subtitlesJson = subtitlesJson,
                    updatedTimestamp = System.currentTimeMillis(),
                    isFeatured = dto.isFeatured || existing.isFeatured,
                    isTrending = dto.isTrending || existing.isTrending,
                    isPopular = dto.isPopular || existing.isPopular,
                    seasonCount = seasonsCount,
                    episodeCount = dto.episodes.size.coerceAtLeast(existing.episodeCount)
                )
            } else {
                if (dto.type == ContentType.MOVIE) newMovies++ else newSeries++
                MediaItemEntity(
                    externalId = dto.externalId,
                    sourceId = source.id,
                    sourceName = source.name,
                    isSampleContent = source.isSampleSource,
                    type = dto.type.name,
                    title = dto.title,
                    titleAr = dto.titleAr,
                    originalTitle = dto.originalTitle,
                    description = dto.description,
                    descriptionAr = dto.descriptionAr,
                    posterUrl = dto.posterUrl,
                    backdropUrl = dto.backdropUrl,
                    localPosterRes = dto.localPosterRes,
                    localBackdropRes = dto.localBackdropRes,
                    releaseYear = dto.releaseYear,
                    genres = dto.genres.joinToString(","),
                    durationMinutes = dto.durationMinutes,
                    language = dto.language,
                    rating = dto.rating,
                    director = dto.director,
                    castMembers = dto.cast.joinToString(","),
                    playbackUrl = dto.playbackUrl,
                    subtitlesJson = subtitlesJson,
                    isFeatured = dto.isFeatured,
                    isTrending = dto.isTrending,
                    isPopular = dto.isPopular,
                    seasonCount = seasonsCount,
                    episodeCount = dto.episodes.size
                )
            }

            val savedMediaId = dao.insertMediaItem(mediaEntity)
            val resolvedSeriesId = existing?.id ?: savedMediaId

            if (dto.type == ContentType.SERIES && dto.episodes.isNotEmpty()) {
                for (epDto in dto.episodes) {
                    val existingEp = dao.getEpisodeByNumber(
                        seriesId = resolvedSeriesId,
                        season = epDto.seasonNumber,
                        episode = epDto.episodeNumber
                    )
                    if (existingEp == null) {
                        newEpisodes++
                        dao.insertEpisode(
                            EpisodeEntity(
                                externalId = epDto.externalId,
                                seriesId = resolvedSeriesId,
                                sourceId = source.id,
                                seasonNumber = epDto.seasonNumber,
                                episodeNumber = epDto.episodeNumber,
                                title = epDto.title,
                                description = epDto.description,
                                thumbnailUrl = epDto.thumbnailUrl,
                                durationMinutes = epDto.durationMinutes,
                                playbackUrl = epDto.playbackUrl,
                                subtitlesJson = serializeSubtitles(epDto.subtitles),
                                releaseDate = epDto.releaseDate
                            )
                        )
                    } else {
                        dao.updateEpisode(
                            existingEp.copy(
                                title = epDto.title,
                                description = epDto.description,
                                thumbnailUrl = epDto.thumbnailUrl,
                                durationMinutes = epDto.durationMinutes,
                                playbackUrl = epDto.playbackUrl,
                                subtitlesJson = serializeSubtitles(epDto.subtitles),
                                releaseDate = epDto.releaseDate
                            )
                        )
                    }
                }
            }
        }

        val totalMovieCount = dao.countMoviesForSource(source.id)
        val totalSeriesCount = dao.countSeriesForSource(source.id)
        val capabilitiesStr = fetchResult.detectedCapabilities.joinToString(",") { it.name }
        val summaryMsg = "Synced $totalMovieCount movies & $totalSeriesCount series (+$newMovies new movies, +$newSeries new series, +$newEpisodes new episodes)."

        dao.updateSource(
            source.copy(
                connectionStatus = SourceConnectionStatus.CONNECTED.name,
                detectedCapabilities = capabilitiesStr,
                importedMovieCount = totalMovieCount,
                importedSeriesCount = totalSeriesCount,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncError = null,
                syncProgress = 1.0f
            )
        )

        dao.insertSyncLog(
            SyncLogEntity(
                sourceId = source.id,
                sourceName = source.name,
                timestamp = System.currentTimeMillis(),
                isSuccess = true,
                moviesAdded = newMovies,
                seriesAdded = newSeries,
                episodesAdded = newEpisodes,
                durationMs = System.currentTimeMillis() - startTime,
                message = summaryMsg
            )
        )

        return SyncSummary(
            sourceId = source.id,
            sourceName = source.name,
            isSuccess = true,
            moviesAdded = newMovies,
            seriesAdded = newSeries,
            episodesAdded = newEpisodes,
            message = summaryMsg
        )
    }

    suspend fun synchronizeAllEnabledSources(): List<SyncSummary> {
        val enabled = dao.getEnabledSourcesOnce()
        return enabled.map { synchronizeSource(it.id) }
    }

    companion object {
        fun serializeSubtitles(tracks: List<SubtitleTrack>): String {
            val arr = JSONArray()
            tracks.forEach { t ->
                val obj = JSONObject()
                obj.put("languageCode", t.languageCode)
                obj.put("label", t.label)
                obj.put("url", t.url)
                obj.put("mimeType", t.mimeType)
                arr.put(obj)
            }
            return arr.toString()
        }

        fun deserializeSubtitles(json: String): List<SubtitleTrack> {
            if (json.isBlank()) return emptyList()
            val list = mutableListOf<SubtitleTrack>()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    list.add(
                        SubtitleTrack(
                            languageCode = obj.optString("languageCode", "en"),
                            label = obj.optString("label", "English"),
                            url = obj.optString("url", ""),
                            mimeType = obj.optString("mimeType", "text/vtt")
                        )
                    )
                }
            } catch (_: Exception) {
            }
            return list
        }
    }
}
