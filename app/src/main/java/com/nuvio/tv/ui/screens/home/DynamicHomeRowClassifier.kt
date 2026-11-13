package com.nuvio.tv.ui.screens.home

import java.text.Normalizer
import java.util.Locale

/**
 * Presentation styles supported by the Dynamic home. AUTO is a preference value;
 * the classifier always resolves it to a concrete style before rendering.
 */
internal enum class DynamicHomeRowStyle {
    AUTO,
    PORTRAIT,
    LANDSCAPE,
    COLLECTION,
    SERVICE,
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
     * - Continue Watching and Upcoming do not advance the portrait/landscape cycle;
     * - the first eligible ranking (5+ items) gets numbered Top 10;
     * - later recognised rankings stay portrait and advance the cycle;
     * - ordinary rows alternate portrait, landscape, starting with portrait.
     */
    fun classify(rows: List<DynamicHomeRowInput>): List<DynamicHomeRowStyle> {
        var numberedRankingAssigned = 0
        var alternatingRows = 0

        return rows.map { row ->
            if (row.manualStyle != DynamicHomeRowStyle.AUTO) {
                return@map row.manualStyle
            }

            if (row.itemCount < 1 || row.initialStyle != DynamicHomeRowStyle.PORTRAIT) {
                return@map row.initialStyle
            }

            if (row.key == CONTINUE_WATCHING_KEY || row.key == UPCOMING_KEY) {
                return@map row.initialStyle
            }

            val isCatalog = row.hasCatalogIdentity &&
                !row.catalogId.isNullOrBlank()

            if (isCatalog &&
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
                val style = if ((alternatingRows++ and 1) == 1) {
                    DynamicHomeRowStyle.LANDSCAPE
                } else {
                    DynamicHomeRowStyle.PORTRAIT
                }
                style
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
