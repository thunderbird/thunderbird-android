package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import app.k9mail.feature.account.common.R

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
                )
            }
            if (state.showNext) {
                ButtonFilled(
                    text = nextButtonText,
                    onClick = onNextClick,
                    enabled = state.isNextEnabled,
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

@PreviewDevices
@Composable
internal fun WizardNavigationBarK9Preview() {
    K9Theme {
        WizardNavigationBar(
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewDevices
@Composable
internal fun WizardNavigationBarPreview() {
    PreviewWithThemes {
        WizardNavigationBar(
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewDevices
@Composable
internal fun WizardNavigationBarDisabledPreview() {
    PreviewWithThemes {
        WizardNavigationBar(
            onNextClick = {},
            onBackClick = {},
            state = WizardNavigationBarState(
                isNextEnabled = false,
                isBackEnabled = false,
            ),
        )
    }
}

@PreviewDevices
@Composable
internal fun WizardNavigationBarHideNextPreview() {
    PreviewWithThemes {
        WizardNavigationBar(
            onNextClick = {},
            onBackClick = {},
            state = WizardNavigationBarState(
                showNext = false,
            ),
        )
    }
}

@PreviewDevices
@Composable
internal fun WizardNavigationBarHideBackPreview() {
    PreviewWithThemes {
        WizardNavigationBar(
            onNextClick = {},
            onBackClick = {},
            state = WizardNavigationBarState(
                showBack = false,
            ),
        )
    }
}
