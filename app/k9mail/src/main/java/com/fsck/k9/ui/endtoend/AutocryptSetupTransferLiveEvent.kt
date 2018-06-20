package com.fsck.k9.ui.endtoend

import android.app.PendingIntent
import com.fsck.k9.helper.SingleLiveEvent
import com.fsck.k9.mail.Transport
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

class AutocryptSetupTransferLiveEvent : SingleLiveEvent<AutocryptSetupTransferResult>() {
    fun sendMessageAsync(transport: Transport, setupMsg: AutocryptSetupMessage) {
        launch(UI) {
            val setupMessage = bg {
                transport.sendMessage(setupMsg.setupMessage)
            }

            delay(2000)

            try {
                setupMessage.await()
                value = AutocryptSetupTransferResult.Success(setupMsg.showTransferCodePi)
            } catch (e: Exception) {
                value = AutocryptSetupTransferResult.Failure(e)
            }
        }
    }
}

sealed class AutocryptSetupTransferResult {
    data class Success(val showTransferCodePi: PendingIntent) : AutocryptSetupTransferResult()
    data class Failure(val exception: Exception) : AutocryptSetupTransferResult()
}
