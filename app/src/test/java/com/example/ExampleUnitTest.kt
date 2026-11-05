package com.example

import com.example.core.model.SearchNormalizer
import com.example.core.model.SourceType
import com.example.core.network.NetworkClient
import com.example.datasource.connectors.LegalSampleCatalogProvider
import com.example.datasource.connectors.SourceConnectorRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun searchNormalizer_normalizesArabicAndEnglishQueries() {
        assertTrue(SearchNormalizer.matches("افلام", "أفلام سينمائية"))
        assertTrue(SearchNormalizer.matches("الأفق", "دموع الفولاذ: الافق الاسود"))
        assertTrue(SearchNormalizer.matches("steel", "Tears of Steel: Obsidian Horizon"))
    }

    @Test
    fun sourceRegistry_detectsSourceTypeFromUrl() {
        assertEquals(
            SourceType.M3U_PLAYLIST,
            SourceConnectorRegistry.detectSourceTypeFromUrl("https://cdn.example.com/vod/playlist.m3u8")
        )
        assertEquals(
            SourceType.XML_RSS_FEED,
            SourceConnectorRegistry.detectSourceTypeFromUrl("https://syndicate.example.com/feed.rss")
        )
        assertEquals(
            SourceType.GRAPHQL_API,
            SourceConnectorRegistry.detectSourceTypeFromUrl("https://api.example.com/graphql")
        )
        assertEquals(
            SourceType.JSON_FEED,
            SourceConnectorRegistry.detectSourceTypeFromUrl("https://api.example.com/catalog.json")
        )
    }

    @Test
    fun networkClient_enforcesHttpsByDefault() {
        assertTrue(NetworkClient.isSecureUrl("https://api.example.com/catalog.json"))
        assertTrue(NetworkClient.isSecureUrl("kras://open-json-feed"))
        assertFalse(NetworkClient.isSecureUrl("http://insecure.example.com/feed.json"))
    }

    @Test
    fun legalDemoCatalog_containsValidMoviesAndMultiSeasonSeries() {
        val catalog = LegalSampleCatalogProvider.getLegalDemoCatalog()
        assertTrue(catalog.isNotEmpty())
        val series = catalog.filter { it.episodes.isNotEmpty() }
        assertTrue(series.isNotEmpty())
        assertTrue(series.first().episodes.all { it.playbackUrl.startsWith("https://") })
    }
}
