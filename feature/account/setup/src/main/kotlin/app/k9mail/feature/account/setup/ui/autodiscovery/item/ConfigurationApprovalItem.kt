package app.k9mail.feature.account.setup.ui.autodiscovery.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.setup.domain.input.BooleanInputField
import app.k9mail.feature.account.setup.ui.autodiscovery.view.ConfigurationApprovalView

@Composable
internal fun LazyItemScope.ConfigurationApprovalItem(
    approvalState: BooleanInputField,
    onConfigurationApprovalChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
    ) {
        ConfigurationApprovalView(
            approvalState = approvalState,
            onConfigurationApprovalChange = onConfigurationApprovalChange,
        )
    }
}
