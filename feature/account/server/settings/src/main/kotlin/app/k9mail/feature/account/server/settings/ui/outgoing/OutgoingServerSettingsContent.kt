package app.k9mail.feature.account.server.settings.ui.outgoing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.outgoing.content.outgoingFormItems

@Composable
internal fun OutgoingServerSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("OutgoingServerSettingsContent")
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            outgoingFormItems(
                state = state,
                onEvent = onEvent,
                resources = resources,
            )
        }
    }
}

@Composable
@DevicePreviews
internal fun OutgoingServerSettingsContentK9Preview() {
    K9Theme {
        OutgoingServerSettingsContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@DevicePreviews
internal fun OutgoingServerSettingsContentThunderbirdPreview() {
    ThunderbirdTheme {
        OutgoingServerSettingsContent(
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}
