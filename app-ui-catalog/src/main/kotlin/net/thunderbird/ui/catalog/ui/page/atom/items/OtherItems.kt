package net.thunderbird.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.divider.HorizontalDivider
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedSelect
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

private const val MAX_HORIZONTAL_DIVIDER_THICKNESS = 25
fun LazyGridScope.otherItems() {
    sectionHeaderItem(text = "Others")

    horizontalDivider()
}

private fun LazyGridScope.horizontalDivider() {
    sectionSubtitleItem(text = "Horizontal Divider")
    fullSpanItem {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(defaultItemPadding()),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            var thickness by remember { mutableIntStateOf(1) }
            val outlineColor = MainTheme.colors.outline
            var color by remember { mutableStateOf(outlineColor) }
            val options = rememberColorOptions()

            TextLabelSmall(text = "Change thickness ($thickness.dp):")
            Slider(
                value = thickness.toFloat(),
                onValueChange = { thickness = it.toInt() },
                valueRange = 1f..MAX_HORIZONTAL_DIVIDER_THICKNESS.toFloat(),
                steps = MAX_HORIZONTAL_DIVIDER_THICKNESS - 1,
                modifier = Modifier
                    .padding(horizontal = MainTheme.spacings.double)
                    .align(Alignment.CenterHorizontally),
            )

            TextLabelSmall(text = "Change divider color:")
            TextFieldOutlinedSelect(
                options = remember(options) { options.keys.toImmutableList() },
                selectedOption = color,
                onValueChange = { selected -> color = selected },
                optionToStringTransformation = { color -> options.getValue(color) },
            )

            TextLabelSmall(text = "Result:")
            HorizontalDivider(
                thickness = thickness.dp,
                color = color,
                modifier = Modifier
                    .padding(vertical = MainTheme.spacings.double),
            )
        }
    }
}

@Composable
private fun rememberColorOptions(): ImmutableMap<Color, String> {
    val colorScheme = MainTheme.colors
    return remember {
        persistentMapOf(
            colorScheme.primary to "Primary",
            colorScheme.primaryContainer to "Primary Container",
            colorScheme.secondary to "Secondary",
            colorScheme.secondaryContainer to "Secondary Container",
            colorScheme.tertiary to "Tertiary",
            colorScheme.tertiaryContainer to "Tertiary Container",
            colorScheme.error to "Error",
            colorScheme.errorContainer to "Error Container",
            colorScheme.surface to "Surface",
            colorScheme.surfaceContainerLowest to "Surface Container Lowest",
            colorScheme.surfaceContainerLow to "Surface Container Low",
            colorScheme.surfaceContainer to "Surface Container",
            colorScheme.surfaceContainerHigh to "Surface Container High",
            colorScheme.surfaceContainerHighest to "Surface Container Highest",
            colorScheme.inverseSurface to "Inverse Surface",
            colorScheme.inversePrimary to "Inverse Primary",
            colorScheme.outline to "Outline",
            colorScheme.outlineVariant to "Outline Variant",
            colorScheme.surfaceBright to "Surface Bright",
            colorScheme.surfaceDim to "Surface Dim",
            colorScheme.info to "Info",
            colorScheme.infoContainer to "Info Container",
            colorScheme.success to "Success",
            colorScheme.successContainer to "Success Container",
            colorScheme.warning to "Warning",
            colorScheme.warningContainer to "Warning Container",
        )
    }
}
