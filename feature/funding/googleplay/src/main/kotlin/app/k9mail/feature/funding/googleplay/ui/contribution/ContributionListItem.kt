package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun ContributionListItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Box(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MainTheme.colors.primary else MainTheme.colors.outlineVariant,
                shape = MainTheme.shapes.small,
            )
            .clickable(
                onClick = onClick,
                enabled = !isSelected,
            ),
        contentAlignment = Alignment.Center,
    ) {
        TextBodyMedium(
            text = text,
            modifier = Modifier.padding(
                horizontal = MainTheme.spacings.triple,
                vertical = MainTheme.spacings.oneHalf,
            ),
        )
    }
}
