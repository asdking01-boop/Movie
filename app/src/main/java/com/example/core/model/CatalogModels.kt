package com.example.core.model

enum class SourceType(val displayName: String, val displayNameAr: String, val description: String) {
    REST_API(
        "REST API",
        "واجهة REST API",
        "Authorized JSON REST catalog endpoints with optional Bearer or API Key auth"
    ),
    GRAPHQL_API(
        "GraphQL API",
        "واجهة GraphQL",
        "Structured GraphQL catalog & episode query endpoints"
    ),
    JSON_FEED(
        "Authorized JSON Feed",
        "خلاصة JSON مصرحة",
        "Direct JSON movie & series catalog feed"
    ),
    XML_RSS_FEED(
        "XML / RSS Feed",
        "خلاصة XML / RSS",
        "RSS 2.0 / MRSS media catalog discovery feed"
    ),
    M3U_PLAYLIST(
        "Authorized M3U/M3U8",
        "قائمة تشغيل M3U/M3U8",
        "Standard #EXTM3U VOD & series playlist with group-title and tvg metadata"
    ),
    XTREAM_API(
        "Xtream-Compatible API",
        "واجهة Xtream المصرحة",
        "Authorized Xtream VOD & Series API with user credentials"
    ),
    PERMITTED_WEB_METADATA(
        "Permitted Website Metadata",
        "بيانات موقع مسموح بها",
        "Extracts Schema.org VideoObject / OpenGraph metadata when permitted by robots.txt"
    ),
    LEGAL_DEMO_CATALOG(
        "Open Cinema Catalog",
        "مكتبة السينما المفتوحة",
        "Built-in Creative Commons & Public Domain HD streaming catalog for verification"
    )
}

enum class SourceCapability(val label: String, val labelAr: String) {
    CATALOG_DISCOVERY("Catalog Discovery", "اكتشاف الكتالوج"),
    PAGINATION("Pagination", "تعدد الصفحات"),
    SERIES_EPISODES("Seasons & Episodes", "المواسم والحلقات"),
    RICH_METADATA("Rich Metadata", "بيانات تفصيلية"),
    AUTHORIZED_PLAYBACK("Authorized Playback", "تشغيل مصرح"),
    SUBTITLES_SUPPORT("Subtitles", "الترجمة"),
    INCREMENTAL_SYNC("Incremental Sync", "مزامنة تزايدية")
}

enum class SourceConnectionStatus(val label: String, val labelAr: String) {
    CONNECTED("Connected", "متصل"),
    SYNCING("Syncing", "جاري المزامنة"),
    ERROR("Connection Error", "خطأ في الاتصال"),
    DISABLED("Disabled", "معطل"),
    UNTESTED("Untested", "غير مختبر")
}

enum class ContentType {
    MOVIE,
    SERIES
}

data class SubtitleTrack(
    val languageCode: String,
    val label: String,
    val url: String,
    val mimeType: String = "text/vtt"
)

data class ImportedEpisodeDto(
    val externalId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val durationMinutes: Int,
    val playbackUrl: String,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val releaseDate: String = ""
)

data class ImportedMediaDto(
    val externalId: String,
    val type: ContentType,
    val title: String,
    val titleAr: String = "",
    val originalTitle: String = "",
    val description: String,
    val descriptionAr: String = "",
    val posterUrl: String,
    val backdropUrl: String,
    val localPosterRes: String = "",
    val localBackdropRes: String = "",
    val releaseYear: Int,
    val genres: List<String>,
    val durationMinutes: Int,
    val language: String,
    val rating: Double,
    val director: String = "",
    val cast: List<String> = emptyList(),
    val playbackUrl: String = "",
    val subtitles: List<SubtitleTrack> = emptyList(),
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isPopular: Boolean = false,
    val episodes: List<ImportedEpisodeDto> = emptyList()
)

data class ConnectionValidationResult(
    val isSuccess: Boolean,
    val statusMessage: String,
    val detectedCapabilities: Set<SourceCapability> = emptySet(),
    val errorCode: String? = null,
    val isPermissionOrDrmError: Boolean = false
)

data class SourceCatalogFetchResult(
    val isSuccess: Boolean,
    val items: List<ImportedMediaDto> = emptyList(),
    val detectedCapabilities: Set<SourceCapability> = emptySet(),
    val errorMessage: String? = null,
    val isRateLimited: Boolean = false
)

enum class SortOption(val labelEn: String, val labelAr: String) {
    DATE_ADDED("Recently Added", "الأحدث إضافة"),
    RATING("Highest Rated", "الأعلى تقييماً"),
    TITLE("Title (A–Z)", "العنوان (أ-ي)"),
    RELEASE_YEAR("Release Year", "سنة الإصدار")
}
