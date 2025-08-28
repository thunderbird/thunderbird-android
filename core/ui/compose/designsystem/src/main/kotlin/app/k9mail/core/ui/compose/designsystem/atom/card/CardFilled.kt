package app.k9mail.core.ui.compose.designsystem.atom.card

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.Card as Material3Card

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
