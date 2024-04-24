package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.feature.account.common.domain.input.BooleanInputField

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryResultApprovalViewPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultApprovalView(
            approvalState = BooleanInputField(
                value = true,
                isValid = true,
            ),
            onApprovalChange = {},
        )
    }
}
