package net.thunderbird.app.common.account

import android.content.Context
import androidx.annotation.VisibleForTesting
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.legacy.mailstore.domain.GetFolderIdsForTypeUseCase
import app.k9mail.legacy.mailstore.domain.SetPushForFolderUseCase
import com.fsck.k9.Core
import com.fsck.k9.account.DeletePolicyProvider
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.createExtra
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isSendClientInfo
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isUseCompression
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.legacy.logging.Log

// TODO Move to feature/account/setup
@Suppress("LongParameterList")
internal class AccountCreator(
    private val accountColorPicker: AccountColorPicker,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val accountManager: LegacyAccountManager,
    private val context: Context,
    private val messagingController: MessagingController,
    private val backendManager: BackendManager,
    private val deletePolicyProvider: DeletePolicyProvider,
    private val avatarMonogramCreator: AvatarMonogramCreator,
    private val unifiedInboxConfigurator: UnifiedInboxConfigurator,
    private val accountDefaultsProvider: AccountDefaultsProvider,
    private val featureFlagProvider: FeatureFlagProvider,
    private val getFolderIdsForTypeUseCase: GetFolderIdsForTypeUseCase,
    private val setPushForFolderUseCase: SetPushForFolderUseCase,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountSetupExternalContract.AccountCreator {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createAccount(account: Account): AccountCreatorResult {
        return try {
            withContext(coroutineDispatcher) { AccountCreatorResult.Success(create(account)) }
        } catch (e: Exception) {
            Log.e(e, "Error while creating new account")

            AccountCreatorResult.Error(e.message ?: "Unknown create account error")
        }
    }

    internal suspend fun createLegacyAccount(account: Account): LegacyAccount {
        val accountId = account.id
        val profile = ProfileDto(
            id = accountId,
            name = account.options.displayName,
            color = accountColorPicker.pickColor(),
            avatar = AvatarDto(
                id = accountId,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = avatarMonogramCreator.create(
                    name = account.options.accountName,
                    email = account.emailAddress,
                ),
                avatarImageUri = null,
                avatarIconName = null,
            ),
        )
        val defaultAccount = accountDefaultsProvider.applyDefaults(
            LegacyAccount(
                id = accountId,
                name = account.options.accountName,
                email = account.emailAddress,
                profile = profile,
                incomingServerSettings = account.incomingServerSettings,
                outgoingServerSettings = account.outgoingServerSettings,
                identities = emptyList(),
            ),
        )
        val identity = defaultAccount.identities.first().copy(
            name = account.options.displayName,
            email = account.emailAddress,
            signatureUse = account.options.emailSignature != null,
            signature = account.options.emailSignature,
        )
        return defaultAccount.copy(
            identities = listOf(identity),
            senderName = identity.name,
            signatureUse = identity.signatureUse,
            signature = identity.signature,
            oAuthState = account.authorizationState,
            isNotifyNewMail = account.options.showNotification,
            automaticCheckIntervalMinutes = account.options.checkFrequencyInMinutes,
            displayCount = account.options.messageDisplayCount,
        ).setIncomingServerSettings(account.incomingServerSettings)
    }

    private suspend fun create(account: Account): AccountId {
        var newAccount = createLegacyAccount(account)

        // this needs the updated incoming server settings
        newAccount = newAccount.copy(
            deletePolicy = deletePolicyProvider.getDeletePolicy(newAccount.incomingServerSettings.type),
        )

        accountManager.updateSync(newAccount)
        localFoldersCreator.createSpecialLocalFolders(newAccount.id)
        newAccount = accountManager.findById(newAccount.id) ?: error("Account not found after creating local folders")

        account.specialFolderSettings?.let { specialFolderSettings ->
            newAccount = newAccount.setSpecialFolders(specialFolderSettings)
        }

        newAccount = newAccount.copy(
            isFinishedSetup = true,
        )

        accountManager.updateSync(newAccount)

        unifiedInboxConfigurator.configureUnifiedInbox()

        Core.setServicesEnabled(context)

        refreshInitialFolderList(newAccount.id)

        featureFlagProvider.provide(GeneratedFeatureFlagKey.PUSH_ENABLED_ON_INBOX_BY_DEFAULT)
            .onEnabled {
                // The AccountCreator is only called when not importing settings.
                // We can update inbox push here by default, as it's always a new account.
                getFolderIdsForTypeUseCase(
                    newAccount.id,
                    FolderType.INBOX,
                ).firstOrNull()?.let { inboxFolderId ->
                    setPushForFolderUseCase(accountUuid = newAccount.id, folderId = inboxFolderId, enabled = true)
                }
            }

        if (account.options.checkFrequencyInMinutes == -1) {
            messagingController.checkMail(newAccount.id, false, true, false, null)
        }

        return newAccount.id
    }

    /**
     * Refreshes folders during setup without saving the account snapshot taken before the refresh.
     */
    @VisibleForTesting
    internal fun refreshInitialFolderList(accountId: AccountId) {
        val folderPathDelimiter = backendManager.getBackend(accountId).refreshFolderList()
        var refreshedAccount = accountManager.findById(accountId)
            ?: error("Account not found after refreshing folder list")
        if (!folderPathDelimiter.isNullOrEmpty() && folderPathDelimiter != refreshedAccount.folderPathDelimiter) {
            refreshedAccount = refreshedAccount.updateFolderDelimiter(folderPathDelimiter)
        }
        accountManager.updateSync(refreshedAccount.updateLastFolderListRefreshTime(System.currentTimeMillis()))
    }

    /**
     * Set special folders by name.
     *
     * Since the folder list hasn't been synced yet, we don't have database IDs for the folders. So we use the same
     * mechanism that is used when importing settings. See [com.fsck.k9.mailstore.SpecialFolderUpdater] for details.
     */
    private fun LegacyAccount.setSpecialFolders(specialFolders: SpecialFolderSettings): LegacyAccount {
        return copy(
            importedArchiveFolder = specialFolders.archiveSpecialFolderOption.toFolderServerId(),
            archiveFolderSelection = specialFolders.archiveSpecialFolderOption.toFolderSelection(),

            importedDraftsFolder = specialFolders.draftsSpecialFolderOption.toFolderServerId(),
            draftsFolderSelection = specialFolders.draftsSpecialFolderOption.toFolderSelection(),

            importedSentFolder = specialFolders.sentSpecialFolderOption.toFolderServerId(),
            sentFolderSelection = specialFolders.sentSpecialFolderOption.toFolderSelection(),

            importedSpamFolder = specialFolders.spamSpecialFolderOption.toFolderServerId(),
            spamFolderSelection = specialFolders.spamSpecialFolderOption.toFolderSelection(),

            importedTrashFolder = specialFolders.trashSpecialFolderOption.toFolderServerId(),
            trashFolderSelection = specialFolders.trashSpecialFolderOption.toFolderSelection(),
        )
    }

    private fun SpecialFolderOption.toFolderServerId(): String? {
        return when (this) {
            is SpecialFolderOption.None -> null
            is SpecialFolderOption.Regular -> remoteFolder.serverId.serverId
            is SpecialFolderOption.Special -> remoteFolder.serverId.serverId
        }
    }

    private fun SpecialFolderOption.toFolderSelection(): SpecialFolderSelection {
        return when (this) {
            is SpecialFolderOption.None -> {
                if (isAutomatic) SpecialFolderSelection.AUTOMATIC else SpecialFolderSelection.MANUAL
            }

            is SpecialFolderOption.Regular -> {
                SpecialFolderSelection.MANUAL
            }

            is SpecialFolderOption.Special -> {
                if (isAutomatic) SpecialFolderSelection.AUTOMATIC else SpecialFolderSelection.MANUAL
            }
        }
    }
}

private fun LegacyAccount.setIncomingServerSettings(serverSettings: ServerSettings): LegacyAccount {
    return if (serverSettings.type == Protocols.IMAP) {
        copy(
            useCompression = serverSettings.isUseCompression,
            isSendClientInfoEnabled = serverSettings.isSendClientInfo,
            incomingServerSettings = serverSettings.copy(
                extra = createExtra(
                    autoDetectNamespace = serverSettings.autoDetectNamespace,
                    pathPrefix = serverSettings.pathPrefix,
                ),
            ),
        )
    } else {
        copy(
            incomingServerSettings = serverSettings,
        )
    }
}
