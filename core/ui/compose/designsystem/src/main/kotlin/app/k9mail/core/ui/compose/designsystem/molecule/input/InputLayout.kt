package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme

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
                    is MessageState.Error -> MainTheme.colors.error
                    is MessageState.Warning -> MainTheme.colors.warning
                }

                TextBodySmall(
                    text = message.text,
                    modifier = Modifier.padding(start = MainTheme.spacings.double, top = MainTheme.spacings.half),
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
