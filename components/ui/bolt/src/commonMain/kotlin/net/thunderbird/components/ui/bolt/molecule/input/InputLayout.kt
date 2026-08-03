package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlined
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * Layout for input fields that displays an optional error or warning message below the input.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param contentPadding Padding values to be applied around the content.
 * @param errorMessage Optional error message to be displayed below the input.
 * @param warningMessage Optional warning message to be displayed below the input.
 * @param content Composable content representing the input field.
 */
@Composable
fun InputLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = inputContentPadding(),
    errorMessage: String? = null,
    warningMessage: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        content()

        val messageState = remember(errorMessage, warningMessage) {
            errorMessage?.let {
                MessageState.Error(it)
            } ?: warningMessage?.let {
                MessageState.Warning(it)
            }
        }

        AnimatedContent(
            targetState = messageState,
            label = "ErrorMessageAnimation",
            transitionSpec = {
                slideInVertically { height -> height } togetherWith
                    slideOutVertically { height -> -height }
            },
        ) { message ->
            if (message != null) {
                val color = when (message) {
                    is MessageState.Error -> BoltTheme.colors.error
                    is MessageState.Warning -> BoltTheme.colors.warning
                }

                TextBodySmall(
                    text = message.text,
                    modifier = Modifier.padding(start = BoltTheme.spacings.double, top = BoltTheme.spacings.half),
                    color = color,
                )
            }
        }
    }
}

private sealed interface MessageState {
    val text: String

    data class Error(override val text: String) : MessageState
    data class Warning(override val text: String) : MessageState
}

@Composable
@Preview(showBackground = true)
internal fun InputLayoutPreview() {
    PreviewWithThemes {
        InputLayout {
            TextFieldOutlined(value = "InputLayout", onValueChange = {})
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun InputLayoutWithErrorPreview() {
    PreviewWithThemes {
        InputLayout(
            errorMessage = "Error message",
        ) {
            TextFieldOutlined(value = "InputLayout", onValueChange = {})
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun InputLayoutWithWarningPreview() {
    PreviewWithThemes {
        InputLayout(
            warningMessage = "Warning message",
        ) {
            TextFieldOutlined(value = "InputLayout", onValueChange = {})
        }
    }
}
