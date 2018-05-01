package com.fsck.k9.ui.endtoend

import android.app.PendingIntent
import android.content.Intent
import android.content.res.Resources
import com.fsck.k9.Account
import com.fsck.k9.autocrypt.AutocryptTransferMessageUtil
import com.fsck.k9.helper.SingleLiveEvent
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.openintents.openpgp.util.OpenPgpApi
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AutocryptSetupMessageLiveEvent : SingleLiveEvent<AutocryptSetupMessage>() {
    fun loadAutocryptSetupMessageAsync(resources: Resources, openPgpApi: OpenPgpApi, account: Account) {
        launch(UI) {
            val setupMessage = bg {
                loadAutocryptSetupMessage(resources, openPgpApi, account)
            }

            value = setupMessage.await()
        }
    }

    private fun loadAutocryptSetupMessage(resources: Resources, openPgpApi: OpenPgpApi, account: Account): AutocryptSetupMessage {
        val keyIds = longArrayOf(account.openPgpKey)
        val address = Address.parse(account.getIdentity(0).email)[0]

        val intent = Intent(OpenPgpApi.ACTION_AUTOCRYPT_KEY_TRANSFER)
        intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, keyIds)
        val baos = ByteArrayOutputStream()
        val result = openPgpApi.executeApi(intent, null as InputStream?, baos)

        val keyData = baos.toByteArray()
        val pi: PendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)

        val setupMessage = AutocryptTransferMessageUtil.createAutocryptTransferMessage(resources, keyData, address)

        return AutocryptSetupMessage(setupMessage, pi)
    }
}

data class AutocryptSetupMessage(val setupMessage: Message, val showTransferCodePi: PendingIntent)
