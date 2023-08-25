package app.k9mail.feature.account.setup.ui.autodiscovery.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.setup.ui.autodiscovery.view.AutoDiscoveryStatusView

@Composable
internal fun LazyItemScope.AutoDiscoveryStatusItem(
    autoDiscoverySettings: AutoDiscoveryResult.Settings?,
    onEditConfigurationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
    ) {
        AutoDiscoveryStatusView(
            settings = autoDiscoverySettings,
            onEditConfigurationClick = onEditConfigurationClick,
        )
    }
}
