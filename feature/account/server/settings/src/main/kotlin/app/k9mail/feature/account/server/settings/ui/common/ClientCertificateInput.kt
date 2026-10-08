package app.k9mail.feature.account.server.settings.ui.common

import android.app.Activity
import android.security.KeyChain
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.account.server.settings.R
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedFakeSelect
import net.thunderbird.components.ui.bolt.molecule.input.inputContentPadding

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
        val activity = LocalActivity.current as Activity
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
