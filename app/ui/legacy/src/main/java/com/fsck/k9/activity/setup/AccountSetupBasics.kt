package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.Core
import com.fsck.k9.EmailAddressValidator
import com.fsck.k9.Preferences
import com.fsck.k9.account.AccountCreator
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.autodiscovery.api.DiscoveryTarget
import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.helper.SimpleTextWatcher
import com.fsck.k9.helper.Utility.requiredFieldValid
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.ui.ConnectionSettings
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.getEnum
import com.fsck.k9.ui.putEnum
import com.fsck.k9.ui.settings.ExtraAccountDiscovery
import com.fsck.k9.view.ClientCertificateSpinner
import com.google.android.material.textfield.TextInputEditText
import org.koin.android.ext.android.inject

/**
 * Prompts the user for the email address and password.
 *
 * Attempts to lookup default settings for the domain the user specified. If the domain is known, the settings are
 * handed off to the [AccountSetupCheckSettings] activity. If no settings are found, the settings are handed off to the
 * [AccountSetupAccountType] activity.
 */
class AccountSetupBasics : K9Activity() {
    private val providersXmlDiscovery: ProvidersXmlDiscovery by inject()
    private val accountCreator: AccountCreator by inject()
    private val localFoldersCreator: SpecialLocalFoldersCreator by inject()
    private val preferences: Preferences by inject()
    private val emailValidator: EmailAddressValidator by inject()

    private lateinit var emailView: TextInputEditText
    private lateinit var passwordView: TextInputEditText
    private lateinit var passwordLayout: View
    private lateinit var clientCertificateCheckBox: CheckBox
    private lateinit var clientCertificateSpinner: ClientCertificateSpinner
    private lateinit var advancedOptionsContainer: View
    private lateinit var nextButton: Button
    private lateinit var manualSetupButton: Button
    private lateinit var allowClientCertificateView: ViewGroup

