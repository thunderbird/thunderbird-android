package app.k9mail.feature.account.setup.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
internal fun AccountOptionsContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("AccountOptionsContent")
            .padding(contentPadding)
            .then(modifier),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            item {
                TextBody1(text = "Options placeholder")
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsContentK9Preview() {
    K9Theme {
        AccountOptionsContent(
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOptionsContentThunderbirdPreview() {
    ThunderbirdTheme {
        AccountOptionsContent(
            contentPadding = PaddingValues(),
        )
    }
}
