package app.k9mail.feature.onboarding.success.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline4
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.feature.onboarding.success.R

@Composable
internal fun SuccessLogo(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .then(modifier),
    ) {
        Column(
            modifier = Modifier.then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.onboarding_success_logo),
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun SuccessTitle(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextHeadline4(
            text = stringResource(id = R.string.onboarding_success_title),
        )
    }
}

@Composable
internal fun SuccessMessage(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(start = MainTheme.spacings.quadruple, end = MainTheme.spacings.quadruple)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextBody1(
            text = stringResource(id = R.string.onboarding_success_message),
        )
    }
}

@Composable
internal fun SuccessFooter(
    onGoToInboxClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = MainTheme.spacings.double),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quarter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            text = stringResource(id = R.string.onboarding_success_go_to_inbox_button),
            onClick = onGoToInboxClick,
        )
    }
}

internal fun Modifier.defaultItemModifier() = composed {
    fillMaxWidth()
        .padding(MainTheme.spacings.default)
}
