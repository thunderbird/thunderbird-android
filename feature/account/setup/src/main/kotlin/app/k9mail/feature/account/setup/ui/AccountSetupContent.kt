package app.k9mail.feature.account.setup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline2
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R

@Composable
internal fun AccountSetupContent(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResponsiveContent {
        Background(
            modifier = modifier,
        ) {
            LazyColumnWithFooter(
                modifier = Modifier.fillMaxSize(),
                footer = {
                    AccountSetupFooter(
                        onNextClick = onNextClick,
                        onBackClick = onBackClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                },
                verticalArrangement = Arrangement.Center,
            ) {
                item {
                    AccountSetupTitle(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    AccountSetupEmailForm(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSetupTitle(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(all = MainTheme.spacings.double)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextHeadline2(text = stringResource(id = R.string.account_setup_title))
    }
}

@Composable
private fun AccountSetupEmailForm(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(all = MainTheme.spacings.double)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextFieldOutlined(value = "Email address", onValueChange = { /*TODO*/ })
    }
}

@Composable
fun AccountSetupFooter(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .padding(all = MainTheme.spacings.double)
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ButtonOutlined(text = stringResource(id = R.string.account_setup_button_back), onClick = onBackClick)
        Button(text = stringResource(id = R.string.account_setup_button_next), onClick = onNextClick)
    }
}

@Composable
@DevicePreviews
internal fun AccountSetupContentK9Preview() {
    K9Theme {
        AccountSetupContent({}, {})
    }
}

@Composable
@DevicePreviews
internal fun AccountSetupContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountSetupContent({}, {})
    }
}
