package com.fsck.k9.ui.settings.account

import android.content.Intent
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.ListPreference
import android.widget.Toast
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.activity.ManageIdentities
import com.fsck.k9.activity.setup.AccountSetupComposition
import com.fsck.k9.activity.setup.AccountSetupIncoming
import com.fsck.k9.activity.setup.AccountSetupOutgoing
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.crypto.OpenPgpApiHelper
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.settings.oneTimeClickListener
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.settings.removeEntry
import com.fsck.k9.ui.withArguments
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import com.fsck.k9.ui.observe
import org.koin.android.architecture.ext.sharedViewModel
import org.koin.android.ext.android.inject
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.util.OpenPgpKeyPreference
import org.openintents.openpgp.util.OpenPgpProviderUtil

class AccountSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: AccountSettingsViewModel by sharedViewModel()
    private val dataStoreFactory: AccountSettingsDataStoreFactory by inject()
    private val storageManager: StorageManager by inject()
    private val openPgpApiManager: OpenPgpApiManager by inject(parameters = { mapOf("lifecycleOwner" to this) })
    private val messagingController: MessagingController by inject()

    private val accountUuid: String by lazy {
        checkNotNull(arguments?.getString(ARG_ACCOUNT_UUID)) { "$ARG_ACCOUNT_UUID == null" }
    }
    private var title: CharSequence? = null


    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        val account = getAccount()
        val dataStore = dataStoreFactory.create(account)

        preferenceManager.preferenceDataStore = dataStore
        setPreferencesFromResource(R.xml.account_settings, rootKey)
        title = preferenceScreen.title

        initializeIncomingServer()
        initializeComposition()
        initializeManageIdentities()
        initializeOutgoingServer()
        initializeQuoteStyle()
        initializeDeletePolicy(account)
        initializeExpungePolicy(account)
        initializeMessageAge(account)
        initializeAdvancedPushSettings(account)
        initializeLocalStorageProvider()
        initializeCryptoSettings(account)
        initializeFolderSettings(account)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().title = title
    }

    private fun initializeIncomingServer() {
        findPreference(PREFERENCE_INCOMING_SERVER)?.onClick {
            AccountSetupIncoming.actionEditIncomingSettings(requireActivity(), accountUuid)
        }
    }

    private fun initializeComposition() {
        findPreference(PREFERENCE_COMPOSITION)?.onClick {
            AccountSetupComposition.actionEditCompositionSettings(requireActivity(), accountUuid)
        }
    }

    private fun initializeManageIdentities() {
        findPreference(PREFERENCE_MANAGE_IDENTITIES)?.onClick {
            ManageIdentities.start(requireActivity(), accountUuid)
        }
    }

    private fun initializeOutgoingServer() {
        findPreference(PREFERENCE_OUTGOING_SERVER)?.onClick {
            AccountSetupOutgoing.actionEditOutgoingSettings(requireActivity(), accountUuid)
        }
    }

    private fun initializeQuoteStyle() {
        findPreference(PREFERENCE_QUOTE_STYLE)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val quoteStyle = Account.QuoteStyle.valueOf(newValue.toString())
                notifyDependencyChange(quoteStyle == Account.QuoteStyle.HEADER)
                true
            }
        }
    }

    private fun initializeDeletePolicy(account: Account) {
        (findPreference(PREFERENCE_DELETE_POLICY) as? ListPreference)?.apply {
            if (!messagingController.supportsSeenFlag(account)) {
                removeEntry(DELETE_POLICY_MARK_AS_READ)
            }
        }
    }

    private fun initializeExpungePolicy(account: Account) {
        findPreference(PREFERENCE_EXPUNGE_POLICY)?.apply {
            if (!messagingController.supportsExpunge(account)) {
                remove()
            }
        }
    }

    private fun initializeMessageAge(account: Account) {
        findPreference(PREFERENCE_MESSAGE_AGE)?.apply {
            if (!messagingController.supportsSearchByDate(account)) {
                remove()
            }
        }
    }

    private fun initializeAdvancedPushSettings(account: Account) {
        if (!messagingController.isPushCapable(account)) {
            findPreference(PREFERENCE_PUSH_MODE)?.remove()
            findPreference(PREFERENCE_ADVANCED_PUSH_SETTINGS)?.remove()
            findPreference(PREFERENCE_REMOTE_SEARCH)?.remove()
        }
    }

    private fun initializeLocalStorageProvider() {
        (findPreference(PREFERENCE_LOCAL_STORAGE_PROVIDER) as? ListPreference)?.apply {
            val providers = storageManager.availableProviders.entries
            entries = providers.map { it.value }.toTypedArray()
            entryValues = providers.map { it.key }.toTypedArray()
        }
    }

    private fun initializeCryptoSettings(account: Account) {
        findPreference(PREFERENCE_OPENPGP)?.let {
            configureCryptoPreferences(account)
        }
    }

    private fun configureCryptoPreferences(account: Account) {
        var pgpProviderName: String? = null
        var pgpProvider = account.openPgpProvider
        var isPgpConfigured = account.isOpenPgpProviderConfigured

        if (isPgpConfigured) {
            pgpProviderName = getOpenPgpProviderName(pgpProvider)
            if (pgpProviderName == null) {
                Toast.makeText(requireContext(), R.string.account_settings_openpgp_missing, Toast.LENGTH_LONG).show()

                account.openPgpProvider = null
                pgpProvider = null
                isPgpConfigured = false
            }
        }

        configureEnablePgpSupport(account, isPgpConfigured, pgpProviderName)
        configurePgpKey(account, pgpProvider)
        configureAutocryptTransfer(account)
    }

    private fun getOpenPgpProviderName(pgpProvider: String?): String? {
        val packageManager = requireActivity().packageManager
        return OpenPgpProviderUtil.getOpenPgpProviderName(packageManager, pgpProvider)
    }

    private fun configureEnablePgpSupport(account: Account, isPgpConfigured: Boolean, pgpProviderName: String?) {
        (findPreference(PREFERENCE_OPENPGP_ENABLE) as SwitchPreference).apply {
            if (!isPgpConfigured) {
                isChecked = false
                setSummary(R.string.account_settings_crypto_summary_off)
                oneTimeClickListener(clickHandled = false) {
                    val context = requireContext().applicationContext
                    val openPgpProviderPackages = OpenPgpProviderUtil.getOpenPgpProviderPackages(context)
                    if (openPgpProviderPackages.size == 1) {
                        account.openPgpProvider = openPgpProviderPackages[0]
                        configureCryptoPreferences(account)
                    } else {
                        OpenPgpAppSelectDialog.startOpenPgpChooserActivity(requireActivity(), account)
                    }
                }
            } else {
                isChecked = true
                summary = getString(R.string.account_settings_crypto_summary_on, pgpProviderName)
                oneTimeClickListener {
                    account.openPgpProvider = null
                    account.openPgpKey = Account.NO_OPENPGP_KEY
                    configureCryptoPreferences(account)
                }
            }
        }
    }

    private fun configurePgpKey(account: Account, pgpProvider: String?) {
        (findPreference(PREFERENCE_OPENPGP_KEY) as OpenPgpKeyPreference).apply {
            setOpenPgpProvider(openPgpApiManager, pgpProvider)
            setIntentSenderFragment(this@AccountSettingsFragment)
            setDefaultUserId(OpenPgpApiHelper.buildUserId(account.getIdentity(0)))
            setShowAutocryptHint(true)
        }
    }

    private fun configureAutocryptTransfer(account: Account) {
        findPreference(PREFERENCE_AUTOCRYPT_TRANSFER).onClick {
            val intent = AutocryptKeyTransferActivity.createIntent(requireContext(), account.uuid)
            startActivity(intent)
        }
    }

    private fun initializeFolderSettings(account: Account) {
        findPreference(PREFERENCE_FOLDERS)?.let {
            if (!messagingController.isMoveCapable(account)) {
                findPreference(PREFERENCE_ARCHIVE_FOLDER).remove()
                findPreference(PREFERENCE_DRAFTS_FOLDER).remove()
                findPreference(PREFERENCE_SENT_FOLDER).remove()
                findPreference(PREFERENCE_SPAM_FOLDER).remove()
                findPreference(PREFERENCE_TRASH_FOLDER).remove()
            }

            loadFolders(account)
        }
    }

    private fun loadFolders(account: Account) {
        viewModel.getFolders(account).observe(this@AccountSettingsFragment) { folders ->
            if (folders != null) {
                FOLDER_LIST_PREFERENCES.forEach {
                    (findPreference(it) as? FolderListPreference)?.folders = folders
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val openPgpKeyPreference = findPreference(PREFERENCE_OPENPGP_KEY) as? OpenPgpKeyPreference
        if (openPgpKeyPreference?.handleOnActivityResult(requestCode, resultCode, data) == true) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getAccount(): Account {
        return viewModel.getAccountBlocking(accountUuid)
    }


    companion object {
        internal const val PREFERENCE_OPENPGP = "openpgp"
        private const val ARG_ACCOUNT_UUID = "accountUuid"
        private const val PREFERENCE_INCOMING_SERVER = "incoming"
        private const val PREFERENCE_COMPOSITION = "composition"
        private const val PREFERENCE_MANAGE_IDENTITIES = "manage_identities"
        private const val PREFERENCE_OUTGOING_SERVER = "outgoing"
        private const val PREFERENCE_QUOTE_STYLE = "quote_style"
        private const val PREFERENCE_DELETE_POLICY = "delete_policy"
        private const val PREFERENCE_EXPUNGE_POLICY = "expunge_policy"
        private const val PREFERENCE_MESSAGE_AGE = "account_message_age"
        private const val PREFERENCE_PUSH_MODE = "folder_push_mode"
        private const val PREFERENCE_ADVANCED_PUSH_SETTINGS = "push_advanced"
        private const val PREFERENCE_REMOTE_SEARCH = "search"
        private const val PREFERENCE_LOCAL_STORAGE_PROVIDER = "local_storage_provider"
        private const val PREFERENCE_OPENPGP_ENABLE = "openpgp_provider"
        private const val PREFERENCE_OPENPGP_KEY = "openpgp_key"
        private const val PREFERENCE_AUTOCRYPT_TRANSFER = "autocrypt_transfer"
        private const val PREFERENCE_FOLDERS = "folders"
        private const val PREFERENCE_AUTO_EXPAND_FOLDER = "account_setup_auto_expand_folder"
        private const val PREFERENCE_ARCHIVE_FOLDER = "archive_folder"
        private const val PREFERENCE_DRAFTS_FOLDER = "drafts_folder"
        private const val PREFERENCE_SENT_FOLDER = "sent_folder"
        private const val PREFERENCE_SPAM_FOLDER = "spam_folder"
        private const val PREFERENCE_TRASH_FOLDER = "trash_folder"
        private const val DELETE_POLICY_MARK_AS_READ = "MARK_AS_READ"

        private val FOLDER_LIST_PREFERENCES = listOf(
                PREFERENCE_AUTO_EXPAND_FOLDER,
                PREFERENCE_ARCHIVE_FOLDER,
                PREFERENCE_DRAFTS_FOLDER,
                PREFERENCE_SENT_FOLDER,
                PREFERENCE_SPAM_FOLDER,
                PREFERENCE_TRASH_FOLDER
        )

        fun create(accountUuid: String, rootKey: String?) = AccountSettingsFragment().withArguments(
                ARG_ACCOUNT_UUID to accountUuid,
                PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to rootKey)
    }
}
