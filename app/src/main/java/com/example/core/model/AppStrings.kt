package com.example.core.model

data class AppStrings(
    val isArabic: Boolean = false,
    val brandName: String = "KRAS MOVIES",
    val navHome: String = if (isArabic) "الرئيسية" else "Home",
    val navMovies: String = if (isArabic) "أفلام" else "Movies",
    val navSeries: String = if (isArabic) "مسلسلات" else "Series",
    val navSearch: String = if (isArabic) "بحث" else "Search",
    val navLibrary: String = if (isArabic) "قائمتي" else "My List",
    val navSources: String = if (isArabic) "المصادر" else "Sources",
    val play: String = if (isArabic) "تشغيل الآن" else "Play",
    val resume: String = if (isArabic) "متابعة المشاهدة" else "Resume",
    val moreInfo: String = if (isArabic) "مزيد من المعلومات" else "More Info",
    val addToMyList: String = if (isArabic) "أضف لقائمتي" else "My List",
    val inMyList: String = if (isArabic) "في قائمتي" else "In My List",
    val featuredContent: String = if (isArabic) "أبرز الأفلام والمسلسلات" else "Featured Movies & Series",
    val continueWatching: String = if (isArabic) "متابعة المشاهدة" else "Continue Watching",
    val recentlyAddedMovies: String = if (isArabic) "أفلام أضيفت مؤخراً" else "Recently Added Movies",
    val recentlyAddedSeries: String = if (isArabic) "مسلسلات أضيفت مؤخراً" else "Recently Added Series",
    val trendingNow: String = if (isArabic) "الأكثر رائجاً الآن" else "Trending Content",
    val popularMovies: String = if (isArabic) "أفلام شائعة" else "Popular Movies",
    val popularSeries: String = if (isArabic) "مسلسلات شائعة" else "Popular Series",
    val recommendedForYou: String = if (isArabic) "موصى به لك" else "Recommended For You",
    val genreAction: String = if (isArabic) "أكشن وإثارة" else "Action",
    val genreComedy: String = if (isArabic) "كوميديا" else "Comedy",
    val genreDrama: String = if (isArabic) "دراما" else "Drama",
    val genreHorror: String = if (isArabic) "رعب وغموض" else "Horror",
    val genreSciFi: String = if (isArabic) "خيال علمي" else "Science Fiction",
    val genreAnimation: String = if (isArabic) "رسوم متحركة" else "Animation",
    val genreDocumentary: String = if (isArabic) "وثائقي" else "Documentaries",
    val allGenres: String = if (isArabic) "كل التصنيفات" else "All Genres",
    val allSources: String = if (isArabic) "كل المصادر" else "All Sources",
    val allYears: String = if (isArabic) "كل السنوات" else "All Years",
    val latest20: String = if (isArabic) "أحدث ٢٠ إضافة" else "Latest 20 Imported",
    val recentlyReleased: String = if (isArabic) "أحدث الإصدارات" else "Recently Released",
    val recentlyUpdated: String = if (isArabic) "محدثة مؤخراً" else "Recently Updated",
    val sampleBadge: String = if (isArabic) "كتالوج تجريبي قانوني" else "LEGAL DEMO",
    val importedBadge: String = if (isArabic) "مصدر مستورد" else "IMPORTED",
    val contentSourcesTitle: String = if (isArabic) "إدارة مصادر المحتوى" else "Content Sources",
    val addSource: String = if (isArabic) "إضافة مصدر" else "Add Source",
    val syncAll: String = if (isArabic) "مزامنة الكل" else "Sync All",
    val testConnection: String = if (isArabic) "اختبار الاتصال" else "Test Connection",
    val syncNow: String = if (isArabic) "مزامنة الآن" else "Sync Now",
    val editSource: String = if (isArabic) "تعديل" else "Edit",
    val removeSource: String = if (isArabic) "حذف" else "Remove",
    val searchPlaceholder: String = if (isArabic) "ابحث عن فيلم، مسلسل، ممثل، مخرج أو سنة..." else "Search movies, series, actor, director, genre, year...",
    val favorites: String = if (isArabic) "المفضلة" else "Favorites",
    val watchHistory: String = if (isArabic) "سجل المشاهدة" else "Watch History",
    val completedEpisodes: String = if (isArabic) "حلقات مكتملة" else "Completed",
    val unwatchedEpisodes: String = if (isArabic) "حلقات غير مشاهدة" else "Unwatched",
    val seasons: String = if (isArabic) "المواسم" else "Seasons",
    val episodes: String = if (isArabic) "الحلقات" else "Episodes",
    val languageToggleLabel: String = if (isArabic) "English (LTR)" else "العربية (RTL)"
)

object SearchNormalizer {
    private val arabicDiacriticsRegex = Regex("[\\u064B-\\u065F\\u0670]")

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        return input.lowercase()
            .replace(arabicDiacriticsRegex, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .trim()
    }

    fun matches(query: String, vararg fields: String): Boolean {
        val normQuery = normalize(query)
        if (normQuery.isEmpty()) return true
        val tokens = normQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val combinedTarget = fields.joinToString(" ") { normalize(it) }
        return tokens.all { token -> combinedTarget.contains(token) }
    }
}
