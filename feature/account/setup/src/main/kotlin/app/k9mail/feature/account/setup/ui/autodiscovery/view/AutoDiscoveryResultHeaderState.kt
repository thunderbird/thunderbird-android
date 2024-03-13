package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.feature.account.setup.R

enum class AutoDiscoveryResultHeaderState(
    val icon: ImageVector,
    @StringRes val titleResourceId: Int,
    @StringRes val subtitleResourceId: Int,
    val isExpandable: Boolean,
) {
    NoSettings(
        icon = Icons.Outlined.info,
        titleResourceId = R.string.account_setup_auto_discovery_result_header_title_configuration_not_found,
        subtitleResourceId = R.string.account_setup_auto_discovery_result_header_subtitle_configuration_not_found,
        isExpandable = false,
    ),

    Trusted(
        icon = Icons.Outlined.check,
        titleResourceId = R.string.account_setup_auto_discovery_status_header_title_configuration_found,
        subtitleResourceId = R.string.account_setup_auto_discovery_result_header_subtitle_configuration_trusted,
        isExpandable = true,
    ),

    Untrusted(
        icon = Icons.Outlined.info,
        titleResourceId = R.string.account_setup_auto_discovery_status_header_title_configuration_found,
        subtitleResourceId = R.string.account_setup_auto_discovery_result_header_subtitle_configuration_untrusted,
        isExpandable = true,
    ),
}
