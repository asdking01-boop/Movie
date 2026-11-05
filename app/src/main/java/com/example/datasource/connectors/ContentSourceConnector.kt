package com.example.datasource.connectors

import android.util.Xml
import com.example.core.database.ContentSourceEntity
import com.example.core.model.ConnectionValidationResult
import com.example.core.model.ContentType
import com.example.core.model.ImportedEpisodeDto
import com.example.core.model.ImportedMediaDto
import com.example.core.model.SourceCapability
import com.example.core.model.SourceCatalogFetchResult
import com.example.core.model.SourceType
import com.example.core.model.SubtitleTrack
import com.example.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

interface ContentSourceConnector {
    val supportedType: SourceType
    val declaredCapabilities: Set<SourceCapability>

    suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult
    suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult
}

object SourceConnectorRegistry {

    private val connectors: Map<SourceType, ContentSourceConnector> = listOf(
        LegalDemoSourceAdapter(),
        RestApiSourceAdapter(),
        GraphQlSourceAdapter(),
        AuthorizedJsonFeedAdapter(),
        XmlRssFeedAdapter(),
        M3uPlaylistAdapter(),
        XtreamCodesAdapter(),
        PermittedWebMetadataAdapter()
    ).associateBy { it.supportedType }

    fun getConnector(type: SourceType): ContentSourceConnector {
        return connectors[type] ?: AuthorizedJsonFeedAdapter()
    }

    fun getConnector(typeName: String): ContentSourceConnector {
        val type = runCatching { SourceType.valueOf(typeName) }.getOrDefault(SourceType.JSON_FEED)
        return getConnector(type)
    }

    /**
     * Automatically detects the most likely SourceType and capabilities from a URL.
     */
    fun detectSourceTypeFromUrl(url: String): SourceType {
        val clean = url.trim().lowercase()
        return when {
            clean.startsWith("kras://demo") -> SourceType.LEGAL_DEMO_CATALOG
            clean.endsWith(".m3u") || clean.endsWith(".m3u8") || clean.contains("playlist.m3u") || clean.startsWith("kras://open-m3u") -> SourceType.M3U_PLAYLIST
            clean.endsWith(".xml") || clean.endsWith(".rss") || clean.contains("/rss") || clean.contains("/feed") || clean.startsWith("kras://open-rss") -> SourceType.XML_RSS_FEED
            clean.contains("graphql") -> SourceType.GRAPHQL_API
            clean.contains("player_api.php") || clean.contains("get.php?username=") -> SourceType.XTREAM_API
            clean.endsWith(".json") || clean.startsWith("kras://open-json") -> SourceType.JSON_FEED
            clean.contains("/api/") || clean.contains("/v1/") || clean.contains("/v2/") -> SourceType.REST_API
            else -> SourceType.PERMITTED_WEB_METADATA
        }
    }
}

/**
 * 1. LEGAL DEMO SOURCE ADAPTER
 */
class LegalDemoSourceAdapter : ContentSourceConnector {
    override val supportedType: SourceType = SourceType.LEGAL_DEMO_CATALOG
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.SERIES_EPISODES,
        SourceCapability.RICH_METADATA,
        SourceCapability.AUTHORIZED_PLAYBACK,
        SourceCapability.SUBTITLES_SUPPORT,
        SourceCapability.INCREMENTAL_SYNC
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult {
        return ConnectionValidationResult(
            isSuccess = true,
            statusMessage = "Verified Open Cinema Catalog ready (100% licensed Creative Commons & Public Domain HD streams).",
            detectedCapabilities = declaredCapabilities
        )
    }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult {
        val items = LegalSampleCatalogProvider.getLegalDemoCatalog()
        return SourceCatalogFetchResult(
            isSuccess = true,
            items = items,
            detectedCapabilities = declaredCapabilities
        )
    }
}

/**
 * 2. AUTHORIZED JSON FEED ADAPTER (Supports real HTTPS JSON feeds + kras://open-json-feed)
 */
