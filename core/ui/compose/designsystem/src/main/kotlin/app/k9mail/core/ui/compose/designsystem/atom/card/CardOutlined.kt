package app.k9mail.core.ui.compose.designsystem.atom.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

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
