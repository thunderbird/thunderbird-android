package com.fsck.k9.ui.endtoend


import android.app.PendingIntent
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.SystemClock
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.TransportProvider
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferLiveData.AutocryptSetupMessage
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderError
import timber.log.Timber


class AutocryptKeyTransferPresenter internal constructor(
        private val context: Context, lifecycleOwner: LifecycleOwner, private val view: AutocryptKeyTransferView,
        private val viewModel: AutocryptKeyTransferViewModel, private val openPgpApiManager: OpenPgpApiManager,
        private val transportProvider: TransportProvider) {

    private lateinit var account: Account
    private var showTransferCodePi: PendingIntent? = null

    init {
        viewModel.autocryptKeyTransferLiveData.observe(lifecycleOwner, Observer { msg -> onLoadedAutocryptSetupMessage(msg) })
    }

    fun initFromIntent(accountUuid: String?) {
        if (accountUuid == null) {
            view.finishWithInvalidAccountError()
            return
        }

        account = Preferences.getPreferences(context).getAccount(accountUuid)

        openPgpApiManager.setOpenPgpProvider(account.openPgpProvider, object : OpenPgpApiManagerCallback {
            override fun onOpenPgpProviderStatusChanged() {
                if (openPgpApiManager.openPgpProviderState == OpenPgpApiManager.OpenPgpProviderState.UI_REQUIRED) {
                    view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
                }
            }

            override fun onOpenPgpProviderError(error: OpenPgpProviderError) {
                view.finishWithProviderConnectError(openPgpApiManager.readableOpenPgpProviderName)
            }
        })

        view.setAddress(account.identities[0].email)
        view.sceneBegin()
    }

    fun onClickTransferSend() {
        view.sceneGeneratingAndSending()

        launch(UI) {
            delay(1200) // ux delay, to give the scene transition some breathing room
            view.setLoadingStateGenerating()

            viewModel.autocryptKeyTransferLiveData.loadAutocryptSetupMessageAsync(
                    this@AutocryptKeyTransferPresenter.context.resources, openPgpApiManager.openPgpApi, account)
        }
    }

    fun onClickShowTransferCode() {
        if (showTransferCodePi == null) {
            return
        }

        view.launchUserInteractionPendingIntent(showTransferCodePi!!)
    }

    private fun onLoadedAutocryptSetupMessage(setupMsg: AutocryptSetupMessage?) {
        if (setupMsg == null) {
            return
        }

        this.showTransferCodePi = setupMsg.showTransferCodePi
        view.setLoadingStateSending()

        val transport = transportProvider.getTransport(context, account)
        launch(UI) {
            val startTime = SystemClock.elapsedRealtime()

            val msg = bg {
                transport.sendMessage(setupMsg.setupMessage)
            }

            try {
                msg.await()

                val delayTime = 2000 - (SystemClock.elapsedRealtime() - startTime)
                if (delayTime > 0) {
                    delay(delayTime)
                }

                view.setLoadingStateFinished()
                view.sceneFinished()
            } catch (e: MessagingException) {
                Timber.e(e, "Error sending setup message")
                view.setLoadingStateSendingFailed()
                view.sceneSendError()
            }
        }
    }
}
