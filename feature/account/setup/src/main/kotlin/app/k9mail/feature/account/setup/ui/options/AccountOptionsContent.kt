package app.k9mail.feature.account.setup.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline5
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithHeaderFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContentWithBackground
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R

@Composable
internal fun AccountOptionsContent(
    onFinishClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResponsiveContentWithBackground(
        modifier = modifier,
    ) {
        LazyColumnWithHeaderFooter(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            footer = {
                AccountOptionsFooter(
                    onFinishClick = onFinishClick,
                    onBackClick = onBackClick,
                )
            },
        ) {
            item {
                TextHeadline5(text = "Options")
            }
        }
    }
}

@Composable
private fun AccountOptionsFooter(
    onFinishClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MainTheme.spacings.quadruple,
                top = MainTheme.spacings.triple,
                end = MainTheme.spacings.quadruple,
                bottom = MainTheme.spacings.double,
            )
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ButtonOutlined(
            text = stringResource(id = R.string.account_setup_button_back),
            onClick = onBackClick,
        )
        Button(
            text = stringResource(id = R.string.account_setup_button_finish),
            onClick = onFinishClick,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsContentK9Preview() {
    K9Theme {
        AccountOptionsContent(
            onFinishClick = { },
            onBackClick = { },
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountOptionsContent(
            onFinishClick = { },
            onBackClick = { },
        )
    }
}
