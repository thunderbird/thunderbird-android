package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline2
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithHeaderFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.onboarding.welcome.R

private const val CIRCLE_COLOR = 0xFFEEEEEE
private const val CIRCLE_SIZE_DP = 300
private const val LOGO_SIZE_DP = 200

@Composable
internal fun WelcomeContent(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Background(
        modifier = modifier,
    ) {
        ResponsiveContent {
            LazyColumnWithHeaderFooter(
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
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                item {
                    WelcomeLogo(
                        modifier = Modifier
                            .defaultItemModifier()
                            .padding(top = MainTheme.spacings.double),
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(CIRCLE_COLOR))
                .size(CIRCLE_SIZE_DP.dp),
        ) {
            Image(
                painter = painterResource(id = MainTheme.images.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(LOGO_SIZE_DP.dp)
                    .align(Alignment.Center),
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
        modifier = modifier.padding(bottom = MainTheme.spacings.double),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quarter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ButtonFilled(
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
        .padding(MainTheme.spacings.default)
}

@Composable
@PreviewDevices
internal fun WelcomeContentK9Preview() {
    K9Theme {
        WelcomeContent(
            onStartClick = {},
            onImportClick = {},
        )
    }
}

@Composable
@PreviewDevices
internal fun WelcomeContentThunderbirdPreview() {
    ThunderbirdTheme {
        WelcomeContent(
            onStartClick = {},
            onImportClick = {},
        )
    }
}
