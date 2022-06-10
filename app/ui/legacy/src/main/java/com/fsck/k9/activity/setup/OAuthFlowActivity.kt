package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.preferences.AccountManager
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.observe
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class OAuthFlowActivity : K9Activity() {
    private val authViewModel: AuthViewModel by viewModel()
    private val accountManager: AccountManager by inject()

    private lateinit var errorText: TextView
    private lateinit var signInButton: Button
    private lateinit var signInProgress: ProgressBar

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.account_setup_oauth)
        setTitle(R.string.account_setup_basics_title)

        val accountUUid = intent.getStringExtra(EXTRA_ACCOUNT_UUID) ?: error("Missing account UUID")
        val account = accountManager.getAccount(accountUUid) ?: error("Account not found")

        errorText = findViewById(R.id.error_text)
        signInProgress = findViewById(R.id.sign_in_progress)
        signInButton = if (authViewModel.isUsingGoogle(account)) {
            findViewById(R.id.google_sign_in_button)
        } else {
            findViewById(R.id.oauth_sign_in_button)
        }

        signInButton.isVisible = true
        signInButton.setOnClickListener { startOAuthFlow(account) }

        savedInstanceState?.let {
            val signInRunning = it.getBoolean(STATE_PROGRESS)
            signInButton.isVisible = !signInRunning
            signInProgress.isVisible = signInRunning
        }

        authViewModel.init(activityResultRegistry, lifecycle)

        authViewModel.uiState.observe(this) { state ->
            handleUiUpdates(state)
        }
    }

    private fun handleUiUpdates(state: AuthFlowState) {
        when (state) {
            AuthFlowState.Idle -> {
                return
            }
            AuthFlowState.Success -> {
                setResult(RESULT_OK)
                finish()
            }
            AuthFlowState.Canceled -> {
                displayErrorText(R.string.account_setup_failed_dlg_oauth_flow_canceled)
            }
            is AuthFlowState.Failed -> {
                displayErrorText(R.string.account_setup_failed_dlg_oauth_flow_failed, state)
            }
            AuthFlowState.NotSupported -> {
                displayErrorText(R.string.account_setup_failed_dlg_oauth_not_supported)
            }
            AuthFlowState.BrowserNotFound -> {
                displayErrorText(R.string.account_setup_failed_dlg_browser_not_found)
            }
        }

        authViewModel.authResultConsumed()
    }

    private fun displayErrorText(errorTextResId: Int, vararg args: Any?) {
        signInProgress.isVisible = false
        signInButton.isVisible = true
        errorText.text = getString(errorTextResId, *args)
    }

    private fun startOAuthFlow(account: Account) {
        signInButton.isVisible = false
        signInProgress.isVisible = true
        errorText.text = ""

        authViewModel.login(account)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_PROGRESS, signInProgress.isVisible)
    }

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"

        private const val STATE_PROGRESS = "signInProgress"

        fun buildLaunchIntent(context: Context, accountUuid: String): Intent {
            return Intent(context, OAuthFlowActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
            }
        }
    }
}
