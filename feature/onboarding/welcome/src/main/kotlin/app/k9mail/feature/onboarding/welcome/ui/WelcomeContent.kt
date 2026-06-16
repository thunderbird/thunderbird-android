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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.k9mail.feature.onboarding.welcome.R
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.text.TextDisplayMediumAutoResize
import net.thunderbird.components.ui.bolt.common.window.WindowSizeClass
import net.thunderbird.components.ui.bolt.common.window.WindowWidthSizeClass
import net.thunderbird.components.ui.bolt.common.window.calculateWindowSizeInfo
import net.thunderbird.components.ui.bolt.template.LazyColumnWithHeaderFooter
import net.thunderbird.components.ui.bolt.template.ResponsiveContent
import net.thunderbird.components.ui.bolt.theme.MainTheme
import org.jetbrains.compose.resources.painterResource

private const val CIRCLE_COLOR = 0xFFEEEEEE
private const val CIRCLE_SIZE_DP = 200
private const val CIRCLE_SIZE_SMALL_DP = 125
private const val LOGO_SIZE_DP = 125
private const val LOGO_SIZE_SMALL_DP = 80

@Composable
internal fun WelcomeContent(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    appName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
    ) {
        ResponsiveContent { contentPadding ->
            LazyColumnWithHeaderFooter(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.SpaceEvenly,
                header = {
                    WelcomeHeaderSection(title = appName)
                },
                footer = {
                    WelcomeFooterSection(
                        onStartClick = onStartClick,
                        onImportClick = onImportClick,
                    )
                },
                content = {
                    item { WelcomeMessageItem() }
                },
            )
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
            .defaultItemModifier()
            .padding(top = MainTheme.spacings.quadruple),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeLogo()
        WelcomeTitleItem(title = title)
    }
}

@Composable
private fun WelcomeLogo(
    modifier: Modifier = Modifier,
) {
    val windowSizeInfo = calculateWindowSizeInfo()
    val isSmallScreen =
        windowSizeInfo.sizeClass.widthSizeClass == WindowWidthSizeClass.Small
    val circleSize = if (isSmallScreen) {
        CIRCLE_SIZE_SMALL_DP
    } else {
        CIRCLE_SIZE_DP
    }
    val logoSize = if (isSmallScreen) {
        LOGO_SIZE_SMALL_DP
    } else {
        LOGO_SIZE_DP
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(CIRCLE_COLOR))
                .size(circleSize.dp),
        ) {
            Image(
                painter = painterResource(MainTheme.images.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(logoSize.dp)
                    .align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun WelcomeTitleItem(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        WelcomeTitle(
            title = title,
            modifier = Modifier.defaultItemModifier(),
        )
    }
}

@Composable
private fun WelcomeTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = MainTheme.spacings.quadruple),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextDisplayMediumAutoResize(
            text = title,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomeMessageItem(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        WelcomeMessage(
            modifier = Modifier.defaultItemModifier(),
        )
    }
}

@Composable
private fun WelcomeMessage(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = MainTheme.spacings.quadruple)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextBodyLarge(
            text = stringResource(id = R.string.onboarding_welcome_text),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomeFooterSection(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MainTheme.spacings.quadruple),
    ) {
        WelcomeFooter(
            onStartClick = onStartClick,
            onImportClick = onImportClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MainTheme.spacings.quadruple),
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
            modifier = Modifier.testTag("onboarding_welcome_start_button"),
        )
        ButtonText(
            text = stringResource(id = R.string.onboarding_welcome_import_button),
            onClick = onImportClick,
        )

        TextBodySmall(
            text = stringResource(R.string.onboarding_welcome_developed_by),
            modifier = Modifier
                .padding(top = MainTheme.spacings.quadruple)
                .padding(horizontal = MainTheme.spacings.double),
        )
    }
}

@Composable
private fun Modifier.defaultItemModifier() = this
    .fillMaxWidth()
    .padding(MainTheme.spacings.default)
