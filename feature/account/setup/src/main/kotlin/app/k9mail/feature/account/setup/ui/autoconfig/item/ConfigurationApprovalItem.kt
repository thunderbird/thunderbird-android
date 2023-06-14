package app.k9mail.feature.account.setup.ui.autoconfig.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.feature.account.setup.domain.input.BooleanInputField
import app.k9mail.feature.account.setup.ui.autoconfig.view.ConfigurationApprovalView
import app.k9mail.feature.account.setup.ui.common.item.ListItem

@Composable
fun LazyItemScope.ConfigurationApprovalItem(
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
