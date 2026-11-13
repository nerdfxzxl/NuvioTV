package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
internal fun NerdFazalSettingsContent(
    viewModel: LayoutSettingsViewModel = hiltViewModel(),
    initialFocusRequester: FocusRequester? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCatalog by remember { mutableStateOf<CatalogInfo?>(null) }
    val firstStyleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedCatalog?.stableKey) {
        if (selectedCatalog != null) {
            withFrameNanos { }
            runCatching { firstStyleFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.nerd_fazal_settings_title),
            subtitle = stringResource(R.string.nerd_fazal_settings_subtitle)
        )

        SettingsGroupCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            SettingsSectionLabel(
                text = stringResource(R.string.dynamic_home_row_styles_title),
                description = stringResource(R.string.dynamic_home_row_styles_subtitle)
            )

            if (uiState.availableCatalogs.isEmpty()) {
                SettingsNote(text = stringResource(R.string.dynamic_home_no_catalogs))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
                ) {
                    items(
                        items = uiState.availableCatalogs,
                        key = { it.stableKey }
                    ) { catalog ->
                        val selectedStyle = uiState.dynamicHomeRowStyles[catalog.stableKey] ?: "AUTO"
                        SettingsActionRow(
                            title = catalog.name,
                            subtitle = catalog.addonName,
                            value = dynamicHomeStyleLabel(selectedStyle),
                            onClick = { selectedCatalog = catalog },
                            modifier = if (catalog == uiState.availableCatalogs.firstOrNull()) {
                                initialFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }

    val catalog = selectedCatalog
    if (catalog != null) {
        val selectedStyle = uiState.dynamicHomeRowStyles[catalog.stableKey] ?: "AUTO"
        NuvioDialog(
            onDismiss = { selectedCatalog = null },
            title = catalog.name,
            subtitle = catalog.addonName
        ) {
            listOf("AUTO", "PORTRAIT", "LANDSCAPE").forEachIndexed { index, style ->
                SettingsActionRow(
                    title = dynamicHomeStyleLabel(style),
                    subtitle = if (style == selectedStyle) {
                        stringResource(R.string.dynamic_home_style_selected)
                    } else {
                        null
                    },
                    value = if (style == selectedStyle) "✓" else null,
                    onClick = {
                        viewModel.onEvent(
                            LayoutSettingsEvent.SetDynamicHomeRowStyle(
                                catalogKey = catalog.stableKey,
                                styleName = style
                            )
                        )
                        selectedCatalog = null
                    },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstStyleFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@Composable
private fun dynamicHomeStyleLabel(styleName: String): String = when (styleName) {
    "AUTO" -> stringResource(R.string.dynamic_home_style_auto)
    "PORTRAIT" -> stringResource(R.string.dynamic_home_style_portrait)
    "LANDSCAPE" -> stringResource(R.string.dynamic_home_style_landscape)
    else -> styleName
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
}
