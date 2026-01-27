package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.feature.account.setup.R
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Suppress("detekt.UnnecessaryAnnotationUseSiteTarget") // https://github.com/detekt/detekt/issues/8212
enum class AutoDiscoveryResultHeaderState(
    val icon: ImageVector,
    @param:StringRes val titleResourceId: Int,
    @param:StringRes val subtitleResourceId: Int,
    val isExpandable: Boolean,
) {
    NoSettings(
        icon = Icons.Outlined.Info,
        titleResourceId = R.string.account_setup_auto_discovery_result_header_title_configuration_not_found,
        subtitleResourceId = R.string.account_setup_auto_discovery_result_header_subtitle_configuration_not_found,
        isExpandable = false,
    ),

    Trusted(
        icon = Icons.Outlined.Check,
        titleResourceId = R.string.account_setup_auto_discovery_status_header_title_configuration_found,
        subtitleResourceId = R.string.account_setup_auto_discovery_result_header_subtitle_configuration_trusted,
        isExpandable = true,
    ),

    Untrusted(
        icon = Icons.Outlined.Info,
        titleResourceId = R.string.account_setup_auto_discovery_status_header_title_configuration_found,
        subtitleResourceId = R.string.account_setup_auto_discovery_result_header_subtitle_configuration_untrusted,
        isExpandable = true,
    ),
}
