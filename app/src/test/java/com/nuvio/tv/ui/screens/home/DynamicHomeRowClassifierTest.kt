package com.nuvio.tv.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicHomeRowClassifierTest {
    private fun catalog(
        title: String,
        count: Int = 12,
        key: String = title.lowercase().replace(' ', '_'),
        manual: DynamicHomeRowStyle = DynamicHomeRowStyle.AUTO
    ) = DynamicHomeRowInput(
        key = key,
        title = title,
        catalogId = title,
        hasAddonBaseUrl = true,
        hasCatalogIdentity = true,
        itemCount = count,
        initialStyle = DynamicHomeRowClassifier.initialStyleForCatalog(title),
        manualStyle = manual
    )

    @Test
    fun firstEligibleAddonCatalogBecomesFeaturedBeforeAlternation() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                DynamicHomeRowInput(
                    key = "continue_watching",
                    title = "Continue Watching",
                    itemCount = 4,
                    initialStyle = DynamicHomeRowStyle.CONTINUE_WATCHING
                ),
                catalog("Movies"),
                catalog("Series"),
                catalog("Anime"),
                catalog("Documentaries")
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.CONTINUE_WATCHING,
                DynamicHomeRowStyle.FEATURED,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.LANDSCAPE,
                DynamicHomeRowStyle.PORTRAIT
            ),
            result
        )
    }

    @Test
    fun channelCatalogKeepsLandscapeAndConsumesFeaturedSlot() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                catalog("Live Channels").copy(initialStyle = DynamicHomeRowStyle.LANDSCAPE),
                catalog("Movies"),
                catalog("Series")
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.LANDSCAPE,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.LANDSCAPE
            ),
            result
        )
    }

    @Test
    fun continueWatchingAndUpcomingDoNotAdvanceAlternation() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                catalog("Movies"),
                DynamicHomeRowInput(
                    key = "continue_watching",
                    title = "Continue Watching",
                    itemCount = 4,
                    initialStyle = DynamicHomeRowStyle.CONTINUE_WATCHING
                ),
                DynamicHomeRowInput(
                    key = "upcoming_section",
                    title = "Upcoming",
                    itemCount = 3,
                    initialStyle = DynamicHomeRowStyle.CONTINUE_WATCHING
                ),
                catalog("Series")
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.FEATURED,
                DynamicHomeRowStyle.CONTINUE_WATCHING,
                DynamicHomeRowStyle.CONTINUE_WATCHING,
                DynamicHomeRowStyle.PORTRAIT
            ),
            result
        )
    }

    @Test
    fun firstRankingAfterFeaturedRowGetsNumberedTopTen() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(catalog("Movies"), catalog("Top 10 Movies", count = 5), catalog("Series"))
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.FEATURED,
                DynamicHomeRowStyle.TOP_10_NUMBERED,
                DynamicHomeRowStyle.PORTRAIT
            ),
            result
        )
    }

    @Test
    fun rankingWithoutAddonBaseUrlDoesNotGetNumberedTreatment() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                DynamicHomeRowInput(
                    key = "top_movies",
                    title = "Top Movies",
                    catalogId = "top_movies",
                    hasCatalogIdentity = true,
                    itemCount = 8
                ),
                catalog("Series")
            )
        )

        assertEquals(
            listOf(DynamicHomeRowStyle.PORTRAIT, DynamicHomeRowStyle.FEATURED),
            result
        )
    }

    @Test
    fun preclassifiedStackedRankingUsesTheSingleNumberedSlot() {
        val result = DynamicHomeRowRowClassifierTestHelper.classifyStackedRows()

        assertEquals(
            listOf(
                DynamicHomeRowStyle.FEATURED,
                DynamicHomeRowStyle.TOP_10_NUMBERED,
                DynamicHomeRowStyle.PORTRAIT
            ),
            result
        )
    }

    @Test
    fun rankingWithFewerThanFiveItemsDoesNotConsumeNumberedSlot() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                catalog("Movies"),
                catalog("Top 10 Movies", count = 4),
                catalog("Trending Series", count = 8)
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.FEATURED,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.TOP_10_NUMBERED
            ),
            result
        )
    }

    @Test
    fun laterRecognisedRankingStaysPortraitAndAdvancesAlternation() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                catalog("Movies"),
                catalog("Top 10 Movies"),
                catalog("Trending Series"),
                catalog("Documentaries")
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.FEATURED,
                DynamicHomeRowStyle.TOP_10_NUMBERED,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.LANDSCAPE
            ),
            result
        )
    }

    @Test
    fun manualStyleIsAppliedWhileCatalogIsEmptyOrLoading() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                DynamicHomeRowInput(
                    key = "loading_catalog",
                    title = "Movies",
                    catalogId = "movies",
                    hasAddonBaseUrl = true,
                    hasCatalogIdentity = true,
                    itemCount = 0,
                    initialStyle = DynamicHomeRowStyle.PORTRAIT,
                    manualStyle = DynamicHomeRowStyle.LANDSCAPE
                )
            )
        )

        assertEquals(listOf(DynamicHomeRowStyle.LANDSCAPE), result)
    }

    @Test
    fun manualStyleOverridesFeaturedAndDoesNotAdvanceAlternation() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                catalog("Movies", manual = DynamicHomeRowStyle.LANDSCAPE),
                catalog("Series")
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.LANDSCAPE,
                DynamicHomeRowStyle.PORTRAIT
            ),
            result
        )
    }

    @Test
    fun catalogProfileDetectionMatchesLegacySpecialNames() {
        assertEquals(
            DynamicHomeRowStyle.TOP_10_STACKED,
            DynamicHomeRowClassifier.initialStyleForCatalog("Top 100 · Movies")
        )
        assertEquals(
            DynamicHomeRowStyle.TIMELINE,
            DynamicHomeRowClassifier.initialStyleForCatalog("The Saga Collection")
        )
        assertEquals(
            DynamicHomeRowStyle.COLLECTION,
            DynamicHomeRowClassifier.initialStyleForCatalog("Oscar Winners")
        )
        assertEquals(
            DynamicHomeRowStyle.SERVICE,
            DynamicHomeRowClassifier.initialStyleForCatalog("Netflix Originals")
        )
        assertEquals(
            DynamicHomeRowStyle.PORTRAIT,
            DynamicHomeRowClassifier.initialStyleForCatalog("Christopher Nolan")
        )
    }

    @Test
    fun specialInitialStyleIsPreservedForNonCatalogRows() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(
                DynamicHomeRowInput(
                    key = "special",
                    title = "Editorial",
                    itemCount = 1,
                    initialStyle = DynamicHomeRowStyle.HERO
                ),
                catalog("Movies")
            )
        )

        assertEquals(
            listOf(DynamicHomeRowStyle.HERO, DynamicHomeRowStyle.FEATURED),
            result
        )
    }
}

private object DynamicHomeRowRowClassifierTestHelper {
    fun classifyStackedRows(): List<DynamicHomeRowStyle> =
        DynamicHomeRowClassifier.classify(
            listOf(
                DynamicHomeRowInput(
                    key = "addon_movie_top",
                    title = "Movies",
                    catalogId = "catalog_top",
                    hasAddonBaseUrl = true,
                    itemCount = 12,
                    initialStyle = DynamicHomeRowStyle.TOP_10_STACKED
                ),
                DynamicHomeRowInput(
                    key = "addon_series_top",
                    title = "Series",
                    catalogId = "catalog_series",
                    hasAddonBaseUrl = true,
                    itemCount = 12,
                    initialStyle = DynamicHomeRowStyle.TOP_10_STACKED
                ),
                DynamicHomeRowInput(
                    key = "addon_documentaries",
                    title = "Documentaries",
                    catalogId = "documentaries",
                    hasAddonBaseUrl = true,
                    itemCount = 12
                )
            )
        )
}
