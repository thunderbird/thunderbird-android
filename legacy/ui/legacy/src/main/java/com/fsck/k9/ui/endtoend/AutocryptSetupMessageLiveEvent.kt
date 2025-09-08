package com.fsck.k9.ui.endtoend

import android.app.PendingIntent
import android.content.Intent
import androidx.core.content.IntentCompat
import com.fsck.k9.autocrypt.AutocryptTransferMessageCreator
import com.fsck.k9.helper.SingleLiveEvent
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDto
import org.openintents.openpgp.util.OpenPgpApi

class AutocryptSetupMessageLiveEvent(
    val messageCreator: AutocryptTransferMessageCreator,
    private val eventScope: CoroutineScope = MainScope(),
) : SingleLiveEvent<AutocryptSetupMessage>() {

    fun loadAutocryptSetupMessageAsync(openPgpApi: OpenPgpApi, account: LegacyAccountDto) {
        eventScope.launch {
            val result = withContext(Dispatchers.IO) {
                loadAutocryptSetupMessage(openPgpApi, account)
            }

            value = result
        }
    }

    private fun loadAutocryptSetupMessage(openPgpApi: OpenPgpApi, account: LegacyAccountDto): AutocryptSetupMessage {
        val keyIds = longArrayOf(account.openPgpKey)
        val address = Address.parse(account.getIdentity(0).email)[0]

        val intent = Intent(OpenPgpApi.ACTION_AUTOCRYPT_KEY_TRANSFER)
        intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, keyIds)
        val baos = ByteArrayOutputStream()
        val result = openPgpApi.executeApi(intent, null as InputStream?, baos)

        val keyData = baos.toByteArray()
        val pi: PendingIntent = IntentCompat.getParcelableExtra(
            result,
            OpenPgpApi.RESULT_INTENT,
            PendingIntent::class.java,
        ) ?: error("Missing result intent")

        val setupMessage = messageCreator.createAutocryptTransferMessage(keyData, address)

        return AutocryptSetupMessage(setupMessage, pi)
    }
}

data class AutocryptSetupMessage(val setupMessage: Message, val showTransferCodePi: PendingIntent)
