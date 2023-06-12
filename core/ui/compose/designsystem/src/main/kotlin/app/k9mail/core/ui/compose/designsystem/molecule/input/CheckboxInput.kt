package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

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
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
            TextBody1(text = text)
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun CheckboxInputPreview() {
    PreviewWithThemes {
        CheckboxInput(
            text = "CheckboxInput",
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun CheckboxInputWithErrorPreview() {
    PreviewWithThemes {
        CheckboxInput(
            text = "CheckboxInput",
            checked = false,
            onCheckedChange = {},
            errorMessage = "Error message",
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun CheckboxInputCheckedPreview() {
    PreviewWithThemes {
        CheckboxInput(
            text = "CheckboxInput",
            checked = true,
            onCheckedChange = {},
        )
    }
}
