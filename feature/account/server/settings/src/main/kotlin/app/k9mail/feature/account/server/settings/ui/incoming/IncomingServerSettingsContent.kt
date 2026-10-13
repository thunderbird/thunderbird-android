package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.incoming.content.incomingFormItems
import net.thunderbird.components.ui.bolt.template.ResponsiveWidthContainer
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.common.mail.ZohoMailServerSettings

@Composable
internal fun IncomingServerSettingsContent(
    mode: InteractionMode,
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val packageName = LocalContext.current.packageName

    // Do not key this effect on the field value: that would overwrite each URI edit while the user types.
    LaunchedEffect(state.protocolType, packageName) {
        val currentRedirectUri = state.zohoRedirectUri.value
        val defaultRedirectUri = "$packageName:${ZohoMailServerSettings.REDIRECT_PATH}"
        val legacyDefaultRedirectUri = "$packageName:${ZohoMailServerSettings.LEGACY_REDIRECT_PATH}"
        if (state.protocolType == IncomingProtocolType.ZOHO_MAIL_API &&
            (currentRedirectUri.isBlank() || currentRedirectUri == legacyDefaultRedirectUri)
        ) {
            onEvent(Event.ZohoRedirectUriChanged(defaultRedirectUri))
        }
    }

    ResponsiveWidthContainer(
        modifier = Modifier.testTag("IncomingServerSettingsContent")
            .then(modifier),
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        ) {
            incomingFormItems(
                mode = mode,
                state = state,
                onEvent = onEvent,
                resources = resources,
                applicationId = packageName,
            )
        }
    }
}
