package net.thunderbird.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    ResponsiveContent {
        Background(
            modifier = modifier,
        ) {
            LazyColumnWithFooter(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(MainTheme.spacings.double),
                footer = {
                    WelcomeFooter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MainTheme.spacings.triple),
                        onStartClick = onStartClick,
                        onImportClick = onImportClick,
                    )
                },
                verticalArrangement = Arrangement.spacedBy(MainTheme.sizes.large, Alignment.CenterVertically),
            ) {
                item {
                    WelcomeLogo(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    WelcomeTitle(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    WelcomeMessage(
                        modifier = Modifier.fillMaxWidth(),
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
        Box(
            modifier = Modifier
                .size(MainTheme.sizes.huge)
                .clip(shape = RoundedCornerShape(percent = 50))
                .background(color = MainTheme.colors.surface),
        ) {
            Image(
                painter = painterResource(id = MainTheme.images.logo),
                contentDescription = null,
                modifier = Modifier.size(MainTheme.sizes.huge),
            )
        }
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
