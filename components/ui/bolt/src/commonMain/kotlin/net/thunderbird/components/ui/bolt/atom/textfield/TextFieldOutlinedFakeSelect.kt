package net.thunderbird.components.ui.bolt.atom.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField
import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@Composable
fun TextFieldOutlinedFakeSelect(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Material3OutlinedTextField(
        value = text,
        onValueChange = { },
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        readOnly = true,
        label = optionalLabel(label),
        trailingIcon = { Icon(Icons.Outlined.ExpandMore) },
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

private fun optionalLabel(label: String?): @Composable (() -> Unit)? = label?.let { { Material3Text(label) } }

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedFakeSelectPreview() {
    PreviewWithThemes {
        TextFieldOutlinedFakeSelect(
            text = "Current value",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedFakeSelectPreviewWithLabel() {
    PreviewWithThemes {
        TextFieldOutlinedFakeSelect(
            text = "Current value",
            onClick = {},
            label = "Label",
        )
    }
}
