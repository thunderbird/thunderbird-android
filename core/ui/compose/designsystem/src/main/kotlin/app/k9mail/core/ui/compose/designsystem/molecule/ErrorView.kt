package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody2
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

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
                .padding(MainTheme.spacings.double),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                Icon(
                    imageVector = Icons.Filled.error,
                    contentDescription = null,
                    tint = MainTheme.colors.error,
                )
                TextSubtitle1(
                    text = title,
                    textAlign = TextAlign.Center,
                )
            }

            if (message != null) {
                Spacer(modifier = Modifier.height(MainTheme.spacings.double))
                TextBody2(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(MainTheme.spacings.default))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ButtonText(
                        text = stringResource(id = R.string.designsystem_molecule_error_view_button_retry),
                        onClick = onRetry,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun ErrorViewPreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ErrorViewWithMessagePreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            message = "Something went wrong.",
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ErrorViewWithRetryPreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ErrorViewWithRetryAndMessagePreview() {
    PreviewWithThemes {
        ErrorView(
            title = "Error",
            message = "Something went wrong.",
            onRetry = {},
        )
    }
}
