package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.FilledTonalButton as Material3FilledTonalButton
import androidx.compose.material3.Text as Material3Text

@Composable
fun ButtonFilledTonal(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = buttonContentPadding(),
) {
    Material3FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
    ) {
        Material3Text(text = text)
    }
}
