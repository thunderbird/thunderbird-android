package com.fsck.k9.ui.endtoend

import android.app.PendingIntent
import com.fsck.k9.Account
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.SingleLiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutocryptSetupTransferLiveEvent(
    private val messagingController: MessagingController,
    private val eventScope: CoroutineScope = MainScope(),
) : SingleLiveEvent<AutocryptSetupTransferResult>() {

    fun sendMessageAsync(account: Account, setupMsg: AutocryptSetupMessage) {
        eventScope.launch {
            val setupMessage = async(Dispatchers.IO) {
                messagingController.sendMessageBlocking(account, setupMsg.setupMessage)
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
