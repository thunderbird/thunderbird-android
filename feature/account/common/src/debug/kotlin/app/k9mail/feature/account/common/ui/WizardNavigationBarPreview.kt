package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@PreviewDevices
internal fun WizardNavigationBarPreview() {
    PreviewWithThemes {
        WizardNavigationBar(
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@Composable
@PreviewDevices
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

@Composable
@PreviewDevices
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

@Composable
@PreviewDevices
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
