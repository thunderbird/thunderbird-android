package net.thunderbird.components.ui.bolt.atom.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard as Material3ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun CardElevated(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Material3ElevatedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors.toMaterial3CardColors(),
            elevation = elevation.toMaterial3CardElevation(),
            content = content,
        )
    } else {
        Material3ElevatedCard(
            modifier = modifier,
            shape = shape,
            colors = colors.toMaterial3CardColors(),
            elevation = elevation.toMaterial3CardElevation(),
            content = content,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun CardElevatedPreview() {
    PreviewWithThemes {
        CardElevated {
            Box(modifier = Modifier.padding(MainTheme.spacings.double)) {
                TextBodyMedium("Text in card")
            }
        }
    }
}
