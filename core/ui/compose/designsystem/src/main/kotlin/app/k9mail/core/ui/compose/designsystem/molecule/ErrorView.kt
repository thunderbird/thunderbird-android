package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.button.buttonContentPadding
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody2
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun ErrorView(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    onRetry: () -> Unit = { },
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
                .padding(
                    vertical = MainTheme.spacings.default,
                    horizontal = MainTheme.spacings.double,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            Icon(
                imageVector = Icons.Filled.error,
                contentDescription = null,
                tint = MainTheme.colors.error,
                modifier = Modifier.padding(top = MainTheme.spacings.default),
            )
            TextSubtitle1(
                text = title,
                modifier = Modifier.padding(bottom = MainTheme.spacings.default),
            )
            if (message != null) {
                TextBody2(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                ButtonText(
                    text = stringResource(id = R.string.designsystem_molecule_error_view_button_retry),
                    onClick = onRetry,
                    contentPadding = buttonContentPadding(
                        start = MainTheme.spacings.double,
                        end = MainTheme.spacings.double,
                    ),
                )
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
