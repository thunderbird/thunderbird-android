package com.fsck.k9.ui.endtoend

import android.app.PendingIntent
import androidx.lifecycle.LifecycleOwner
import app.k9mail.legacy.account.Account
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderError
import timber.log.Timber

class AutocryptKeyTransferPresenter internal constructor(
    lifecycleOwner: LifecycleOwner,
    private val openPgpApiManager: OpenPgpApiManager,
    private val preferences: Preferences,
    private val viewModel: AutocryptKeyTransferViewModel,
    private val view: AutocryptKeyTransferActivity,
    private val presenterScope: CoroutineScope = MainScope(),
) {

    private lateinit var account: Account
    private lateinit var showTransferCodePi: PendingIntent

    init {
        viewModel.autocryptSetupMessageLiveEvent.observe(lifecycleOwner) { msg ->
            msg?.let { onEventAutocryptSetupMessage(it) }
        }
        viewModel.autocryptSetupTransferLiveEvent.observe(lifecycleOwner) { pi ->
            onLoadedAutocryptSetupTransfer(pi)
        }
    }

    fun initFromIntent(accountUuid: String?) {
        if (accountUuid == null) {
            view.finishWithInvalidAccountError()
            return
        }

        account = preferences.getAccount(accountUuid) ?: error("Account $accountUuid not found")

        openPgpApiManager.setOpenPgpProvider(
            account.openPgpProvider,
            object : OpenPgpApiManagerCallback {
                override fun onOpenPgpProviderStatusChanged() {
                    if (openPgpApiManager.openPgpProviderState == OpenPgpApiManager.OpenPgpProviderState.UI_REQUIRED) {
                        view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
                    }
                }

                override fun onOpenPgpProviderError(error: OpenPgpProviderError) {
                    view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
                }
            },
        )

        view.setAddress(account.identities[0].email!!)

        viewModel.autocryptSetupTransferLiveEvent.recall()
    }

    fun onClickHome() {
        view.finishAsCancelled()
    }

    fun onClickTransferSend() {
        view.sceneGeneratingAndSending()

        presenterScope.launch {
            view.uxDelay()
            view.setLoadingStateGenerating()

            viewModel.autocryptSetupMessageLiveEvent.loadAutocryptSetupMessageAsync(
                openPgpApiManager.openPgpApi,
                account,
            )
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
