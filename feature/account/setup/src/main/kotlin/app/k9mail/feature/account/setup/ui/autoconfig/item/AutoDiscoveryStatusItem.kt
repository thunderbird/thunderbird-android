package app.k9mail.feature.account.setup.ui.autoconfig.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.feature.account.setup.ui.autoconfig.view.AutoDiscoveryStatusView
import app.k9mail.feature.account.setup.ui.common.item.ListItem

@Composable
fun LazyItemScope.AutoDiscoveryStatusItem(
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
