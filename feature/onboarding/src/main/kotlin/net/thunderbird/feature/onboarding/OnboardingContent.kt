package net.thunderbird.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline2
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
internal fun OnboardingContent(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Background(
        modifier = modifier,
    ) {
        ResponsiveContent {
            LazyColumnWithFooter(
                modifier = Modifier.fillMaxSize(),
                footer = {
                    WelcomeFooter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MainTheme.spacings.quadruple),
                        onStartClick = onStartClick,
                        onImportClick = onImportClick,
                    )
                },
                verticalArrangement = Arrangement.spacedBy(MainTheme.sizes.medium, Alignment.CenterVertically),
            ) {
                item {
                    WelcomeLogo(
                        modifier = Modifier
                            .defaultItemModifier()
                            .padding(top = MainTheme.spacings.quadruple),
                    )
                }
                item {
                    WelcomeTitle(
                        modifier = Modifier.defaultItemModifier(),
                    )
                }
                item {
                    WelcomeMessage(
                        modifier = Modifier.defaultItemModifier(),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeLogo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.onboarding_welcome_logo),
            contentDescription = null,
        )
    }
}

@Composable
private fun WelcomeTitle(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextHeadline2(
            text = stringResource(id = R.string.onboarding_welcome_title),
        )
    }
}

@Composable
private fun WelcomeMessage(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(start = MainTheme.spacings.quadruple, end = MainTheme.spacings.quadruple)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextBody1(
            text = stringResource(id = R.string.onboarding_welcome_message),
        )
    }
}

@Composable
private fun WelcomeFooter(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            text = stringResource(id = R.string.onboarding_welcome_start_button),
            onClick = onStartClick,
        )
        ButtonText(
            text = stringResource(id = R.string.onboarding_welcome_import_button),
            onClick = onImportClick,
        )
    }
}

private fun Modifier.defaultItemModifier() = composed {
    fillMaxWidth()
        .padding(MainTheme.spacings.double)
}

@Composable
@DevicePreviews
internal fun OnboardingContentK9Preview() {
    K9Theme {
        OnboardingContent(
            onStartClick = {},
            onImportClick = {},
        )
    }
}

@Composable
@DevicePreviews
internal fun OnboardingContentThunderbirdPreview() {
    ThunderbirdTheme {
        OnboardingContent(
            onStartClick = {},
            onImportClick = {},
        )
    }
}
