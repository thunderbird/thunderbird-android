package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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
                color = if (isSelected) BoltTheme.colors.primary else BoltTheme.colors.outlineVariant,
                shape = BoltTheme.shapes.small,
            )
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        TextBodyMedium(
            text = text,
            modifier = Modifier.padding(
                horizontal = BoltTheme.spacings.triple,
                vertical = BoltTheme.spacings.oneHalf,
            ),
        )
    }
}
