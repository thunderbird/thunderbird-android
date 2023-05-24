package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.Switch
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun SwitchInput(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    Row(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        TextBody1(text = text)
    }
}

@Preview(showBackground = true)
@Composable
internal fun SwitchInputPreview() {
    PreviewWithThemes {
        SwitchInput(
            text = "SwitchInput",
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun SwitchInputCheckedPreview() {
    PreviewWithThemes {
        SwitchInput(
            text = "SwitchInput",
            checked = true,
            onCheckedChange = {},
        )
    }
}
