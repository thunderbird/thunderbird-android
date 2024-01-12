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
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.loadingerror.rememberContentLoadingErrorViewState
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.outgoing.content.outgoingFormItems

@Composable
internal fun OutgoingServerSettingsContent(
    mode: InteractionMode,
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
        ContentLoadingErrorView(
            state = rememberContentLoadingErrorViewState(state = state),
            loading = { /* no-op */ },
            error = { /* no-op */ },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                outgoingFormItems(
                    mode = mode,
                    state = state,
                    onEvent = onEvent,
                    resources = resources,
                )
            }
        }
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerSettingsContentK9Preview() {
    K9Theme {
        OutgoingServerSettingsContent(
            mode = InteractionMode.Create,
            state = State(),
            onEvent = { },
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerSettingsContentThunderbirdPreview() {
    ThunderbirdTheme {
        OutgoingServerSettingsContent(
            mode = InteractionMode.Create,
            onEvent = { },
            state = State(),
            contentPadding = PaddingValues(),
        )
    }
}
