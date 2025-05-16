package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.featureflag.FeatureFlagKey
import app.k9mail.core.featureflag.FeatureFlagProvider
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import com.fsck.k9.account.BackgroundAccountRemover
import com.fsck.k9.activity.ManageIdentities
import com.fsck.k9.activity.setup.AccountSetupComposition
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.crypto.OpenPgpApiHelper
import com.fsck.k9.fragment.ConfirmationDialogFragment
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import com.fsck.k9.notification.NotificationSettingsUpdater
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.extensions.withArguments
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.settings.oneTimeClickListener
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.settings.removeEntry
import com.takisoft.preferencex.PreferenceFragmentCompat
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.NO_OPENPGP_KEY
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.util.OpenPgpKeyPreference
import org.openintents.openpgp.util.OpenPgpProviderUtil
import com.fsck.k9.ui.base.R as BaseR

class AccountSettingsFragment : PreferenceFragmentCompat(), ConfirmationDialogFragmentListener {
    private val viewModel: AccountSettingsViewModel by activityViewModel()
    private val dataStoreFactory: AccountSettingsDataStoreFactory by inject()
    private val openPgpApiManager: OpenPgpApiManager by inject { parametersOf(this) }
    private val messagingController: MessagingController by inject()
    private val accountRemover: BackgroundAccountRemover by inject()
    private val notificationChannelManager: NotificationChannelManager by inject()
    private val notificationSettingsUpdater: NotificationSettingsUpdater by inject()
    private val vibrator: Vibrator by inject()
    private val appNameProvider: AppNameProvider by inject()
    private val featureFlagProvider: FeatureFlagProvider by inject()

    private lateinit var dataStore: AccountSettingsDataStore

    private var notificationSoundPreference: NotificationSoundPreference? = null
    private var notificationLightPreference: ListPreference? = null
    private var notificationVibrationPreference: VibrationPreference? = null

    private val accountUuid: String by lazy {
        checkNotNull(arguments?.getString(ARG_ACCOUNT_UUID)) { "$ARG_ACCOUNT_UUID == null" }
    }
    private var title: CharSequence? = null

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        val account = getAccount()
        dataStore = dataStoreFactory.create(account)

        preferenceManager.preferenceDataStore = dataStore
        setPreferencesFromResource(R.xml.account_settings, rootKey)
        title = preferenceScreen.title
        setHasOptionsMenu(true)

