package net.thunderbird.components.ui.bolt.atom.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun CardOutlined(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors.toMaterial3CardColors(),
            elevation = elevation.toMaterial3CardElevation(),
            border = border,
            content = content,
        )
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = shape,
            colors = colors.toMaterial3CardColors(),
            elevation = elevation.toMaterial3CardElevation(),
            border = border,
            content = content,
        )
    }
}

@PreviewLightDark
@Composable
private fun CardOutlinedPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(BoltTheme.spacings.quadruple)) {
            CardOutlined {
                Box(modifier = Modifier.padding(BoltTheme.spacings.double)) {
                    TextBodyMedium("Text in card")
                }
            }
        }
    }
}