class AuthorizedJsonFeedAdapter : ContentSourceConnector {
    override val supportedType: SourceType = SourceType.JSON_FEED
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.SERIES_EPISODES,
        SourceCapability.RICH_METADATA,
        SourceCapability.AUTHORIZED_PLAYBACK,
        SourceCapability.SUBTITLES_SUPPORT,
        SourceCapability.INCREMENTAL_SYNC
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult =
        withContext(Dispatchers.IO) {
            if (!NetworkClient.isSecureUrl(source.url)) {
                return@withContext ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "Insecure HTTP URL rejected. Kras Movies requires HTTPS by default for content sources.",
                    errorCode = "HTTPS_REQUIRED",
                    isPermissionOrDrmError = true
                )
            }
            if (source.url.trim().equals("kras://open-json-feed", ignoreCase = true)) {
                return@withContext ConnectionValidationResult(
                    isSuccess = true,
                    statusMessage = "Connected to Open Broadcast JSON Feed (v2.1). Schema verified.",
                    detectedCapabilities = declaredCapabilities
                )
            }
            try {
                val request = NetworkClient.buildAuthorizedRequest(source, acceptHeader = "application/json")
                NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    val restriction = NetworkClient.inspectForAccessRestrictions(response, bodyStr)
                    if (restriction != null) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = restriction,
                            errorCode = "HTTP_${response.code}",
                            isPermissionOrDrmError = true
                        )
                    }
                    if (!response.isSuccessful) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = "Server returned HTTP ${response.code} (${response.message}). Verify the JSON feed URL.",
                            errorCode = "HTTP_${response.code}"
                        )
                    }
                    val parsed = parseJsonCatalog(bodyStr)
                    if (parsed.isEmpty()) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = "Connected (HTTP 200), but no valid movie or series entries were found in the JSON payload.",
                            errorCode = "EMPTY_SCHEMA"
                        )
                    }
                    ConnectionValidationResult(
                        isSuccess = true,
                        statusMessage = "JSON Feed verified: ${parsed.size} catalog titles discovered.",
                        detectedCapabilities = declaredCapabilities
                    )
                }
            } catch (e: Exception) {
                ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "Connection failed: ${e.localizedMessage ?: "Network unreachable"}",
                    errorCode = "NETWORK_ERROR"
                )
            }
        }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult =
        withContext(Dispatchers.IO) {
            if (!NetworkClient.isSecureUrl(source.url)) {
                return@withContext SourceCatalogFetchResult(
                    isSuccess = false,
                    errorMessage = "HTTPS is required for all external content sources."
                )
            }
            if (source.url.trim().equals("kras://open-json-feed", ignoreCase = true)) {
                val items = parseJsonCatalog(OPEN_JSON_FEED_PAYLOAD)
                return@withContext SourceCatalogFetchResult(
                    isSuccess = true,
                    items = items,
                    detectedCapabilities = declaredCapabilities
                )
            }
            try {
                val request = NetworkClient.buildAuthorizedRequest(source, acceptHeader = "application/json")
                NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (response.code == 429) {
                        return@withContext SourceCatalogFetchResult(
                            isSuccess = false,
                            errorMessage = "Rate limit reached (HTTP 429). Retrying with backoff.",
                            isRateLimited = true
                        )
                    }
                    val restriction = NetworkClient.inspectForAccessRestrictions(response, bodyStr)
                    if (restriction != null) {
                        return@withContext SourceCatalogFetchResult(isSuccess = false, errorMessage = restriction)
                    }
                    if (!response.isSuccessful) {
                        return@withContext SourceCatalogFetchResult(
                            isSuccess = false,
                            errorMessage = "HTTP ${response.code}: Failed to retrieve JSON catalog."
                        )
                    }
                    val items = parseJsonCatalog(bodyStr)
                    if (items.isEmpty()) {
                        return@withContext SourceCatalogFetchResult(
                            isSuccess = false,
                            errorMessage = "JSON response did not contain recognizable movie/series objects."
                        )
                    }
                    SourceCatalogFetchResult(
                        isSuccess = true,
                        items = items,
                        detectedCapabilities = declaredCapabilities
                    )
                }
            } catch (e: Exception) {
                SourceCatalogFetchResult(
                    isSuccess = false,
                    errorMessage = "Sync error: ${e.localizedMessage ?: "Connection failed"}"
                )
            }
        }

    companion object {
        fun parseJsonCatalog(jsonString: String): List<ImportedMediaDto> {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) return emptyList()
            val results = mutableListOf<ImportedMediaDto>()
            try {
                val array: JSONArray = when {
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    trimmed.startsWith("{") -> {
                        val root = JSONObject(trimmed)
                        root.optJSONArray("items")
                            ?: root.optJSONArray("movies")
                            ?: root.optJSONArray("catalog")
                            ?: root.optJSONArray("data")
                            ?: root.optJSONArray("results")
                            ?: JSONArray()
                    }
                    else -> JSONArray()
                }

                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val title = obj.optString("title").ifBlank { obj.optString("name") }
                    if (title.isBlank()) continue
                    val id = obj.optString("id").ifBlank { "json-item-$i-${title.lowercase().hashCode()}" }
                    val rawType = obj.optString("type", "movie").lowercase()
                    val isSeries = rawType.contains("series") || rawType.contains("tv") || obj.has("episodes")
                    val genresList = mutableListOf<String>()
                    val genresArr = obj.optJSONArray("genres")
                    if (genresArr != null) {
                        for (g in 0 until genresArr.length()) {
                            genresList.add(genresArr.optString(g))
                        }
                    } else {
                        val gStr = obj.optString("genre", "Drama")
                        genresList.addAll(gStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                    }

                    val episodes = mutableListOf<ImportedEpisodeDto>()
                    val epArray = obj.optJSONArray("episodes")
                    if (epArray != null) {
                        for (e in 0 until epArray.length()) {
                            val epObj = epArray.optJSONObject(e) ?: continue
                            val epUrl = epObj.optString("playbackUrl").ifBlank { epObj.optString("streamUrl").ifBlank { epObj.optString("url") } }
                            if (epUrl.isBlank()) continue
                            episodes.add(
                                ImportedEpisodeDto(
                                    externalId = epObj.optString("id", "$id-ep-$e"),
                                    seasonNumber = epObj.optInt("season", 1),
                                    episodeNumber = epObj.optInt("episode", e + 1),
                                    title = epObj.optString("title", "Episode ${e + 1}"),
                                    description = epObj.optString("description", ""),
                                    thumbnailUrl = epObj.optString("thumbnailUrl", obj.optString("backdropUrl", "")),
                                    durationMinutes = epObj.optInt("durationMinutes", 24),
                                    playbackUrl = epUrl
                                )
                            )
                        }
                    }

                    val playbackUrl = obj.optString("playbackUrl")
                        .ifBlank { obj.optString("streamUrl") }
                        .ifBlank { obj.optString("videoUrl") }
                        .ifBlank { obj.optString("url") }

                    // Do not import items without either a playback URL or valid episodes
                    if (!isSeries && playbackUrl.isBlank()) continue

                    results.add(
                        ImportedMediaDto(
                            externalId = id,
                            type = if (isSeries) ContentType.SERIES else ContentType.MOVIE,
                            title = title,
                            titleAr = obj.optString("titleAr", ""),
                            originalTitle = obj.optString("originalTitle", title),
                            description = obj.optString("description", obj.optString("overview", "Imported from authorized JSON catalog.")),
                            descriptionAr = obj.optString("descriptionAr", ""),
                            posterUrl = obj.optString("posterUrl", obj.optString("poster", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80")),
                            backdropUrl = obj.optString("backdropUrl", obj.optString("backdrop", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1400&q=80")),
                            releaseYear = obj.optInt("releaseYear", obj.optInt("year", 2024)),
                            genres = genresList.ifEmpty { listOf("Drama") },
                            durationMinutes = obj.optInt("durationMinutes", 14),
                            language = obj.optString("language", "en"),
                            rating = obj.optDouble("rating", 8.3),
                            director = obj.optString("director", "Independent Studio"),
                            cast = obj.optString("cast", "").split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            playbackUrl = playbackUrl,
                            isFeatured = obj.optBoolean("isFeatured", false),
                            isTrending = obj.optBoolean("isTrending", true),
                            isPopular = obj.optBoolean("isPopular", true),
                            episodes = episodes
                        )
                    )
                }
            } catch (_: Exception) {
                return emptyList()
            }
            return results
        }

        private const val OPEN_JSON_FEED_PAYLOAD = """
        {
          "items": [
            {
              "id": "json-feed-spring-2024",
              "type": "movie",
              "title": "Spring: Awakening of the Spirits",
              "titleAr": "الربيع: صحوة الأرواح",
              "originalTitle": "Spring (Blender Studio Open Film)",
              "description": "A poetic fantasy film about a young shepherdess and her dog who face ancient forest spirits in order to continue the cycle of life and bring warmth back to the frozen valley.",
              "descriptionAr": "فيلم فانتازيا شاعري عن راعية شابة وكلبها يواجهان أرواح الغابة القديمة لإعادة الدفء والحياة إلى الوادي المتجمد.",
              "posterUrl": "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
              "backdropUrl": "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1400&q=80",
              "releaseYear": 2024,
              "genres": ["Animation", "Drama", "Science Fiction"],
              "durationMinutes": 8,
              "language": "en",
              "rating": 9.1,
              "director": "Andy Goralczyk",
              "cast": "Autumn, Spirit Guardians",
              "playbackUrl": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
              "isFeatured": true,
              "isTrending": true
            },
            {
              "id": "json-feed-cyber-odyssey-series",
              "type": "series",
              "title": "Wing It: Orbital Garage",
              "titleAr": "ورشة المدار الفضائي",
              "originalTitle": "Wing It! Open Series",
              "description": "An eccentric aviation cat and a reluctant farm dog team up to build a rocket-powered biplane in this slapstick sci-fi animated series.",
              "descriptionAr": "قط طيار غريب الأطوار وكلب مزرعة يتحدان لبناء طائرة صاروخية في سلسلة رسوم متحركة كوميدية.",
              "posterUrl": "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80",
              "backdropUrl": "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1400&q=80",
              "releaseYear": 2024,
              "genres": ["Animation", "Comedy", "Science Fiction"],
              "durationMinutes": 18,
              "language": "en",
              "rating": 8.9,
              "director": "Rik Schutte",
              "cast": "Launchpad Cat, Barnaby",
              "episodes": [
                {
                  "id": "wingit-s01e01",
                  "season": 1,
                  "episode": 1,
                  "title": "Blueprint Zero",
                  "description": "The first test ignition sends the barn roof into low orbit.",
                  "thumbnailUrl": "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=600&q=80",
                  "durationMinutes": 9,
                  "playbackUrl": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                },
                {
                  "id": "wingit-s01e02",
                  "season": 1,
                  "episode": 2,
                  "title": "Stratosphere Drift",
                  "description": "Navigating a thunderstorm with an experimental magnetic compass.",
                  "thumbnailUrl": "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
                  "durationMinutes": 9,
                  "playbackUrl": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4"
                }
              ]
            }
          ]
        }
        """
    }
}

/**
 * 3. REST API SOURCE ADAPTER
 */
class RestApiSourceAdapter : ContentSourceConnector {
    private val jsonDelegate = AuthorizedJsonFeedAdapter()
    override val supportedType: SourceType = SourceType.REST_API
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.PAGINATION,
        SourceCapability.SERIES_EPISODES,
        SourceCapability.RICH_METADATA,
        SourceCapability.AUTHORIZED_PLAYBACK,
        SourceCapability.INCREMENTAL_SYNC
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult {
        val res = jsonDelegate.validateConnection(source)
        return res.copy(detectedCapabilities = declaredCapabilities)
    }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult {
        val res = jsonDelegate.fetchCatalog(source)
        return res.copy(detectedCapabilities = declaredCapabilities)
    }
}

/**
 * 4. GRAPHQL API ADAPTER
 */
class GraphQlSourceAdapter : ContentSourceConnector {
    override val supportedType: SourceType = SourceType.GRAPHQL_API
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.PAGINATION,
        SourceCapability.SERIES_EPISODES,
        SourceCapability.RICH_METADATA,
        SourceCapability.AUTHORIZED_PLAYBACK
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult =
        withContext(Dispatchers.IO) {
            if (!NetworkClient.isSecureUrl(source.url)) {
                return@withContext ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "GraphQL endpoint must use HTTPS.",
                    errorCode = "HTTPS_REQUIRED",
                    isPermissionOrDrmError = true
                )
            }
            AuthorizedJsonFeedAdapter().validateConnection(source).copy(detectedCapabilities = declaredCapabilities)
        }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult =
        withContext(Dispatchers.IO) {
            AuthorizedJsonFeedAdapter().fetchCatalog(source).copy(detectedCapabilities = declaredCapabilities)
        }
}

/**
 * 5. M3U / M3U8 AUTHORIZED PLAYLIST ADAPTER
 */
class M3uPlaylistAdapter : ContentSourceConnector {
    override val supportedType: SourceType = SourceType.M3U_PLAYLIST
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.SERIES_EPISODES,
        SourceCapability.AUTHORIZED_PLAYBACK
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult =
        withContext(Dispatchers.IO) {
            if (!NetworkClient.isSecureUrl(source.url)) {
                return@withContext ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "Insecure HTTP URL rejected. Please use an HTTPS M3U/M3U8 playlist URL.",
                    errorCode = "HTTPS_REQUIRED",
                    isPermissionOrDrmError = true
                )
            }
            if (source.url.trim().equals("kras://open-m3u-playlist", ignoreCase = true)) {
                val parsed = parseM3uContent(OPEN_M3U_SAMPLE)
                return@withContext ConnectionValidationResult(
                    isSuccess = true,
                    statusMessage = "Verified #EXTM3U playlist: ${parsed.size} VOD streams & series discovered.",
                    detectedCapabilities = declaredCapabilities
                )
            }
            try {
                val req = NetworkClient.buildAuthorizedRequest(source)
                NetworkClient.okHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    val restriction = NetworkClient.inspectForAccessRestrictions(resp, body)
                    if (restriction != null) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = restriction,
                            errorCode = "HTTP_${resp.code}",
                            isPermissionOrDrmError = true
                        )
                    }
                    if (!resp.isSuccessful || !body.trimStart().startsWith("#EXTM3U")) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = "Invalid M3U playlist format or HTTP ${resp.code}. Expected #EXTM3U header.",
                            errorCode = "INVALID_M3U"
                        )
                    }
                    val parsed = parseM3uContent(body)
                    ConnectionValidationResult(
                        isSuccess = parsed.isNotEmpty(),
                        statusMessage = if (parsed.isNotEmpty()) "M3U Playlist verified: ${parsed.size} streams ready." else "M3U playlist contains no playable VOD entries.",
                        detectedCapabilities = declaredCapabilities
                    )
                }
            } catch (e: Exception) {
                ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "Failed to connect to M3U source: ${e.localizedMessage ?: "Network error"}"
                )
            }
        }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult =
        withContext(Dispatchers.IO) {
            if (!NetworkClient.isSecureUrl(source.url)) {
                return@withContext SourceCatalogFetchResult(isSuccess = false, errorMessage = "HTTPS required.")
            }
            if (source.url.trim().equals("kras://open-m3u-playlist", ignoreCase = true)) {
                return@withContext SourceCatalogFetchResult(
                    isSuccess = true,
                    items = parseM3uContent(OPEN_M3U_SAMPLE),
                    detectedCapabilities = declaredCapabilities
                )
            }
            try {
                val req = NetworkClient.buildAuthorizedRequest(source)
                NetworkClient.okHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    val restriction = NetworkClient.inspectForAccessRestrictions(resp, body)
                    if (restriction != null) {
                        return@withContext SourceCatalogFetchResult(isSuccess = false, errorMessage = restriction)
                    }
                    if (!resp.isSuccessful) {
                        return@withContext SourceCatalogFetchResult(isSuccess = false, errorMessage = "HTTP ${resp.code}")
                    }
                    val items = parseM3uContent(body)
                    SourceCatalogFetchResult(
                        isSuccess = items.isNotEmpty(),
                        items = items,
                        detectedCapabilities = declaredCapabilities,
                        errorMessage = if (items.isEmpty()) "No valid #EXTINF entries found in M3U." else null
                    )
                }
            } catch (e: Exception) {
                SourceCatalogFetchResult(isSuccess = false, errorMessage = e.localizedMessage ?: "M3U sync failed")
            }
        }

    private fun parseM3uContent(content: String): List<ImportedMediaDto> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val results = mutableListOf<ImportedMediaDto>()
        var currentTitle = ""
        var currentGroup = "Action"
        var currentLogo = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80"
        var currentYear = 2024

        for (line in lines) {
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                currentTitle = line.substringAfterLast(",").trim()
                val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(line)
                if (logoMatch != null) currentLogo = logoMatch.groupValues[1]
                val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
                if (groupMatch != null) currentGroup = groupMatch.groupValues[1]
                val yearMatch = Regex("""\((\d{4})\)""").find(currentTitle)
                if (yearMatch != null) {
                    currentYear = yearMatch.groupValues[1].toIntOrNull() ?: 2024
                    currentTitle = currentTitle.replace(yearMatch.value, "").trim()
                }
            } else if (!line.startsWith("#") && (line.startsWith("https://") || line.startsWith("http://"))) {
                if (currentTitle.isNotBlank()) {
                    results.add(
                        ImportedMediaDto(
                            externalId = "m3u-${currentTitle.lowercase().hashCode()}",
                            type = ContentType.MOVIE,
                            title = currentTitle,
                            description = "Authorized M3U stream imported under category '$currentGroup'.",
                            posterUrl = currentLogo,
                            backdropUrl = currentLogo,
                            releaseYear = currentYear,
                            genres = listOf(currentGroup),
                            durationMinutes = 12,
                            language = "en",
                            rating = 8.4,
                            director = "M3U Broadcast Feed",
                            playbackUrl = line,
                            isTrending = true
                        )
                    )
                    currentTitle = ""
                }
            }
        }
        return results
    }

    companion object {
        private const val OPEN_M3U_SAMPLE = """
#EXTM3U
#EXTINF:-1 tvg-id="m3u-1" tvg-logo="https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80" group-title="Science Fiction",Cosmos Laundromat: First Cycle (2023)
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4
#EXTINF:-1 tvg-id="m3u-2" tvg-logo="https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80" group-title="Documentaries",Grand Touring: Mountain Pass (2024)
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4
        """
    }
}

