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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMedium
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithHeaderFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.onboarding.welcome.R


private const val CIRCLE_COLOR = 0xFFEEEEEE
private const val CIRCLE_SIZE_DP = 200
private const val LOGO_SIZE_DP = 125


@Composable
internal fun WelcomeContent(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    appName: String,
    showImportButton: Boolean,
    modifier: Modifier = Modifier,
) {

    Surface(modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            ResponsiveContent {
                LazyColumnWithHeaderFooter(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    header = {
                        WelcomeHeaderSection()
                    },
                    footer = {
                        WelcomeFooterSection(
                            showImportButton = showImportButton,
                            onStartClick = onStartClick,
                            onImportClick = onImportClick,
                        )
                    },
                    content = {
                        item {WelcomeTitleItem(title = appName)}
                        item {WelcomeMessageItem()}
                    },
                )
            }



        }
    }
}

@Composable
private fun WelcomeHeaderSection(

){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultItemModifier()
            .padding(top = MainTheme.spacings.double),
            contentAlignment = Alignment.Center,
    ) {
        WelcomeLogo()
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
private fun WelcomeTitleItem(
    title: String,
) {
    Box(
        modifier = Modifier
            .defaultItemModifier()
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
        TextDisplayMedium(
            text = title,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomeMessageItem(
){
    Box(
        modifier = Modifier
            .defaultItemModifier()
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
    showImportButton: Boolean,
    onStartClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MainTheme.spacings.quadruple)
    ) {
        WelcomeFooter(
            showImportButton = showImportButton,
            onStartClick = onStartClick,
            onImportClick = onImportClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MainTheme.spacings.quadruple)
        )
    }
}

@Composable
private fun WelcomeFooter(
    showImportButton: Boolean,
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
        if (showImportButton) {
            ButtonText(
                text = stringResource(id = R.string.onboarding_welcome_import_button),
                onClick = onImportClick,
            )
        }

        TextBodySmall(
            text = stringResource(R.string.onboarding_welcome_developed_by),
            modifier = Modifier
                .padding(top = MainTheme.spacings.quadruple)
                .padding(horizontal = MainTheme.spacings.double),
        )
    }
}

private fun Modifier.defaultItemModifier() = composed {
    fillMaxWidth()
        .padding(MainTheme.spacings.default)
}
