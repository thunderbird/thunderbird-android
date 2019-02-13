package com.fsck.k9.ui.endtoend


import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.finishWithErrorToast
import com.fsck.k9.ui.R
import com.fsck.k9.view.StatusIndicator
import kotlinx.android.synthetic.main.crypto_key_transfer.*
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import timber.log.Timber


class AutocryptKeyTransferActivity : K9Activity() {
    private val presenter: AutocryptKeyTransferPresenter by inject {
        mapOf("lifecycleOwner" to this, "autocryptTransferView" to this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.crypto_key_transfer)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        transferSendButton.setOnClickListener { presenter.onClickTransferSend() }
        transferButtonShowCode.setOnClickListener { presenter.onClickShowTransferCode() }

        presenter.initFromIntent(accountUuid)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            presenter.onClickHome();
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    fun setAddress(address: String) {
        transferAddress1.text = address
        transferAddress2.text = address
    }

    fun sceneBegin() {
        transferSendButton.visibility = View.VISIBLE
        transferMsgInfo.visibility = View.VISIBLE
        transferLayoutGenerating.visibility = View.GONE
        transferLayoutSending.visibility = View.GONE
        transferLayoutFinish.visibility = View.GONE
        transferErrorSend.visibility = View.GONE
        transferButtonShowCode.visibility = View.GONE
    }

    fun sceneGeneratingAndSending() {
        setupSceneTransition()

        transferSendButton.visibility = View.GONE
        transferMsgInfo.visibility = View.GONE
        transferLayoutGenerating.visibility = View.VISIBLE
        transferLayoutSending.visibility = View.VISIBLE
        transferLayoutFinish.visibility = View.GONE
        transferErrorSend.visibility = View.GONE
        transferButtonShowCode.visibility = View.GONE
    }

    fun sceneSendError() {
        setupSceneTransition()

        transferSendButton.visibility = View.GONE
        transferMsgInfo.visibility = View.GONE
        transferLayoutGenerating.visibility = View.VISIBLE
        transferLayoutSending.visibility = View.VISIBLE
        transferLayoutFinish.visibility = View.GONE
        transferErrorSend.visibility = View.VISIBLE
        transferButtonShowCode.visibility = View.GONE
    }

    fun sceneFinished(transition: Boolean = false) {
        if (transition) {
            setupSceneTransition()
        }

        transferSendButton.visibility = View.GONE
        transferMsgInfo.visibility = View.GONE
        transferLayoutGenerating.visibility = View.VISIBLE
        transferLayoutSending.visibility = View.VISIBLE
        transferLayoutFinish.visibility = View.VISIBLE
        transferErrorSend.visibility = View.GONE
        transferButtonShowCode.visibility = View.VISIBLE
    }

    fun setLoadingStateGenerating() {
        transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.PROGRESS)
        transferProgressSending.setDisplayedChild(StatusIndicator.Status.IDLE)
    }

    fun setLoadingStateSending() {
        transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.OK)
        transferProgressSending.setDisplayedChild(StatusIndicator.Status.PROGRESS)
    }

    fun setLoadingStateSendingFailed() {
        transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.OK)
        transferProgressSending.setDisplayedChild(StatusIndicator.Status.ERROR)
    }

    fun setLoadingStateFinished() {
        transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.OK)
        transferProgressSending.setDisplayedChild(StatusIndicator.Status.OK)
    }

    fun finishWithInvalidAccountError() {
        finishWithErrorToast(R.string.toast_account_not_found)
    }

    fun finishWithProviderConnectError(providerName: String) {
        finishWithErrorToast(R.string.toast_openpgp_provider_error, providerName)
    }

    fun launchUserInteractionPendingIntent(pendingIntent: PendingIntent) {
        try {
            startIntentSender(pendingIntent.intentSender, null, 0, 0, 0)
        } catch (e: SendIntentException) {
            Timber.e(e)
        }
    }

    private fun setupSceneTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val transition = TransitionInflater.from(this).inflateTransition(R.transition.transfer_transitions)
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), transition)
        }
    }

    fun finishAsCancelled() {
        setResult(RESULT_CANCELED)
        finish()
    }

    suspend fun uxDelay() {
        // called before logic resumes upon screen transitions, to give some breathing room
        delay(UX_DELAY_MS)
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"
        private const val UX_DELAY_MS = 1200L

        fun createIntent(context: Context, accountUuid: String): Intent {
            val intent = Intent(context, AutocryptKeyTransferActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, accountUuid)
            return intent
        }
    }
}
