package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autodiscovery.toAutoDiscoveryValidationErrorString
import net.thunderbird.core.validation.input.BooleanInputField

@Composable
internal fun AutoDiscoveryResultApprovalView(
    approvalState: BooleanInputField,
    onApprovalChange: (Boolean) -> Unit,
) {
    val resources = LocalResources.current

    Spacer(modifier = Modifier.height(MainTheme.spacings.default))

    CheckboxInput(
        text = stringResource(
            id = R.string.account_setup_auto_discovery_result_approval_checkbox_label,
        ),
        checked = approvalState.value ?: false,
        onCheckedChange = onApprovalChange,
        errorMessage = approvalState.error?.toAutoDiscoveryValidationErrorString(resources),
        contentPadding = PaddingValues(),
    )
}
