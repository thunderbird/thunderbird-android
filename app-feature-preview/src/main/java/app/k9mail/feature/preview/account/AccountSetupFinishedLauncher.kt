package app.k9mail.feature.preview.account

import android.content.Context
import android.widget.Toast
import app.k9mail.feature.account.setup.AccountSetupExternalContract

class AccountSetupFinishedLauncher(
    private val context: Context,
) : AccountSetupExternalContract.AccountSetupFinishedLauncher {
    override suspend fun launch(accountUuid: String) {
        Toast.makeText(context, "AccountSetupFinishedLauncher.launch($accountUuid)", Toast.LENGTH_SHORT).show()
    }
}
