package com.fsck.k9.ui.endtoend


import android.app.PendingIntent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.openintents.openpgp.util.OpenPgpApi
import timber.log.Timber


class AutocryptKeyTransferPresenter internal constructor(
        lifecycleOwner: LifecycleOwner,
        private val openPgpApi: OpenPgpApi,
        private val preferences: Preferences,
        private val viewModel: AutocryptKeyTransferViewModel,
        private val view: AutocryptKeyTransferActivity
) {

    private lateinit var account: Account
    private lateinit var showTransferCodePi: PendingIntent

    init {
        viewModel.autocryptSetupMessageLiveEvent.observe(lifecycleOwner, Observer { msg -> msg?.let { onEventAutocryptSetupMessage(it) } })
        viewModel.autocryptSetupTransferLiveEvent.observe(lifecycleOwner, Observer { pi -> onLoadedAutocryptSetupTransfer(pi) })
    }

    fun initFromIntent(accountUuid: String?) {
        if (accountUuid == null) {
            view.finishWithInvalidAccountError()
            return
        }

        account = preferences.getAccount(accountUuid)

        view.setAddress(account.email)

        viewModel.autocryptSetupTransferLiveEvent.recall()
    }

    fun onClickHome() {
        view.finishAsCancelled()
    }

    fun onClickTransferSend() {
        view.sceneGeneratingAndSending()

        GlobalScope.launch(Dispatchers.Main) {
            view.uxDelay()
            view.setLoadingStateGenerating()

            viewModel.autocryptSetupMessageLiveEvent.loadAutocryptSetupMessageAsync(openPgpApi, account)
        }
    }

    fun onClickShowTransferCode() {
        view.launchUserInteractionPendingIntent(showTransferCodePi)
    }

    private fun onEventAutocryptSetupMessage(setupMsg: AutocryptSetupMessage) {
        view.setLoadingStateSending()
        view.sceneGeneratingAndSending()

        viewModel.autocryptSetupTransferLiveEvent.sendMessageAsync(account, setupMsg)
    }

    private fun onLoadedAutocryptSetupTransfer(result: AutocryptSetupTransferResult?) {
        when (result) {
            null -> view.sceneBegin()
            is AutocryptSetupTransferResult.Success -> {
                showTransferCodePi = result.showTransferCodePi
                view.setLoadingStateFinished()
                view.sceneFinished()
            }
            is AutocryptSetupTransferResult.Failure -> {
                Timber.e(result.exception, "Error sending setup message")
                view.setLoadingStateSendingFailed()
                view.sceneSendError()
            }
        }
    }
}
