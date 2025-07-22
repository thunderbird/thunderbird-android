package app.k9mail.feature.migration.qrcode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.migration.qrcode.R
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId

@Composable
internal fun PermissionDeniedContent(
    onGoToSettingsClick: () -> Unit,
) {
    ResponsiveContent(
        modifier = Modifier.testTagAsResourceId("PermissionDeniedContent"),
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(MainTheme.spacings.double)
                .padding(contentPadding),
        ) {
            TextTitleLarge(text = stringResource(R.string.migration_qrcode_permission_denied_title))
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))

            TextBodyLarge(text = stringResource(R.string.migration_qrcode_permission_denied_message))
            Spacer(modifier = Modifier.height(MainTheme.spacings.triple))

            ButtonFilled(
                text = stringResource(R.string.migration_qrcode_go_to_settings_button_text),
                onClick = onGoToSettingsClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTagAsResourceId("GoToSettingsButton"),
            )
        }
    }
}
