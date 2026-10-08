package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge

@Composable
fun RadioButton(
    selected: Boolean,
    label: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
        )
        label()
    }
}

@Composable
fun RadioButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    RadioButton(
        selected = selected,
        label = { TextLabelLarge(label) },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}
