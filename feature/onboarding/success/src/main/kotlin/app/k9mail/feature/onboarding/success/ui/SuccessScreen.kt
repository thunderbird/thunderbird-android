package app.k9mail.feature.onboarding.success.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
fun SuccessScreen(
    onGoToInboxClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Background(
        modifier = modifier,
    ) {
        ResponsiveContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                SuccessLogo(
                    modifier = Modifier
                        .defaultItemModifier(),
                )
                SuccessTitle(
                    modifier = Modifier.defaultItemModifier(),
                )
                Spacer(modifier = Modifier.height(MainTheme.spacings.double))
                SuccessMessage(
                    modifier = Modifier.defaultItemModifier(),
                )
                Spacer(modifier = Modifier.weight(1f))
                SuccessFooter(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MainTheme.spacings.quadruple),
                    onGoToInboxClick = onGoToInboxClick,
                )
            }
        }
    }
}

@Composable
@PreviewDevices
internal fun SuccessScreenK9Preview() {
    K9Theme {
        SuccessScreen(
            onGoToInboxClick = {},
        )
    }
}

@Composable
@PreviewDevices
internal fun SuccessScreenThunderbirdPreview() {
    ThunderbirdTheme {
        SuccessScreen(
            onGoToInboxClick = {},
        )
    }
}
