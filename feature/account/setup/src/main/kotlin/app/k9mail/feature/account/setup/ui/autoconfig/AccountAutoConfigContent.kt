package app.k9mail.feature.account.setup.ui.autoconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline2
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithHeaderFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContentWithBackground
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R

@Composable
internal fun AccountAutoConfigContent(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResponsiveContentWithBackground(
        modifier = modifier,
    ) {
        LazyColumnWithHeaderFooter(
            modifier = Modifier.fillMaxSize(),
            header = {
                AccountSetupHeader()
            },
            footer = {
                AccountSetupFooter(
                    onNextClick = onNextClick,
                    onBackClick = onBackClick,
                )
            },
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double, Alignment.CenterVertically),
        ) {
            item { Spacer(modifier = Modifier.height(MainTheme.sizes.larger)) }
            item {
                AccountSetupEmailForm(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Spacer(modifier = Modifier.height(MainTheme.sizes.larger)) }
        }
    }
}

@Composable
private fun AccountSetupHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.double,
                bottom = MainTheme.spacings.triple,
            )
            .requiredHeight(MainTheme.sizes.larger)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        TextHeadline2(text = stringResource(id = R.string.account_setup_title))
    }
}

@Composable
private fun AccountSetupEmailForm(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextFieldOutlined(
            value = "",
            onValueChange = { /*TODO*/ },
            label = "Email address",
        )
    }
}

@Composable
private fun AccountSetupFooter(
    onNextClick: () -> Unit,
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
            text = stringResource(id = R.string.account_setup_button_next),
            onClick = onNextClick,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigContentK9Preview() {
    K9Theme {
        AccountAutoConfigContent({}, {})
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountAutoConfigContent({}, {})
    }
}
