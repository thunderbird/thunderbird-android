package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import app.k9mail.feature.account.common.domain.input.BooleanInputField
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autodiscovery.toResourceString

@Composable
internal fun AutoDiscoveryResultApprovalView(
    approvalState: BooleanInputField,
    onApprovalChange: (Boolean) -> Unit,
) {
    val resources = LocalContext.current.resources

    Spacer(modifier = Modifier.height(MainTheme.spacings.default))

    CheckboxInput(
        text = stringResource(
            id = R.string.account_setup_auto_discovery_result_approval_checkbox_label,
        ),
        checked = approvalState.value ?: false,
        onCheckedChange = onApprovalChange,
        errorMessage = approvalState.error?.toResourceString(resources),
        contentPadding = PaddingValues(),
    )
}

@Preview(showBackground = true)
@Composable
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
