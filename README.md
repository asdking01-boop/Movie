# KRAS MOVIES — Unified Android Movie & Series Streaming Platform

**KRAS MOVIES** is a native Android application built with Kotlin, Jetpack Compose, Material 3, AndroidX Media3 ExoPlayer, Room Database, and WorkManager. It transforms authorized movie and TV series catalogs from user-configured APIs, JSON/XML feeds, and M3U playlists into a unified, Netflix-inspired theater experience.

## Architecture & Module Package Organization

- `com.example.core.model` (`core-model`): Domain models, `SourceType`, `SourceCapability`, `ContentType`, bilingual `AppStrings` (English LTR & Arabic RTL), and `SearchNormalizer`.
- `com.example.core.database` (`core-database`): Room Database (`KrasDatabase`), `KrasDao`, entities (`ContentSourceEntity`, `MediaItemEntity`, `EpisodeEntity`, `SyncLogEntity`, `RecentSearchEntity`), and `KrasRepository` with DataStore Preferences.
- `com.example.core.network` (`core-network`): `NetworkClient` with OkHttp, HTTPS enforcement, Bearer/Basic auth injection, and anti-bot/DRM/CAPTCHA/paywall diagnostic inspection.
- `com.example.datasource.connectors` (`data-source-connectors`): `ContentSourceConnector` interface and 8 dedicated adapters (`RestApiSourceAdapter`, `GraphQlSourceAdapter`, `AuthorizedJsonFeedAdapter`, `XmlRssFeedAdapter`, `M3uPlaylistAdapter`, `XtreamCodesAdapter`, `PermittedWebMetadataAdapter`, `LegalDemoSourceAdapter`).
- `com.example.sync.engine` (`sync-engine`): `CatalogSyncEngine` (incremental sync, externalId + conservative title/year deduplication, exponential backoff retry, offline preservation) and `CatalogSyncWorker` (WorkManager background scheduling).
- `com.example.core.ui` (`core-ui`): Shared cinematic components (`CinematicHeroCarousel`, `CinematicSectionRow`, `CinematicPosterCard`, `ContinueWatchingPosterCard`, `SourceProvenanceBadge`, `ShimmerBox`).
- `com.example.feature.home` (`feature-home`): 16-row cinematic home screen with snap-scrolling carousels and full-bleed hero banner.
- `com.example.feature.movies` (`feature-movies`): Dedicated Movies catalog with Latest 20, Recently Released, Genre/Year/Source filters, and sorting.
- `com.example.feature.series` (`feature-series`): Dedicated Series catalog and `MediaDetailScreen` with Season selector and Episode progress tracking.
- `com.example.feature.player` (`feature-player`): AndroidX Media3 ExoPlayer with HLS/DASH/MP4 playback, subtitle tracks, speed control, brightness/volume vertical swipe gestures, double-tap 10s seek, Picture-in-Picture, and in-player Episode Switcher drawer.
- `com.example.feature.search` (`feature-search`): Unified instant search across titles, directors, cast, genres, and release years with Arabic and English normalization.
- `com.example.feature.sources` (`feature-sources`): Content Sources management dashboard, protocol auto-detector, connection tester, and first-launch onboarding flow.
- `com.example.feature.settings` (`feature-settings`): Personal Library (`My List`, `Favorites`, `Continue Watching`, `Watch History`, `Completed` & `Unwatched` episodes).

## Security & Content Permissions Policy

- **HTTPS Enforced by Default**: `android:usesCleartextTraffic="false"` and strict HTTPS validation in `NetworkClient`.
- **Authorized Sources Only**: Never bypasses DRM, paywalls, authentication, CAPTCHAs, or `robots.txt` rules.
- **Legal Open Cinema Demo Catalog**: Ships with a clearly badged (`LEGAL DEMO`) Creative Commons / Blender Foundation open movie & anthology series catalog for immediate verification.
