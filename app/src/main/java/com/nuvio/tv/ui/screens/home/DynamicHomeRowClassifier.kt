package com.nuvio.tv.ui.screens.home

import java.text.Normalizer
import java.util.Locale

/**
 * Presentation styles supported by the Dynamic home. AUTO is a preference value;
 * the classifier always resolves it to a concrete style before rendering.
 */
internal enum class DynamicHomeRowStyle {
    AUTO,
    CONTINUE_WATCHING,
    PORTRAIT,
    POSTER,
    LANDSCAPE,
    FEATURED,
    FEATURED_SQUARE,
    COLLECTION,
    SERVICE,
    SOCIAL,
    RETURN,
    CATALOG_SHORTCUTS,
    TOP_10_STACKED,
    TOP_10_NUMBERED,
    LARGE_4_3,
    LARGE_4_3_MEDIUM,
    LARGE_4_3_LARGE,
    TIMELINE,
    INFO_CARD,
    STAGE,
    CIRCULAR_PROFILES,
    HERO,
    LARGE_POSTER,
    LARGE_POSTER_SMALL,
    LARGE_POSTER_LARGE,
    SPLIT_HERO
}

/**
 * The initial style represents the row's native/special presentation before
 * Dynamic's automatic rules are applied. Manual style is stored separately and
 * always wins when it is not AUTO.
 */
internal data class DynamicHomeRowInput(
    val key: String,
    val title: String,
    val catalogId: String? = null,
    val hasAddonBaseUrl: Boolean = false,
    val hasCatalogIdentity: Boolean = catalogId != null,
    val itemCount: Int,
    val initialStyle: DynamicHomeRowStyle = DynamicHomeRowStyle.PORTRAIT,
    val manualStyle: DynamicHomeRowStyle = DynamicHomeRowStyle.AUTO
)

internal object DynamicHomeRowClassifier {
    private val rankingTerms = listOf(
        "top",
        "em alta",
        "trending",
        "popular",
        "mais vist",
        "most watched",
        "ranking",
        "tendência"
    )

    /**
     * Mirrors Legacy's dinAtribuirTipos ordering:
     * - manually selected styles are never overwritten;
     * - only native portrait/catalog rows enter automatic classification;
     * - the first eligible addon catalogue becomes the 16:9 featured row;
     * - Continue Watching and Upcoming do not advance the portrait/landscape cycle;
     * - the first eligible ranking (5+ items) after the feature gets numbered Top 10;
     * - later recognised rankings stay portrait and advance the cycle;
     * - ordinary rows alternate portrait, landscape, starting with portrait.
     */
    fun initialStyleForCatalog(title: String): DynamicHomeRowStyle {
        val normalized = normalize(title)
        return when {
            listOf("top 100", "top100", "top 10", "top10").any(normalized::contains) ->
                DynamicHomeRowStyle.TOP_10_STACKED
            listOf("saga", "franquia", "franchise", "filmograf", "timeline", "cronolog", "linha do tempo")
                .any(normalized::contains) -> DynamicHomeRowStyle.TIMELINE
            listOf("oscar", "academy", "award", "premia", "cannes", "golden globe")
                .any(normalized::contains) -> DynamicHomeRowStyle.COLLECTION
            listOf("netflix", "disney", "prime video", "amazon", "apple tv", "hbo", "max -", "paramount", "globoplay", "mubi", "crunchyroll")
                .any(normalized::contains) -> DynamicHomeRowStyle.SERVICE
            else -> DynamicHomeRowStyle.PORTRAIT
        }
    }

    fun classify(rows: List<DynamicHomeRowInput>): List<DynamicHomeRowStyle> {
        var featuredAssigned = false
        var numberedRankingAssigned = 0
        var alternatingRows = 0

        return rows.map { row ->
            if (row.itemCount < 1) {
                // Keep a user's choice visible even while a row is empty/loading.
                return@map row.manualStyle.takeUnless { it == DynamicHomeRowStyle.AUTO }
                    ?: row.initialStyle
            }

            val isCatalog = row.hasAddonBaseUrl &&
                row.hasCatalogIdentity &&
                !row.catalogId.isNullOrBlank()

            // Legacy assigns the first non-empty addon catalogue to the 16:9
            // showcase during catalogue assembly, before dinAtribuirTipos runs.
            val isContinueOrUpcoming =
                row.key == CONTINUE_WATCHING_KEY || row.key == UPCOMING_KEY

            if (!featuredAssigned && isCatalog && !isContinueOrUpcoming) {
                featuredAssigned = true
                return@map when {
                    row.manualStyle != DynamicHomeRowStyle.AUTO -> row.manualStyle
                    row.initialStyle == DynamicHomeRowStyle.LANDSCAPE -> DynamicHomeRowStyle.LANDSCAPE
                    else -> DynamicHomeRowStyle.FEATURED
                }
            }

            // Manual choice wins for every row, but does not rewrite the automatic
            // counters once the first catalogue's showcase slot has been accounted for.
            if (row.manualStyle != DynamicHomeRowStyle.AUTO) {
                return@map row.manualStyle
            }

            if (isContinueOrUpcoming) {
                return@map row.initialStyle
            }

            if (row.initialStyle == DynamicHomeRowStyle.TOP_10_STACKED && isCatalog) {
                if (numberedRankingAssigned < 1) {
                    numberedRankingAssigned++
                    DynamicHomeRowStyle.TOP_10_NUMBERED
                } else {
                    alternatingRows = 1
                    DynamicHomeRowStyle.PORTRAIT
                }
            } else if (row.initialStyle != DynamicHomeRowStyle.PORTRAIT) {
                row.initialStyle
            } else if (isCatalog &&
                numberedRankingAssigned < 1 &&
                row.itemCount >= MIN_NUMBERED_RANKING_ITEMS &&
                isRecognisedRanking(row)
            ) {
                numberedRankingAssigned++
                DynamicHomeRowStyle.TOP_10_NUMBERED
            } else if (numberedRankingAssigned > 0 && isCatalog && isRecognisedRanking(row)) {
                // A ranking without the single numbered slot stays portrait,
                // but Legacy advances the alternation counter for the next row.
                alternatingRows = 1
                DynamicHomeRowStyle.PORTRAIT
            } else {
                if ((alternatingRows++ and 1) == 1) {
                    DynamicHomeRowStyle.LANDSCAPE
                } else {
                    DynamicHomeRowStyle.PORTRAIT
                }
            }
        }
    }

    private fun isRecognisedRanking(row: DynamicHomeRowInput): Boolean =
        rankingTerms.any { term ->
            startsAtWord(row.title, term) ||
                startsAtWord(row.catalogId.orEmpty(), term)
        }

    private fun startsAtWord(value: String, term: String): Boolean {
        val normalizedValue = normalize(value)
        val normalizedTerm = normalize(term)
        var fromIndex = 0
        while (fromIndex < normalizedValue.length) {
            val index = normalizedValue.indexOf(normalizedTerm, fromIndex)
            if (index < 0) return false
            if (index == 0 || !normalizedValue[index - 1].isLetterOrDigit()) return true
            fromIndex = index + 1
        }
        return false
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }

    private const val MIN_NUMBERED_RANKING_ITEMS = 5
    private const val CONTINUE_WATCHING_KEY = "continue_watching"
    private const val UPCOMING_KEY = "upcoming_section"
}
