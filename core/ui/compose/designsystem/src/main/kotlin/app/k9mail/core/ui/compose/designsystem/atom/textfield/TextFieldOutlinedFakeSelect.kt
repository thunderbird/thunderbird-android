package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField
import androidx.compose.material3.Text as Material3Text

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