        initializeGeneralSettings()
        initializeIncomingServer()
        initializeComposition()
        initializeManageIdentities()
        initializeUploadSentMessages(account)
        initializeOutgoingServer()
        initializeQuoteStyle()
        initializeDeletePolicy(account)
        initializeExpungePolicy(account)
        initializeMessageAge(account)
        initializeAdvancedPushSettings(account)
        initializeCryptoSettings(account)
        initializeFolderSettings(account)
        initializeNotifications(account)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().title = title
    }

    override fun onResume() {
        super.onResume()

        // we might be returning from OpenPgpAppSelectDialog, make sure settings are up to date
        val account = getAccount()
        initializeCryptoSettings(account)

        // Don't update the notification preferences when resuming after the user has selected a new notification sound
        // via NotificationSoundPreference. Otherwise we race the background thread and might read data from the old
        // NotificationChannel, overwriting the notification sound with the previous value.
        notificationSoundPreference?.let { notificationSoundPreference ->
            if (notificationSoundPreference.receivedActivityResultJustNow) {
                notificationSoundPreference.receivedActivityResultJustNow = false
            } else {
                maybeUpdateNotificationPreferences(account)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.account_settings_option, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_account -> {
                onDeleteAccount()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeGeneralSettings() {
        featureFlagProvider.provide(FeatureFlagKey("new_account_settings"))
            .onEnabled {
                findPreference<Preference>(PREFERENCE_GENERAL_LEGACY)?.remove()

                findPreference<Preference>(PREFERENCE_GENERAL)?.onClick {
                    FeatureLauncherActivity.launch(
                        context = requireActivity(),
                        target = FeatureLauncherTarget.AccountSettings(accountUuid),
                    )
                }
            }.onDisabledOrUnavailable {
                findPreference<Preference>(PREFERENCE_GENERAL)?.remove()
            }
    }

    private fun initializeIncomingServer() {
        findPreference<Preference>(PREFERENCE_INCOMING_SERVER)?.onClick {
            FeatureLauncherActivity.launch(
                context = requireActivity(),
                target = FeatureLauncherTarget.AccountEditIncomingSettings(accountUuid),
            )
        }
    }

    private fun initializeComposition() {
        findPreference<Preference>(PREFERENCE_COMPOSITION)?.onClick {
            AccountSetupComposition.actionEditCompositionSettings(requireActivity(), accountUuid)
        }
    }

    private fun initializeManageIdentities() {
        findPreference<Preference>(PREFERENCE_MANAGE_IDENTITIES)?.onClick {
            ManageIdentities.start(requireActivity(), accountUuid)
        }
    }

    private fun initializeUploadSentMessages(account: LegacyAccount) {
        findPreference<Preference>(PREFERENCE_UPLOAD_SENT_MESSAGES)?.apply {
            if (!messagingController.supportsUpload(account)) {
                remove()
            }
        }
    }

    private fun initializeOutgoingServer() {
        findPreference<Preference>(PREFERENCE_OUTGOING_SERVER)?.onClick {
            FeatureLauncherActivity.launch(
                context = requireActivity(),
                target = FeatureLauncherTarget.AccountEditOutgoingSettings(accountUuid),
            )
        }
    }

    private fun initializeQuoteStyle() {
        findPreference<Preference>(PREFERENCE_QUOTE_STYLE)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val quoteStyle = QuoteStyle.valueOf(newValue.toString())
                notifyDependencyChange(quoteStyle == QuoteStyle.HEADER)
                true
            }
        }
    }

    private fun initializeDeletePolicy(account: LegacyAccount) {
        (findPreference(PREFERENCE_DELETE_POLICY) as? ListPreference)?.apply {
            if (!messagingController.supportsFlags(account)) {
                removeEntry(DELETE_POLICY_MARK_AS_READ)
            }
        }
    }

    private fun initializeExpungePolicy(account: LegacyAccount) {
        findPreference<Preference>(PREFERENCE_EXPUNGE_POLICY)?.apply {
            if (!messagingController.supportsExpunge(account)) {
                remove()
            }
        }
    }

    private fun initializeMessageAge(account: LegacyAccount) {
        findPreference<Preference>(PREFERENCE_MESSAGE_AGE)?.apply {
            if (!messagingController.supportsSearchByDate(account)) {
                remove()
            }
        }
    }

    private fun initializeAdvancedPushSettings(account: LegacyAccount) {
        if (!messagingController.isPushCapable(account)) {
            findPreference<Preference>(PREFERENCE_ADVANCED_PUSH_SETTINGS)?.remove()
        }
    }

    private fun initializeNotifications(account: LegacyAccount) {
        if (!vibrator.hasVibrator) {
            findPreference<Preference>(PREFERENCE_NOTIFICATION_VIBRATION)?.remove()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            findPreference<NotificationSoundPreference>(PREFERENCE_NOTIFICATION_SOUND)?.let { preference ->
                notificationSoundPreference = preference
            }

            findPreference<ListPreference>(PREFERENCE_NOTIFICATION_LIGHT)?.let { preference ->
                notificationLightPreference = preference
            }

            findPreference<VibrationPreference>(PREFERENCE_NOTIFICATION_VIBRATION)?.let { preference ->
                notificationVibrationPreference = preference
            }

            findPreference<NotificationsPreference>(PREFERENCE_NOTIFICATION_SETTINGS_MESSAGES)?.let {
                it.notificationChannelIdProvider = {
                    notificationChannelManager.getChannelIdFor(account, ChannelType.MESSAGES)
                }
            }

            findPreference<NotificationsPreference>(PREFERENCE_NOTIFICATION_SETTINGS_MISCELLANEOUS)?.let {
                it.notificationChannelIdProvider = {
                    notificationChannelManager.getChannelIdFor(account, ChannelType.MISCELLANEOUS)
                }
            }

            updateNotificationPreferences(account)
        } else {
            findPreference<PreferenceCategory>(PREFERENCE_NOTIFICATION_CHANNELS).remove()
        }
    }

    private fun maybeUpdateNotificationPreferences(account: LegacyAccount) {
        if (notificationSoundPreference != null ||
            notificationLightPreference != null ||
            notificationVibrationPreference != null
        ) {
            updateNotificationPreferences(account)
        }
    }

    @SuppressLint("NewApi")
    private fun updateNotificationPreferences(account: LegacyAccount) {
        notificationSettingsUpdater.updateNotificationSettings(account)
        val notificationSettings = account.notificationSettings

        notificationSoundPreference?.setNotificationSound(notificationSettings.ringtone?.toUri())

        notificationLightPreference?.value = notificationSettings.light.name

        notificationVibrationPreference?.let { preference ->
            val notificationVibration = notificationSettings.vibration
            preference.setVibration(
                isVibrationEnabled = notificationVibration.isEnabled,
                vibratePattern = notificationVibration.pattern,
                vibrationTimes = notificationVibration.repeatCount,
            )
        }
    }

    private fun initializeCryptoSettings(account: LegacyAccount) {
        findPreference<Preference>(PREFERENCE_OPENPGP)?.let {
            configureCryptoPreferences(account)
        }
    }

    private fun configureCryptoPreferences(account: LegacyAccount) {
        var pgpProviderName: String? = null
        var pgpProvider = account.openPgpProvider
        val isPgpConfigured = pgpProvider != null

        if (isPgpConfigured) {
            pgpProviderName = getOpenPgpProviderName(pgpProvider)
            if (pgpProviderName == null) {
                Toast.makeText(requireContext(), R.string.account_settings_openpgp_missing, Toast.LENGTH_LONG).show()

                pgpProvider = null
                removeOpenPgpProvider(account)
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

    private fun configureEnablePgpSupport(account: LegacyAccount, isPgpConfigured: Boolean, pgpProviderName: String?) {
        (findPreference<Preference>(PREFERENCE_OPENPGP_ENABLE) as SwitchPreference).apply {
            if (!isPgpConfigured) {
                isChecked = false
                setSummary(R.string.account_settings_crypto_summary_off)
                oneTimeClickListener(clickHandled = false) {
                    val context = requireContext().applicationContext
                    val openPgpProviderPackages = OpenPgpProviderUtil.getOpenPgpProviderPackages(context)
                    if (openPgpProviderPackages.size == 1) {
                        setOpenPgpProvider(account, openPgpProviderPackages[0])
                        configureCryptoPreferences(account)
                    } else {
                        summary = getString(R.string.account_settings_crypto_summary_config)
                        OpenPgpAppSelectDialog.startOpenPgpChooserActivity(requireActivity(), account)
                    }
                }
            } else {
                isChecked = true
                summary = getString(R.string.account_settings_crypto_summary_on, pgpProviderName)
                oneTimeClickListener {
                    removeOpenPgpProvider(account)
                    configureCryptoPreferences(account)
                }
            }
        }
    }

    private fun configurePgpKey(account: LegacyAccount, pgpProvider: String?) {
        (findPreference<Preference>(PREFERENCE_OPENPGP_KEY) as OpenPgpKeyPreference).apply {
            value = account.openPgpKey
            setOpenPgpProvider(openPgpApiManager, pgpProvider)
            setIntentSenderFragment(this@AccountSettingsFragment)
            setDefaultUserId(OpenPgpApiHelper.buildUserId(account.getIdentity(0)))
            setShowAutocryptHint(true)
        }
    }

    private fun configureAutocryptTransfer(account: LegacyAccount) {
        findPreference<Preference>(PREFERENCE_AUTOCRYPT_TRANSFER)!!.onClick {
            val intent = AutocryptKeyTransferActivity.createIntent(requireContext(), account.uuid)
            startActivity(intent)
        }
    }

    private fun initializeFolderSettings(account: LegacyAccount) {
        findPreference<Preference>(PREFERENCE_FOLDERS)?.let {
            if (!messagingController.supportsFolderSubscriptions(account)) {
                findPreference<Preference>(PREFERENCE_SUBSCRIBED_FOLDERS_ONLY).remove()
            }

            if (!messagingController.isMoveCapable(account)) {
                findPreference<Preference>(PREFERENCE_ARCHIVE_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_DRAFTS_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_SENT_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_SPAM_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_TRASH_FOLDER).remove()
            }

            loadFolders(account)
        }
    }

    private fun loadFolders(account: LegacyAccount) {
        viewModel.getFolders(account).observe(this@AccountSettingsFragment) { remoteFolderInfo ->
            if (remoteFolderInfo != null) {
                setFolders(PREFERENCE_AUTO_EXPAND_FOLDER, remoteFolderInfo.folders)
                setFolders(PREFERENCE_ARCHIVE_FOLDER, remoteFolderInfo, FolderType.ARCHIVE)
                setFolders(PREFERENCE_DRAFTS_FOLDER, remoteFolderInfo, FolderType.DRAFTS)
                setFolders(PREFERENCE_SENT_FOLDER, remoteFolderInfo, FolderType.SENT)
                setFolders(PREFERENCE_SPAM_FOLDER, remoteFolderInfo, FolderType.SPAM)
                setFolders(PREFERENCE_TRASH_FOLDER, remoteFolderInfo, FolderType.TRASH)
            }
        }
    }

    private fun setFolders(preferenceKey: String, folders: List<RemoteFolder>) {
        val folderListPreference = findPreference(preferenceKey) as? FolderListPreference ?: return
        folderListPreference.setFolders(folders)
    }

    private fun setFolders(preferenceKey: String, remoteFolderInfo: RemoteFolderInfo, type: FolderType?) {
        val folderListPreference = findPreference(preferenceKey) as? FolderListPreference ?: return

        val automaticFolder = remoteFolderInfo.automaticSpecialFolders[type]
        folderListPreference.setFolders(remoteFolderInfo.folders, automaticFolder)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val openPgpKeyPreference = findPreference(PREFERENCE_OPENPGP_KEY) as? OpenPgpKeyPreference
        if (openPgpKeyPreference?.handleOnActivityResult(requestCode, resultCode, data) == true) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getAccount(): LegacyAccount {
        return viewModel.getAccountBlocking(accountUuid)
    }

    private fun onDeleteAccount() {
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            DIALOG_DELETE_ACCOUNT,
            getString(R.string.account_delete_dlg_title),
            getString(R.string.account_delete_dlg_instructions_fmt, getAccount().displayName, appNameProvider.appName),
            getString(BaseR.string.okay_action),
            getString(BaseR.string.cancel_action),
        )
        dialogFragment.setTargetFragment(this, REQUEST_DELETE_ACCOUNT)
        dialogFragment.show(requireFragmentManager(), TAG_DELETE_ACCOUNT_CONFIRMATION)
    }

    override fun doPositiveClick(dialogId: Int) {
        closeAccountSettings()
        accountRemover.removeAccountAsync(accountUuid)
    }

    override fun doNegativeClick(dialogId: Int) = Unit

    override fun dialogCancelled(dialogId: Int) = Unit

    private fun closeAccountSettings() {
        requireActivity().finish()
    }

    private fun setOpenPgpProvider(account: LegacyAccount, openPgpProviderPackage: String) {
        account.openPgpProvider = openPgpProviderPackage
        dataStore.saveSettingsInBackground()
    }

    private fun removeOpenPgpProvider(account: LegacyAccount) {
        account.openPgpProvider = null
        account.openPgpKey = NO_OPENPGP_KEY
        dataStore.saveSettingsInBackground()
    }

    companion object {
        internal const val PREFERENCE_OPENPGP = "openpgp"
        private const val ARG_ACCOUNT_UUID = "accountUuid"
        private const val PREFERENCE_GENERAL = "general"
        private const val PREFERENCE_GENERAL_LEGACY = "account_settings"
        private const val PREFERENCE_INCOMING_SERVER = "incoming"
        private const val PREFERENCE_COMPOSITION = "composition"
        private const val PREFERENCE_MANAGE_IDENTITIES = "manage_identities"
        private const val PREFERENCE_OUTGOING_SERVER = "outgoing"
        private const val PREFERENCE_UPLOAD_SENT_MESSAGES = "upload_sent_messages"
        private const val PREFERENCE_QUOTE_STYLE = "quote_style"
        private const val PREFERENCE_DELETE_POLICY = "delete_policy"
        private const val PREFERENCE_EXPUNGE_POLICY = "expunge_policy"
        private const val PREFERENCE_MESSAGE_AGE = "account_message_age"
        private const val PREFERENCE_ADVANCED_PUSH_SETTINGS = "push_advanced"
        private const val PREFERENCE_OPENPGP_ENABLE = "openpgp_provider"
        private const val PREFERENCE_OPENPGP_KEY = "openpgp_key"
        private const val PREFERENCE_AUTOCRYPT_TRANSFER = "autocrypt_transfer"
        private const val PREFERENCE_FOLDERS = "folders"
        private const val PREFERENCE_AUTO_EXPAND_FOLDER = "account_setup_auto_expand_folder"
        private const val PREFERENCE_SUBSCRIBED_FOLDERS_ONLY = "subscribed_folders_only"
        private const val PREFERENCE_ARCHIVE_FOLDER = "archive_folder"
        private const val PREFERENCE_DRAFTS_FOLDER = "drafts_folder"
        private const val PREFERENCE_SENT_FOLDER = "sent_folder"
        private const val PREFERENCE_SPAM_FOLDER = "spam_folder"
        private const val PREFERENCE_TRASH_FOLDER = "trash_folder"
        private const val PREFERENCE_NOTIFICATION_SOUND = "account_ringtone"
        private const val PREFERENCE_NOTIFICATION_LIGHT = "notification_light"
        private const val PREFERENCE_NOTIFICATION_VIBRATION = "account_combined_vibration"
        private const val PREFERENCE_NOTIFICATION_CHANNELS = "notification_channels"
        private const val PREFERENCE_NOTIFICATION_SETTINGS_MESSAGES = "open_notification_settings_messages"
        private const val PREFERENCE_NOTIFICATION_SETTINGS_MISCELLANEOUS = "open_notification_settings_miscellaneous"
        private const val DELETE_POLICY_MARK_AS_READ = "MARK_AS_READ"

        private const val DIALOG_DELETE_ACCOUNT = 1
        private const val REQUEST_DELETE_ACCOUNT = 1
        private const val TAG_DELETE_ACCOUNT_CONFIRMATION = "delete_account_confirmation"

        fun create(accountUuid: String, rootKey: String?) = AccountSettingsFragment().withArguments(
            ARG_ACCOUNT_UUID to accountUuid,
            PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to rootKey,
        )
    }
}
