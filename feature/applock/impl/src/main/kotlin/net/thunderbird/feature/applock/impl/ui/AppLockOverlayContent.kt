package net.thunderbird.feature.applock.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.applock.impl.R

@Composable
internal fun AppLockFailedOverlay(
    errorMessage: String,
    onRetryClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MainTheme.spacings.quadruple),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                modifier = Modifier.size(48.dp),
                tint = MainTheme.colors.error,
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            TextHeadlineSmall(
                text = stringResource(R.string.applock_error_title),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            TextBodyMedium(
                text = errorMessage,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.triple))
            ButtonFilled(
                text = stringResource(R.string.applock_button_unlock),
                onClick = onRetryClick,
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            ButtonOutlined(
                text = stringResource(R.string.applock_button_close_app),
                onClick = onCloseClick,
            )
        }
    }
}

@Composable
internal fun AppLockUnavailableOverlay(
    hintMessage: String,
    actionButtonText: String?,
    onActionClick: (() -> Unit)?,
    onCloseClick: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MainTheme.spacings.quadruple),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                modifier = Modifier.size(48.dp),
                tint = MainTheme.colors.warning,
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            TextHeadlineSmall(
                text = stringResource(R.string.applock_requirements_title),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            TextBodyMedium(
                text = hintMessage,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.triple))
            if (actionButtonText != null && onActionClick != null) {
                ButtonFilled(
                    text = actionButtonText,
                    onClick = onActionClick,
                )
                Spacer(modifier = Modifier.height(MainTheme.spacings.default))
            }
            ButtonOutlined(
                text = stringResource(R.string.applock_button_close_app),
                onClick = onCloseClick,
            )
        }
    }
}