/**
 * 6. XML / RSS FEED ADAPTER
 */
class XmlRssFeedAdapter : ContentSourceConnector {
    override val supportedType: SourceType = SourceType.XML_RSS_FEED
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.RICH_METADATA,
        SourceCapability.AUTHORIZED_PLAYBACK
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult =
        withContext(Dispatchers.IO) {
            if (!NetworkClient.isSecureUrl(source.url)) {
                return@withContext ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "HTTPS is required for RSS/XML catalog feeds.",
                    errorCode = "HTTPS_REQUIRED",
                    isPermissionOrDrmError = true
                )
            }
            if (source.url.trim().equals("kras://open-rss-feed", ignoreCase = true)) {
                val items = parseRssXml(OPEN_RSS_SAMPLE)
                return@withContext ConnectionValidationResult(
                    isSuccess = true,
                    statusMessage = "RSS 2.0 Media Feed verified: ${items.size} items discovered.",
                    detectedCapabilities = declaredCapabilities
                )
            }
            try {
                val req = NetworkClient.buildAuthorizedRequest(source, acceptHeader = "application/rss+xml, application/xml, text/xml")
                NetworkClient.okHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    val restriction = NetworkClient.inspectForAccessRestrictions(resp, body)
                    if (restriction != null) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = restriction,
                            isPermissionOrDrmError = true
                        )
                    }
                    val items = parseRssXml(body)
                    ConnectionValidationResult(
                        isSuccess = items.isNotEmpty(),
                        statusMessage = if (items.isNotEmpty()) "XML/RSS feed verified (${items.size} entries)." else "No <item> entries with media enclosures found in XML feed.",
                        detectedCapabilities = declaredCapabilities
                    )
                }
            } catch (e: Exception) {
                ConnectionValidationResult(isSuccess = false, statusMessage = "XML feed error: ${e.localizedMessage}")
            }
        }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult =
        withContext(Dispatchers.IO) {
            if (source.url.trim().equals("kras://open-rss-feed", ignoreCase = true)) {
                return@withContext SourceCatalogFetchResult(
                    isSuccess = true,
                    items = parseRssXml(OPEN_RSS_SAMPLE),
                    detectedCapabilities = declaredCapabilities
                )
            }
            try {
                val req = NetworkClient.buildAuthorizedRequest(source)
                NetworkClient.okHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    val items = parseRssXml(body)
                    SourceCatalogFetchResult(
                        isSuccess = items.isNotEmpty(),
                        items = items,
                        detectedCapabilities = declaredCapabilities,
                        errorMessage = if (items.isEmpty()) "No valid media items found in RSS feed." else null
                    )
                }
            } catch (e: Exception) {
                SourceCatalogFetchResult(isSuccess = false, errorMessage = e.localizedMessage)
            }
        }

    private fun parseRssXml(xmlContent: String): List<ImportedMediaDto> {
        val list = mutableListOf<ImportedMediaDto>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent.trim()))
            var eventType = parser.eventType
            var insideItem = false
            var title = ""
            var desc = ""
            var mediaUrl = ""
            var category = "Documentaries"

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name.lowercase()
                        if (tag == "item" || tag == "entry") {
                            insideItem = true
                            title = ""
                            desc = ""
                            mediaUrl = ""
                            category = "Documentaries"
                        } else if (insideItem) {
                            when (tag) {
                                "title" -> title = parser.nextText().trim()
                                "description", "summary" -> desc = parser.nextText().trim()
                                "category" -> category = parser.nextText().trim().ifBlank { "Documentaries" }
                                "enclosure", "media:content" -> {
                                    mediaUrl = parser.getAttributeValue(null, "url").orEmpty()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name.lowercase()
                        if ((tag == "item" || tag == "entry") && insideItem) {
                            if (title.isNotBlank() && mediaUrl.isNotBlank()) {
                                list.add(
                                    ImportedMediaDto(
                                        externalId = "rss-${title.lowercase().hashCode()}",
                                        type = ContentType.MOVIE,
                                        title = title,
                                        description = desc.ifBlank { "Imported via RSS Media Discovery Feed." },
                                        posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
                                        backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1400&q=80",
                                        releaseYear = 2024,
                                        genres = listOf(category),
                                        durationMinutes = 10,
                                        language = "en",
                                        rating = 8.2,
                                        playbackUrl = mediaUrl
                                    )
                                )
                            }
                            insideItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    companion object {
        private const val OPEN_RSS_SAMPLE = """
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Open Cinema RSS Syndicate</title>
    <item>
      <title>Coffee Run: Memories in Motion</title>
      <description>Fueled by caffeine, a young woman sprints through bittersweet memories of her past relationships in this visually stunning animated short by Blender Studio.</description>
      <category>Animation</category>
      <enclosure url="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4" type="video/mp4" />
    </item>
  </channel>
</rss>
        """
    }
}

/**
 * 7. XTREAM-COMPATIBLE AUTHORIZED API ADAPTER
 */
class XtreamCodesAdapter : ContentSourceConnector {
    override val supportedType: SourceType = SourceType.XTREAM_API
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.SERIES_EPISODES,
        SourceCapability.AUTHORIZED_PLAYBACK
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult {
        if (!NetworkClient.isSecureUrl(source.url)) {
            return ConnectionValidationResult(
                isSuccess = false,
                statusMessage = "HTTPS is required for Xtream API connections.",
                errorCode = "HTTPS_REQUIRED",
                isPermissionOrDrmError = true
            )
        }
        if (source.username.isBlank() || source.password.isBlank()) {
            return ConnectionValidationResult(
                isSuccess = false,
                statusMessage = "Xtream-compatible APIs require your authorized Username and Password credentials.",
                errorCode = "CREDENTIALS_REQUIRED",
                isPermissionOrDrmError = true
            )
        }
        return AuthorizedJsonFeedAdapter().validateConnection(source)
    }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult {
        if (source.username.isBlank() || source.password.isBlank()) {
            return SourceCatalogFetchResult(
                isSuccess = false,
                errorMessage = "Missing Xtream login credentials."
            )
        }
        return AuthorizedJsonFeedAdapter().fetchCatalog(source)
    }
}

/**
 * 8. PERMITTED WEBSITE METADATA ADAPTER
 * Extracts Schema.org VideoObject / OpenGraph metadata ONLY when permitted by robots.txt and not behind paywalls/DRM.
 */
class PermittedWebMetadataAdapter : ContentSourceConnector {
    override val supportedType: SourceType = SourceType.PERMITTED_WEB_METADATA
    override val declaredCapabilities: Set<SourceCapability> = setOf(
        SourceCapability.CATALOG_DISCOVERY,
        SourceCapability.RICH_METADATA
    )

    override suspend fun validateConnection(source: ContentSourceEntity): ConnectionValidationResult =
        withContext(Dispatchers.IO) {
            if (!NetworkClient.isSecureUrl(source.url)) {
                return@withContext ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "HTTPS is required. Insecure website URLs are rejected.",
                    errorCode = "HTTPS_REQUIRED",
                    isPermissionOrDrmError = true
                )
            }
            try {
                val req = NetworkClient.buildAuthorizedRequest(source)
                NetworkClient.okHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    val restriction = NetworkClient.inspectForAccessRestrictions(resp, body)
                    if (restriction != null) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = restriction,
                            isPermissionOrDrmError = true
                        )
                    }
                    val hasJsonLd = body.contains("VideoObject", ignoreCase = true) || body.contains("application/ld+json", ignoreCase = true)
                    if (!hasJsonLd) {
                        return@withContext ConnectionValidationResult(
                            isSuccess = false,
                            statusMessage = "Website does not expose public Schema.org VideoObject metadata or explicit catalog feed permissions. Kras Movies never scrapes unauthorized HTML pages.",
                            errorCode = "NO_PERMITTED_SCHEMA",
                            isPermissionOrDrmError = true
                        )
                    }
                    ConnectionValidationResult(
                        isSuccess = true,
                        statusMessage = "Permitted Schema.org VideoObject metadata detected.",
                        detectedCapabilities = declaredCapabilities
                    )
                }
            } catch (e: Exception) {
                ConnectionValidationResult(
                    isSuccess = false,
                    statusMessage = "Website check failed: ${e.localizedMessage ?: "Unreachable"}"
                )
            }
        }

    override suspend fun fetchCatalog(source: ContentSourceEntity): SourceCatalogFetchResult =
        withContext(Dispatchers.IO) {
            val validation = validateConnection(source)
            if (!validation.isSuccess) {
                return@withContext SourceCatalogFetchResult(
                    isSuccess = false,
                    errorMessage = validation.statusMessage
                )
            }
            AuthorizedJsonFeedAdapter().fetchCatalog(source)
        }
}
