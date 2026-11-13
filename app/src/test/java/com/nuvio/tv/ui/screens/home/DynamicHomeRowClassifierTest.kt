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
        manualStyle = manual
    )

    @Test
    fun ordinaryCatalogRowsAlternateStartingWithPortrait() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(catalog("Movies"), catalog("Series"), catalog("Anime"), catalog("Documentaries"))
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.PORTRAIT,
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
                    itemCount = 4
                ),
                DynamicHomeRowInput(
                    key = "upcoming_section",
                    title = "Upcoming",
                    itemCount = 3
                ),
                catalog("Series")
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.LANDSCAPE
            ),
            result
        )
    }

    @Test
    fun firstRecognisedRankingWithFiveItemsGetsNumberedTopTen() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(catalog("Movies"), catalog("Top 10 Movies", count = 5), catalog("Series"))
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.TOP_10_NUMBERED,
                DynamicHomeRowStyle.LANDSCAPE
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
            listOf(DynamicHomeRowStyle.PORTRAIT, DynamicHomeRowStyle.LANDSCAPE),
            result
        )
    }

    @Test
    fun preclassifiedStackedRankingUsesTheSingleNumberedSlot() {
        val result = DynamicHomeRowRowClassifierTestHelper.classifyStackedRows()

        assertEquals(
            listOf(
                DynamicHomeRowStyle.TOP_10_NUMBERED,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.LANDSCAPE
            ),
            result
        )
    }

    @Test
    fun rankingWithFewerThanFiveItemsDoesNotConsumeNumberedSlot() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(catalog("Top 10 Movies", count = 4), catalog("Trending Series", count = 8))
        )

        assertEquals(
            listOf(
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
                catalog("Top 10 Movies"),
                catalog("Trending Series"),
                catalog("Documentaries")
            )
        )

        assertEquals(
            listOf(
                DynamicHomeRowStyle.TOP_10_NUMBERED,
                DynamicHomeRowStyle.PORTRAIT,
                DynamicHomeRowStyle.LANDSCAPE
            ),
            result
        )
    }

    @Test
    fun manualStyleOverridesAutomaticClassification() {
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
    fun rankingTermMustStartAtAWordBoundary() {
        val result = DynamicHomeRowClassifier.classify(
            listOf(catalog("Christopher Nolan"), catalog("Popular Movies"))
        )

        assertEquals(
            listOf(DynamicHomeRowStyle.PORTRAIT, DynamicHomeRowStyle.LANDSCAPE),
            result
        )
    }

    @Test
    fun specialInitialStyleIsPreserved() {
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
            listOf(DynamicHomeRowStyle.HERO, DynamicHomeRowStyle.PORTRAIT),
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
