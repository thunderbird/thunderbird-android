package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

/**
 * Top app bar for the account screens with a back button.
 */
@Composable
fun AccountTopAppBarWithBackButton(
    title: String,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        titleContentPadding = PaddingValues(
            start = MainTheme.spacings.default,
        ),
        navigationIcon = {
            ButtonIcon(
                onClick = onBackClicked,
                imageVector = Icons.Outlined.arrowBack,
            )
        },
    )
}

@PreviewDevices
@Composable
internal fun AccountTopAppBarWithBackButtonPreview() {
    PreviewWithThemes {
        AccountTopAppBarWithBackButton(
            title = "Title",
            onBackClicked = {},
        )
    }
}
