package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMedium
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.feature.onboarding.welcome.R
import net.thunderbird.core.common.provider.UsingBrandTypography
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.RegisteredTrademarkInjector
import org.jetbrains.compose.resources.painterResource

private const val LOGO_SIZE_DP = 125
private const val LOGO_DESCRIPTION_SPACING = 48

@Composable
internal fun WelcomeContent(
    onStartClick: () -> Unit,
    appName: String,
    modifier: Modifier = Modifier,
) {
    ResponsiveContent(
        modifier = modifier.fillMaxHeight(),
        useSurfaceForExpandedContent = false,
    ) { contentPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Spacer(modifier = Modifier.weight(weight = .5f))
            Column(
                verticalArrangement = Arrangement.spacedBy(LOGO_DESCRIPTION_SPACING.dp),
            ) {
                WelcomeHeaderSection(title = appName)
                TextBodyMedium(
                    text = stringResource(id = R.string.onboarding_welcome_text),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.defaultItemModifier(),
                )
            }
            Spacer(modifier = Modifier.weight(weight = 1f))

            WelcomeActionButtons(
                onStartClick = onStartClick,
                modifier = Modifier
                    .defaultItemModifier()
                    .padding(top = MainTheme.spacings.triple),
            )

            Spacer(modifier = Modifier.weight(weight = .25f))
            TextBodySmall(
                text = stringResource(R.string.onboarding_welcome_developed_by),
                modifier = Modifier.defaultItemModifier(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(weight = .25f))
        }
    }
}

@Composable
private fun WelcomeHeaderSection(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MainTheme.spacings.quadruple),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeLogo()
        WelcomeTitle(
            title = RegisteredTrademarkInjector.inject(title),
        )
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
        Image(
            painter = painterResource(MainTheme.images.logo),
            contentDescription = null,
            modifier = Modifier.size(LOGO_SIZE_DP.dp),
        )
    }
}

@Composable
private fun WelcomeTitle(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = MainTheme.spacings.quadruple),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UsingBrandTypography {
            TextDisplayMedium(
                text = title,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WelcomeActionButtons(
    onStartClick: () -> Unit,
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
            modifier = Modifier.testTagAsResourceId("onboarding_welcome_start_button"),
        )
    }
}

@Composable
private fun Modifier.defaultItemModifier() = this then Modifier
    .fillMaxWidth()
    .padding(horizontal = MainTheme.spacings.quadruple, vertical = MainTheme.spacings.default)
