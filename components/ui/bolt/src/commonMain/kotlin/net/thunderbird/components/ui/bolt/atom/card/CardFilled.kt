package net.thunderbird.components.ui.bolt.atom.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card as Material3Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun CardFilled(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Material3Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors.toMaterial3CardColors(),
            elevation = elevation.toMaterial3CardElevation(),
            content = content,
        )
    } else {
        Material3Card(
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
internal fun CardFilledPreview() {
    PreviewWithThemes {
        CardFilled {
            Box(modifier = Modifier.padding(MainTheme.spacings.double)) {
                TextBodyMedium("Text in card")
            }
        }
    }
}