    private var uiState = UiState.EMAIL_ADDRESS_ONLY
    private var account: Account? = null
    private var checkedIncoming = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.account_setup_basics)
        setTitle(R.string.account_setup_basics_title)

        emailView = findViewById(R.id.account_email)
        passwordView = findViewById(R.id.account_password)
        passwordLayout = findViewById(R.id.account_password_layout)
        clientCertificateCheckBox = findViewById(R.id.account_client_certificate)
        clientCertificateSpinner = findViewById(R.id.account_client_certificate_spinner)
        allowClientCertificateView = findViewById(R.id.account_allow_client_certificate)
        advancedOptionsContainer = findViewById(R.id.foldable_advanced_options)
        nextButton = findViewById(R.id.next)
        manualSetupButton = findViewById(R.id.manual_setup)

        manualSetupButton.setOnClickListener { onManualSetup() }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        /*
         * We wait until now to initialize the listeners because we didn't want the OnCheckedChangeListener active
         * while the clientCertificateCheckBox state was being restored because it could trigger the pop-up of a
         * ClientCertificateSpinner.chooseCertificate() dialog.
         */
        initializeViewListeners()
        validateFields()

        updateUi()
    }

    private fun initializeViewListeners() {
        val textWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                val checkPassword = uiState == UiState.PASSWORD_FLOW
                validateFields(checkPassword)
            }
        }

        emailView.addTextChangedListener(textWatcher)
        passwordView.addTextChangedListener(textWatcher)

        clientCertificateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            updateViewVisibility(isChecked)
            validateFields()

            // Have the user select the client certificate if not already selected
            if (isChecked && clientCertificateSpinner.alias == null) {
                clientCertificateSpinner.chooseCertificate()
            }
        }

        clientCertificateSpinner.setOnClientCertificateChangedListener {
            validateFields()
        }
    }

    private fun updateUi() {
        when (uiState) {
            UiState.EMAIL_ADDRESS_ONLY -> {
                passwordLayout.isVisible = false
                advancedOptionsContainer.isVisible = false
                nextButton.setOnClickListener { attemptAutoSetupUsingOnlyEmailAddress() }
            }
            UiState.PASSWORD_FLOW -> {
                passwordLayout.isVisible = true
                advancedOptionsContainer.isVisible = true
                nextButton.setOnClickListener { attemptAutoSetup() }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putEnum(STATE_KEY_UI_STATE, uiState)
        outState.putString(EXTRA_ACCOUNT, account?.uuid)
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, checkedIncoming)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        uiState = savedInstanceState.getEnum(STATE_KEY_UI_STATE, UiState.EMAIL_ADDRESS_ONLY)

        val accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT)
        if (accountUuid != null) {
            account = preferences.getAccount(accountUuid)
        }

        checkedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING)
        updateViewVisibility(clientCertificateCheckBox.isChecked)
    }

    private fun updateViewVisibility(usingCertificates: Boolean) {
        allowClientCertificateView.isVisible = usingCertificates
    }

    private fun validateFields(checkPassword: Boolean = true) {
        val email = emailView.text?.toString().orEmpty()
        val valid = requiredFieldValid(emailView) && emailValidator.isValidAddressOnly(email) &&
            (!checkPassword || isPasswordFieldValid())

        nextButton.isEnabled = valid
        nextButton.isFocusable = valid
        manualSetupButton.isEnabled = valid
    }

    private fun isPasswordFieldValid(): Boolean {
        val clientCertificateChecked = clientCertificateCheckBox.isChecked
        val clientCertificateAlias = clientCertificateSpinner.alias

        return !clientCertificateChecked && requiredFieldValid(passwordView) ||
            clientCertificateChecked && clientCertificateAlias != null
    }

    private fun attemptAutoSetupUsingOnlyEmailAddress() {
        val email = emailView.text?.toString() ?: error("Email missing")

        val extraConnectionSettings = ExtraAccountDiscovery.discover(email)
        if (extraConnectionSettings != null) {
            finishAutoSetup(extraConnectionSettings)
            return
        }

        val connectionSettings = providersXmlDiscoveryDiscover(email)

        if (connectionSettings != null &&
            connectionSettings.incoming.authenticationType == AuthType.XOAUTH2 &&
            connectionSettings.outgoing.authenticationType == AuthType.XOAUTH2
        ) {
            startOAuthFlow(connectionSettings)
        } else {
            startPasswordFlow()
        }
    }

    private fun startOAuthFlow(connectionSettings: ConnectionSettings) {
        val account = createAccount(connectionSettings)

        val intent = OAuthFlowActivity.buildLaunchIntent(this, account.uuid)
        startActivityForResult(intent, REQUEST_CODE_OAUTH)
    }

    private fun startPasswordFlow() {
        uiState = UiState.PASSWORD_FLOW

        updateUi()
        validateFields()

        passwordView.requestFocus()
    }

    private fun attemptAutoSetup() {
        if (clientCertificateCheckBox.isChecked) {
            // Auto-setup doesn't support client certificates.
            onManualSetup()
            return
        }

        val email = emailView.text?.toString() ?: error("Email missing")

        val extraConnectionSettings = ExtraAccountDiscovery.discover(email)
        if (extraConnectionSettings != null) {
            finishAutoSetup(extraConnectionSettings)
            return
        }

        val connectionSettings = providersXmlDiscoveryDiscover(email)
        if (connectionSettings != null) {
            finishAutoSetup(connectionSettings)
        } else {
            // We don't have default settings for this account, start the manual setup process.
            onManualSetup()
        }
    }

    private fun finishAutoSetup(connectionSettings: ConnectionSettings) {
        val account = createAccount(connectionSettings)

        // Check incoming here. Then check outgoing in onActivityResult()
        AccountSetupCheckSettings.actionCheckSettings(this, account, CheckDirection.INCOMING)
    }

    private fun createAccount(connectionSettings: ConnectionSettings): Account {
        val email = emailView.text?.toString() ?: error("Email missing")
        val password = passwordView.text?.toString()

        val account = initAccount(email)

        val incomingServerSettings = connectionSettings.incoming.newPassword(password)
        account.incomingServerSettings = incomingServerSettings

        val outgoingServerSettings = connectionSettings.outgoing.newPassword(password)
        account.outgoingServerSettings = outgoingServerSettings

        account.deletePolicy = accountCreator.getDefaultDeletePolicy(incomingServerSettings.type)

        localFoldersCreator.createSpecialLocalFolders(account)

        return account
    }

    private fun onManualSetup() {
        val email = emailView.text?.toString() ?: error("Email missing")
        var password: String? = passwordView.text?.toString()
        var clientCertificateAlias: String? = null
        var authenticationType: AuthType = AuthType.PLAIN

        if (clientCertificateCheckBox.isChecked) {
            clientCertificateAlias = clientCertificateSpinner.alias
            if (password.isNullOrEmpty()) {
                authenticationType = AuthType.EXTERNAL
                password = null
            }
        }

        val account = initAccount(email)

        val initialAccountSettings = InitialAccountSettings(
            authenticationType = authenticationType,
            email = email,
            password = password,
            clientCertificateAlias = clientCertificateAlias
        )

        AccountSetupAccountType.actionSelectAccountType(this, account, makeDefault = false, initialAccountSettings)
    }

    private fun initAccount(email: String): Account {
        val account = this.account ?: createAccount().also { this.account = it }

        account.senderName = getOwnerName()
        account.email = email
        return account
    }

    private fun createAccount(): Account {
        return preferences.newAccount().apply {
            chipColor = accountCreator.pickColor()
        }
    }

    private fun getOwnerName(): String {
        return preferences.defaultAccount?.senderName ?: ""
    }

    private fun providersXmlDiscoveryDiscover(email: String): ConnectionSettings? {
        val discoveryResults = providersXmlDiscovery.discover(email, DiscoveryTarget.INCOMING_AND_OUTGOING)
        if (discoveryResults == null || discoveryResults.incoming.isEmpty() || discoveryResults.outgoing.isEmpty()) {
            return null
        }

        val incomingServerSettings = discoveryResults.incoming.first().toServerSettings() ?: return null
        val outgoingServerSettings = discoveryResults.outgoing.first().toServerSettings() ?: return null

        return ConnectionSettings(incomingServerSettings, outgoingServerSettings)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_CHECK_SETTINGS -> handleCheckSettingsResult(resultCode)
            REQUEST_CODE_OAUTH -> handleSignInResult(resultCode)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleCheckSettingsResult(resultCode: Int) {
        if (resultCode != RESULT_OK) return

        val account = this.account ?: error("Account instance missing")

        if (!checkedIncoming) {
            // We've successfully checked incoming. Now check outgoing.
            checkedIncoming = true
            AccountSetupCheckSettings.actionCheckSettings(this, account, CheckDirection.OUTGOING)
        } else {
            // We've successfully checked outgoing as well.
            preferences.saveAccount(account)
            Core.setServicesEnabled(applicationContext)

            AccountSetupNames.actionSetNames(this, account)
        }
    }

    private fun handleSignInResult(resultCode: Int) {
        if (resultCode != RESULT_OK) return

        val account = this.account ?: error("Account instance missing")

        AccountSetupCheckSettings.actionCheckSettings(this, account, CheckDirection.INCOMING)
    }

    private enum class UiState {
        EMAIL_ADDRESS_ONLY,
        PASSWORD_FLOW
    }

    companion object {
        private const val EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account"
        private const val STATE_KEY_UI_STATE = "com.fsck.k9.AccountSetupBasics.uiState"
        private const val STATE_KEY_CHECKED_INCOMING = "com.fsck.k9.AccountSetupBasics.checkedIncoming"
        private const val REQUEST_CODE_CHECK_SETTINGS = AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE
        private const val REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1

        @JvmStatic
        fun actionNewAccount(context: Context) {
            val intent = Intent(context, AccountSetupBasics::class.java)
            context.startActivity(intent)
        }
    }
}

private fun DiscoveredServerSettings.toServerSettings(): ServerSettings? {
    val authType = this.authType ?: return null
    val username = this.username ?: return null

    return ServerSettings(
        type = protocol,
        host = host,
        port = port,
        connectionSecurity = security,
        authenticationType = authType,
        username = username,
        password = null,
        clientCertificateAlias = null
    )
}
