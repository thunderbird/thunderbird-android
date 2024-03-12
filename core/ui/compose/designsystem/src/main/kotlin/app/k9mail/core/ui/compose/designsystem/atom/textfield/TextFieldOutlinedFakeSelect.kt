package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.OutlinedTextField as MaterialOutlinedTextField

@Composable
fun TextFieldOutlinedFakeSelect(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    MaterialOutlinedTextField(
        value = text,
        onValueChange = { },
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        readOnly = true,
        label = optionalLabel(label),
        trailingIcon = { Icon(Icons.Outlined.arrowDropDown) },
        singleLine = true,
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            onClick()
                        }
                    }
                }
            },
    )
}

private fun optionalLabel(label: String?): @Composable (() -> Unit)? = label?.let { { Text(label) } }

@Preview
@Composable
internal fun TextFieldOutlinedFakeSelectPreview() {
    PreviewWithThemes {
        TextFieldOutlinedFakeSelect(
            text = "Current value",
            onClick = {},
        )
    }
}

@Preview
@Composable
internal fun TextFieldOutlinedFakeSelectPreviewWithLabel() {
    PreviewWithThemes {
        TextFieldOutlinedFakeSelect(
            text = "Current value",
            onClick = {},
            label = "Label",
        )
    }
}
