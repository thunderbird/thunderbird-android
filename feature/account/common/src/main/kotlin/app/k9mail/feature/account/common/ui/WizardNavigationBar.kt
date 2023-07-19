package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun WizardNavigationBar(
    nextButtonText: String,
    backButtonText: String,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ButtonOutlined(
                text = backButtonText,
                onClick = onBackClick,
                enabled = state.isBackEnabled,
            )
            Button(
                text = nextButtonText,
                onClick = onNextClick,
                enabled = state.isNextEnabled,
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun WizardNavigationBarK9Preview() {
    K9Theme {
        WizardNavigationBar(
            nextButtonText = "Next",
            backButtonText = "Back",
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@DevicePreviews
@Composable
internal fun WizardNavigationBarPreview() {
    PreviewWithThemes {
        WizardNavigationBar(
            nextButtonText = "Next",
            backButtonText = "Back",
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@DevicePreviews
@Composable
internal fun WizardNavigationBarDisabledPreview() {
    PreviewWithThemes {
        WizardNavigationBar(
            nextButtonText = "Next",
            backButtonText = "Back",
            onNextClick = {},
            onBackClick = {},
            state = WizardNavigationBarState(
                isNextEnabled = false,
                isBackEnabled = false,
            ),
        )
    }
}
