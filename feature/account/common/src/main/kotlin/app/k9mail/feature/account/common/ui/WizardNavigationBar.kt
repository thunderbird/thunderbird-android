package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.R
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId

@Composable
fun WizardNavigationBar(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    nextButtonText: String = stringResource(id = R.string.account_common_button_next),
    backButtonText: String = stringResource(id = R.string.account_common_button_back),
    state: WizardNavigationBarState = WizardNavigationBarState(),
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = MainTheme.spacings.quadruple,
                    top = MainTheme.spacings.default,
                    end = MainTheme.spacings.quadruple,
                    bottom = MainTheme.spacings.double,
                )
                .fillMaxWidth(),
            horizontalArrangement = getHorizontalArrangement(state),
        ) {
            if (state.showBack) {
                ButtonOutlined(
                    text = backButtonText,
                    onClick = onBackClick,
                    enabled = state.isBackEnabled,
                    modifier = Modifier.testTagAsResourceId("account_setup_back_button"),
                )
            }
            if (state.showNext) {
                ButtonFilled(
                    text = nextButtonText,
                    onClick = onNextClick,
                    enabled = state.isNextEnabled,
                    modifier = Modifier.testTagAsResourceId("account_setup_next_button"),
                )
            }
        }
    }
}

private fun getHorizontalArrangement(state: WizardNavigationBarState): Arrangement.Horizontal {
    return if (state.showNext && state.showBack) {
        Arrangement.SpaceBetween
    } else if (state.showNext) {
        Arrangement.End
    } else {
        Arrangement.Start
    }
}
