package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import app.k9mail.feature.account.common.R

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
        subtitle = stringResource(id = R.string.account_common_title),
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

@DevicePreviews
@Composable
internal fun AccountTopAppBarWithBackButtonPreview() {
    PreviewWithThemes {
        AccountTopAppBarWithBackButton(
            title = "Title",
            onBackClicked = {},
        )
    }
}
