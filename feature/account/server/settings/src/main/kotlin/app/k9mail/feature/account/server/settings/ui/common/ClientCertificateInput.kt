package app.k9mail.feature.account.server.settings.ui.common

import android.security.KeyChain
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.activity.LocalActivity
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedFakeSelect
import app.k9mail.core.ui.compose.designsystem.molecule.input.inputContentPadding
import app.k9mail.feature.account.server.settings.R

@Composable
fun ClientCertificateInput(
    alias: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        val activity = LocalActivity.current
        TextFieldOutlinedFakeSelect(
            text = alias ?: stringResource(R.string.account_server_settings_client_certificate_none_selected),
            onClick = {
                KeyChain.choosePrivateKeyAlias(activity, onValueChange, null, null, null, -1, alias)
            },
            modifier = Modifier.fillMaxWidth(),
            label = label,
        )
    }
}
