package app.k9mail.feature.account.setup.ui.autodiscovery.item

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.feature.account.common.domain.input.BooleanInputField
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.setup.ui.autodiscovery.view.AutoDiscoveryResultApprovalView

@Composable
internal fun LazyItemScope.AutoDiscoveryResultApprovalItem(
    approvalState: BooleanInputField,
    onApprovalChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
    ) {
        AutoDiscoveryResultApprovalView(
            approvalState = approvalState,
            onApprovalChange = onApprovalChange,
        )
    }
}
