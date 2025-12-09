package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.incoming.content.incomingFormItems
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId

@Composable
internal fun IncomingServerSettingsContent(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    ResponsiveWidthContainer(
        modifier = Modifier
            .testTagAsResourceId("IncomingServerSettingsContent")
            .then(modifier),
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
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
