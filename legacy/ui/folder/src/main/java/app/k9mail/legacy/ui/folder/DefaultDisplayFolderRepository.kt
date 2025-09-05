package app.k9mail.legacy.ui.folder

import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.FolderTypeMapper
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType

class DefaultDisplayFolderRepository(
    private val accountManager: AccountManager,
    private val messagingController: MessagingControllerRegistry,
    private val messageStoreManager: MessageStoreManager,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : DisplayFolderRepository {
    private val sortForDisplay =
        compareByDescending<DisplayFolder> { it.folder.type == FolderType.INBOX }
            .thenByDescending { it.folder.type == FolderType.OUTBOX }
            .thenByDescending { it.folder.type != FolderType.REGULAR }
            .thenByDescending { it.isInTopGroup }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.folder.name }

    private fun getDisplayFolders(account: LegacyAccount, includeHiddenFolders: Boolean): List<DisplayFolder> {
        val messageStore = messageStoreManager.getMessageStore(account.uuid)
        return messageStore.getDisplayFolders(
            includeHiddenFolders = includeHiddenFolders,
            outboxFolderId = account.outboxFolderId,
        ) { folder ->
            DisplayFolder(
                folder = Folder(
                    id = folder.id,
                    name = folder.name,
                    type = FolderTypeMapper.folderTypeOf(account, folder.id),
                    isLocalOnly = folder.isLocalOnly,
                ),
                isInTopGroup = folder.isInTopGroup,
                unreadMessageCount = folder.unreadMessageCount,
                starredMessageCount = folder.starredMessageCount,
                pathDelimiter = account.folderPathDelimiter,
            )
        }.sortedWith(sortForDisplay)
    }

    override fun getDisplayFoldersFlow(
        account: LegacyAccount,
        includeHiddenFolders: Boolean,
    ): Flow<List<DisplayFolder>> {
        val messageStore = messageStoreManager.getMessageStore(account.uuid)

        return callbackFlow {
            send(getDisplayFolders(account, includeHiddenFolders))

            val folderStatusChangedListener = object : SimpleMessagingListener() {
                override fun folderStatusChanged(statusChangedAccount: LegacyAccount, folderId: Long) {
                    if (statusChangedAccount.uuid == account.uuid) {
                        trySendBlocking(getDisplayFolders(account, includeHiddenFolders))
                    }
                }
            }
            messagingController.addListener(folderStatusChangedListener)

            val folderSettingsChangedListener = FolderSettingsChangedListener {
                trySendBlocking(getDisplayFolders(account, includeHiddenFolders))
            }
            messageStore.addFolderSettingsChangedListener(folderSettingsChangedListener)

            awaitClose {
                messagingController.removeListener(folderStatusChangedListener)
                messageStore.removeFolderSettingsChangedListener(folderSettingsChangedListener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(coroutineContext)
    }

    override fun getDisplayFoldersFlow(accountUuid: String): Flow<List<DisplayFolder>> {
        val account = accountManager.getAccount(accountUuid) ?: error("Account not found: $accountUuid")
        return getDisplayFoldersFlow(account, includeHiddenFolders = false)
    }
}
