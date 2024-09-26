package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun CheckboxInput(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Checkbox,
                    onValueChange = { onCheckedChange(!checked) },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
            TextBodyLarge(text = text)
        }
    }
}
