package com.fsck.k9.ui.endtoend

import android.app.PendingIntent
import android.content.Intent
import com.fsck.k9.Account
import com.fsck.k9.autocrypt.AutocryptTransferMessageCreator
import com.fsck.k9.helper.SingleLiveEvent
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openintents.openpgp.util.OpenPgpApi

class AutocryptSetupMessageLiveEvent(val messageCreator: AutocryptTransferMessageCreator) : SingleLiveEvent<AutocryptSetupMessage>() {
    fun loadAutocryptSetupMessageAsync(openPgpApi: OpenPgpApi, account: Account) {
        GlobalScope.launch(Dispatchers.Main) {
            value = withContext(Dispatchers.IO) {
                loadAutocryptSetupMessage(openPgpApi, account)
            }
        }
    }

    private fun loadAutocryptSetupMessage(openPgpApi: OpenPgpApi, account: Account): AutocryptSetupMessage {
        val keyIds = longArrayOf(account.openPgpKey)
        val address = Address.parse(account.getIdentity(0).email)[0]

        val intent = Intent(OpenPgpApi.ACTION_AUTOCRYPT_KEY_TRANSFER)
        intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, keyIds)
        val baos = ByteArrayOutputStream()
        val result = openPgpApi.executeApi(intent, null as InputStream?, baos)

        val keyData = baos.toByteArray()
        val pi: PendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT) ?: error("Missing result intent")

        val setupMessage = messageCreator.createAutocryptTransferMessage(keyData, address)

        return AutocryptSetupMessage(setupMessage, pi)
    }
}

data class AutocryptSetupMessage(val setupMessage: Message, val showTransferCodePi: PendingIntent)
