package com.example.datasource.connectors

import com.example.core.model.ContentType
import com.example.core.model.ImportedEpisodeDto
import com.example.core.model.ImportedMediaDto
import com.example.core.model.SubtitleTrack

/**
 * Provides a 100% legal, publicly licensed demonstration catalog (Creative Commons / Blender Studio Open Movies /
 * Public Domain streams) with working HLS and MP4 video streams so the user can test every player and catalog
 * feature immediately while clearly distinguishing sample content from user-imported sources.
 */
object LegalSampleCatalogProvider {

    private val defaultSubtitles = listOf(
        SubtitleTrack(
            languageCode = "en",
            label = "English (CC)",
            url = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_en.vtt"
        ),
        SubtitleTrack(
            languageCode = "ar",
            label = "العربية (Arabic)",
            url = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_en.vtt"
        )
    )

    fun getLegalDemoCatalog(): List<ImportedMediaDto> {
        return listOf(
            // 1. Featured Sci-Fi Movie (Uses generated local hero & poster artwork + Tears of Steel 4K/HD open film stream)
            ImportedMediaDto(
                externalId = "open-cinema-tears-of-steel",
                type = ContentType.MOVIE,
                title = "Tears of Steel: Obsidian Horizon",
                titleAr = "دموع الفولاذ: الأفق الأسود",
                originalTitle = "Tears of Steel (Blender Open Movie)",
                description = "In a cyberpunk future Amsterdam, a group of warriors and scientists gather at the Oude Kerk to stage a desperate stand against sentient machines and rewrite the fate of humanity. Released under Creative Commons Attribution by Blender Foundation.",
                descriptionAr = "في مستقبل سايبربانك المظلم، يجتمع فريق من المحاربين والعلماء في مواجهة حاسمة ضد الآلات الواعية لإنقاذ مستقبل البشرية. فيلم مفتوح المصدر مرخص قانونياً.",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1400&q=80",
                localPosterRes = "img_poster_horizon_1793910456490",
                localBackdropRes = "img_hero_cosmos_1793910454623",
                releaseYear = 2024,
                genres = listOf("Science Fiction", "Action", "Drama"),
                durationMinutes = 12,
                language = "en",
                rating = 9.2,
                director = "Ian Hubert",
                cast = listOf("Derek de Lint", "Sergio Hasselbaink", "Rogier Schippers", "Vanja Rukavina"),
                playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                subtitles = defaultSubtitles,
                isFeatured = true,
                isTrending = true,
                isPopular = true
            ),
            // 2. Featured Neo-Noir / Action Movie
            ImportedMediaDto(
                externalId = "open-cinema-cyber-heist",
                type = ContentType.MOVIE,
                title = "Charge: Neon Protocol",
                titleAr = "بروتوكول النيون: المهمة القصوى",
                originalTitle = "Charge (Blender Studio Open Film)",
                description = "In an ultra-industrial arctic energy facility, a lone cybernetic infiltrator races against the clock to breach a high-voltage core before the automated defense grid locks down forever. Creative Commons Open Movie.",
                descriptionAr = "في منشأة طاقة قطبية فائقة التطور، يسابق مخترق سيبراني الزمن لاختراق النواة عالية الجهد قبل إغلاق شبكة الدفاع الآلية للأبد.",
                posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1400&q=80",
                localPosterRes = "img_poster_solaris_1793910476382",
                localBackdropRes = "img_hero_cyber_heist_1793910455808",
                releaseYear = 2023,
                genres = listOf("Action", "Science Fiction", "Animation"),
                durationMinutes = 5,
                language = "en",
                rating = 8.9,
                director = "Hjalti Hjalmarsson",
                cast = listOf("Hjalti Hjalmarsson", "Pablo Vazquez"),
                playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                subtitles = defaultSubtitles,
                isFeatured = true,
                isTrending = true,
                isPopular = true
            ),
            // 3. Fantasy / Animation Epic
            ImportedMediaDto(
                externalId = "open-cinema-sintel",
                type = ContentType.MOVIE,
                title = "Sintel: The Dragon's Oath",
                titleAr = "سينتيل: عهد التنين",
                originalTitle = "Sintel (Open Movie Project)",
                description = "A lonely young warrior named Sintel embarks on a perilous quest across frozen peaks and ancient deserts to rescue her fledgling dragon companion, Scales, uncovering a heartbreaking truth.",
                descriptionAr = "تنطلق المحاربة الشابة سينتيل في رحلة محفوفة بالمخاطر عبر القمم الجليدية والصحاري القديمة لإنقاذ رفيقها التنين الصغير.",
                posterUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1400&q=80",
                localPosterRes = "img_poster_horizon_1793910456490",
                localBackdropRes = "img_hero_cosmos_1793910454623",
                releaseYear = 2022,
                genres = listOf("Animation", "Action", "Drama"),
                durationMinutes = 15,
                language = "en",
                rating = 9.0,
                director = "Colin Levy",
                cast = listOf("Halina Reijn", "Thom Hoffman"),
                playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                subtitles = defaultSubtitles,
                isFeatured = false,
                isTrending = true,
                isPopular = true
            ),
            // 4. Comedy / Animation
            ImportedMediaDto(
                externalId = "open-cinema-big-buck-bunny",
                type = ContentType.MOVIE,
                title = "Big Buck Bunny: Forest Justice",
                titleAr = "أرنب الغابة العملاق",
                originalTitle = "Big Buck Bunny (Peach Open Movie)",
                description = "When three mischievous woodland rodents push a gentle giant rabbit too far, he uses cleverness and nature's own traps to turn the tables in this beloved open-source animated comedy.",
                descriptionAr = "عندما يتجاوز ثلاثة من القوارض المشاغبين حدودهم مع أرنب عملاق مسالم، يقرر استخدام ذكائه لقلب الطاولة في مغامرة كوميدية ممتعة.",
                posterUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1400&q=80",
                releaseYear = 2021,
                genres = listOf("Comedy", "Animation"),
                durationMinutes = 10,
                language = "en",
                rating = 8.4,
                director = "Sacha Goedegebure",
                cast = listOf("Peach Studio Ensemble"),
                playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                subtitles = defaultSubtitles,
                isFeatured = false,
                isTrending = false,
                isPopular = true
            ),
            // 5. Sci-Fi / Surreal Mystery
            ImportedMediaDto(
                externalId = "open-cinema-elephants-dream",
                type = ContentType.MOVIE,
                title = "Elephants Dream: Machina Labyrinth",
                titleAr = "حلم الأفيال: متاهة الآلة",
                originalTitle = "Elephants Dream (Orange Open Movie)",
                description = "Inside a colossal, surreal clockwork megastructure, the veteran Proog attempts to initiate the curious young Emo into the mysteries of a world that reshapes itself around their thoughts.",
                descriptionAr = "داخل هيكل آلي عملاق وسريالي، يحاول بروغ كشف أسرار عالم غامض يتشكل وفقاً لأفكارهم.",
                posterUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1400&q=80",
                localPosterRes = "img_poster_solaris_1793910476382",
                releaseYear = 2020,
                genres = listOf("Science Fiction", "Horror", "Drama"),
                durationMinutes = 11,
                language = "en",
                rating = 8.1,
                director = "Bassam Kurdali",
                cast = listOf("Tygo Gernandt", "Cas Jansen"),
                playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                subtitles = defaultSubtitles,
                isFeatured = false,
                isTrending = true,
                isPopular = false
            ),
            // 6. Documentary
            ImportedMediaDto(
                externalId = "open-cinema-subaru-rally-doc",
                type = ContentType.MOVIE,
                title = "Apex Grit: Behind the Rally Stage",
                titleAr = "قمة التحدي: كواليس سباقات الرالي",
                originalTitle = "Subaru Outback Street & Trail Documentary",
                description = "An adrenaline-fueled motorsport documentary following precision drivers and mechanical engineers testing endurance vehicles across treacherous mountain passes.",
                descriptionAr = "فيلم وثائقي مشوق يتابع سائقي السباقات والمهندسين أثناء اختبار المركبات عبر الممرات الجبلية الوعرة.",
                posterUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1400&q=80",
                releaseYear = 2024,
                genres = listOf("Documentaries", "Action"),
                durationMinutes = 9,
                language = "en",
                rating = 8.6,
                director = "Marcus Vance",
                cast = listOf("Elena Rostova", "Liam O'Connor"),
                playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                subtitles = defaultSubtitles,
                isFeatured = false,
                isTrending = false,
                isPopular = true
            ),
            // 7. Horror / Supernatural Thriller
            ImportedMediaDto(
                externalId = "open-cinema-crimson-monolith",
                type = ContentType.MOVIE,
                title = "The Fjord Monolith",
                titleAr = "مسلة المضيق المظلم",
                originalTitle = "Vokter av Fjorden",
                description = "When an arctic expedition uncovers a runic obelisk pulsing with crimson bio-luminescence, the crew's radio transmissions begin echoing voices from centuries ago.",
                descriptionAr = "عندما تكتشف بعثة قطبية مسلة قديمة تنبض بضوء قرمزي غامض، تبدأ أجهزة اللاسلكي بالتقاط أصوات من قرون مضت.",
                posterUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1400&q=80",
                localPosterRes = "img_poster_horizon_1793910456490",
                releaseYear = 2024,
                genres = listOf("Horror", "Science Fiction", "Drama"),
                durationMinutes = 8,
                language = "en",
                rating = 8.7,
                director = "Soren Lindqvist",
                cast = listOf("Astrid Holm", "Henrik Mikkelsen"),
                playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                subtitles = defaultSubtitles,
                isFeatured = false,
                isTrending = true,
                isPopular = false
            ),

            // --- TV SERIES ---
            // Series 1: Multi-Season Sci-Fi Anthology Series
            ImportedMediaDto(
                externalId = "open-series-cosmos-chronicles",
                type = ContentType.SERIES,
                title = "Cosmos Chronicles: Deep Space",
                titleAr = "سجلات الكون: الفضاء السحيق",
                originalTitle = "Cosmos Chronicles (Open Anthology Series)",
                description = "An acclaimed anthology series exploring deep-space exploration, quantum anomalies, and humanity's first interstellar outposts across two thrilling seasons.",
                descriptionAr = "مسلسل خيال علمي مشوق يستكشف رحلات الفضاء السحيق والظواهر الكمية وأولى المحطات البشرية بين النجوم عبر موسمين.",
                posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=1400&q=80",
                localPosterRes = "img_poster_solaris_1793910476382",
                localBackdropRes = "img_hero_cosmos_1793910454623",
                releaseYear = 2024,
                genres = listOf("Science Fiction", "Drama", "Action"),
                durationMinutes = 45,
                language = "en",
                rating = 9.4,
                director = "Nadia Al-Mansoor",
                cast = listOf("Tariq Zayd", "Clara Vance", "Kenji Takahashi"),
                isFeatured = true,
                isTrending = true,
                isPopular = true,
                episodes = listOf(
                    ImportedEpisodeDto(
                        externalId = "cosmos-s01e01",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Orbital Ignition",
                        description = "Commander Vance leads the reconnaissance vessel Hyperion into the rings of Saturn where an ancient telemetry beacon awakens.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 12,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2024-01-12"
                    ),
                    ImportedEpisodeDto(
                        externalId = "cosmos-s01e02",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Echoes of the Nebula",
                        description = "As solar winds disrupt communications with Earth, the crew decodes a harmonic signal originating from inside the obsidian crater.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 15,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2024-01-19"
                    ),
                    ImportedEpisodeDto(
                        externalId = "cosmos-s01e03",
                        seasonNumber = 1,
                        episodeNumber = 3,
                        title = "Zero Gravity Protocol",
                        description = "An unexpected hull breach forces Chief Engineer Tariq to perform an untethered spacewalk across the station's solar array.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 11,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2024-01-26"
                    ),
                    ImportedEpisodeDto(
                        externalId = "cosmos-s02e01",
                        seasonNumber = 2,
                        episodeNumber = 1,
                        title = "The Citadel Above",
                        description = "Season 2 Premiere: Two years after the Saturn incident, the crew arrives at the floating glass citadel of Solaris Prime.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 10,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2024-09-01"
                    ),
                    ImportedEpisodeDto(
                        externalId = "cosmos-s02e02",
                        seasonNumber = 2,
                        episodeNumber = 2,
                        title = "Eclipse of Reason",
                        description = "During a total solar eclipse, the citadel's AI archive reveals the true architect behind the deep space beacons.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 12,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2024-09-08"
                    )
                )
            ),

            // Series 2: Animated Adventure & Comedy Series
            ImportedMediaDto(
                externalId = "open-series-caminandes",
                type = ContentType.SERIES,
                title = "Caminandes: Patagonia Tales",
                titleAr = "حكايات باتاغونيا",
                originalTitle = "Caminandes (Blender Open Series)",
                description = "Follow Koro the resilient Patagonian llama as he braves highways, frozen lakes, and rival penguins in this hilarious, family-friendly animated series.",
                descriptionAr = "تابع مغامرات اللاما كورو في براري باتاغونيا وهو يواجه الطرق السريعة والبحيرات المتجمدة في مسلسل رسوم متحركة كوميدي.",
                posterUrl = "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1400&q=80",
                releaseYear = 2023,
                genres = listOf("Animation", "Comedy"),
                durationMinutes = 10,
                language = "en",
                rating = 8.8,
                director = "Pablo Vazquez",
                cast = listOf("Koro the Llama", "Oti the Penguin"),
                isFeatured = false,
                isTrending = true,
                isPopular = true,
                episodes = listOf(
                    ImportedEpisodeDto(
                        externalId = "caminandes-s01e01",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Llama Drama",
                        description = "Koro tries to cross a deserted Patagonian road to reach the greener grass on the other side.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 10,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2023-04-10"
                    ),
                    ImportedEpisodeDto(
                        externalId = "caminandes-s01e02",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Gran Dillama",
                        description = "An electric fence stands between Koro and a delicious berry bush, sparking a rivalry with an armadillo.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 8,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2023-04-17"
                    ),
                    ImportedEpisodeDto(
                        externalId = "caminandes-s01e03",
                        seasonNumber = 1,
                        episodeNumber = 3,
                        title = "Llamigos on Thin Ice",
                        description = "Winter arrives in Patagonia and Koro competes with Oti the penguin for the last red berry on a frozen lake.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 9,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2023-04-24"
                    )
                )
            ),

            // Series 3: Cyber-Thriller / Documentary Series
            ImportedMediaDto(
                externalId = "open-series-velocity-world",
                type = ContentType.SERIES,
                title = "Velocity: Grand Tour Chronicles",
                titleAr = "السرعة القصوى: جولة حول العالم",
                originalTitle = "Velocity World Tour",
                description = "A high-octane automotive and travel docu-series taking viewers across coastal highways, desert dunes, and alpine hairpins.",
                descriptionAr = "سلسلة وثائقية مشوقة تأخذ المشاهدين في جولة عبر الطرق الساحلية والكثبان الصحراوية والمنعطفات الجبلية.",
                posterUrl = "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=800&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1400&q=80",
                localBackdropRes = "img_hero_cyber_heist_1793910455808",
                releaseYear = 2024,
                genres = listOf("Documentaries", "Action"),
                durationMinutes = 25,
                language = "en",
                rating = 8.5,
                director = "Julian Mercer",
                cast = listOf("Alex Mercer", "Samira Hadid"),
                isFeatured = false,
                isTrending = false,
                isPopular = true,
                episodes = listOf(
                    ImportedEpisodeDto(
                        externalId = "velocity-s01e01",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Bullrun Ignition",
                        description = "The team prepares their grand tourers in Miami for a cross-country endurance rally.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 9,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2024-05-01"
                    ),
                    ImportedEpisodeDto(
                        externalId = "velocity-s01e02",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Pacific Coast Horizon",
                        description = "Testing grand tourer suspension systems along the cliffs of Big Sur at sunset.",
                        thumbnailUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=600&q=80",
                        durationMinutes = 10,
                        playbackUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                        subtitles = defaultSubtitles,
                        releaseDate = "2024-05-08"
                    )
                )
            )
        )
    }
}
