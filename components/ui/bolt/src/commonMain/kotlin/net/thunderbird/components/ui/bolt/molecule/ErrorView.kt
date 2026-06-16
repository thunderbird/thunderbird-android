package net.thunderbird.components.ui.bolt.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextHeadlineSmall
import net.thunderbird.components.ui.bolt.resources.Res
import net.thunderbird.components.ui.bolt.resources.bolt_molecule_error_view_button_retry
import net.thunderbird.components.ui.bolt.theme.MainTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ErrorView(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    onRetry: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        contentAlignment = contentAlignment,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MainTheme.spacings.triple),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    tint = MainTheme.colors.error,
                )
                Spacer(modifier = Modifier.height(MainTheme.spacings.double))
                TextHeadlineSmall(
                    text = title,
                    textAlign = TextAlign.Center,
                )
            }

            if (message != null) {
                Spacer(modifier = Modifier.height(MainTheme.spacings.double))
                TextBodyMedium(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(MainTheme.spacings.triple))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ButtonText(
                        text = stringResource(Res.string.bolt_molecule_error_view_button_retry),
                        onClick = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun ErrorViewPreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ErrorViewWithMessagePreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            message = "Something went wrong.",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ErrorViewWithRetryPreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            onRetry = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ErrorViewWithRetryAndMessagePreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            message = "Something went wrong.",
            onRetry = {},
        )
    }
}
