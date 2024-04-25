package app.k9mail.feature.account.server.settings.ui.incoming

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
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.loadingerror.rememberContentLoadingErrorViewState
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.incoming.content.incomingFormItems

@Composable
internal fun IncomingServerSettingsContent(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTag("IncomingServerSettingsContent")
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
                incomingFormItems(
                    mode = mode,
                    state = state,
                    onEvent = onEvent,
                    resources = resources,
                )
            }
        }
    }
}
