package app.k9mail.legacy.ui.folder

import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.FolderTypeMapper
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import java.text.Collator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import com.fsck.k9.mail.FolderType as LegacyFolderType

class DefaultDisplayFolderRepository(
    private val accountManager: LegacyAccountManager,
    private val messagingController: MessagingControllerRegistry,
    private val messageStoreManager: MessageStoreManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DisplayFolderRepository {
    private val sortForDisplay =
        compareByDescending<DisplayFolder> { it.folder.type == FolderType.INBOX }
            .thenByDescending { it.folder.type == FolderType.OUTBOX }
            .thenByDescending { it.folder.type != FolderType.REGULAR }
            .thenByDescending { it.isInTopGroup }
            .thenBy(
                // #10718 use locale-sensitive ordering for folders
                Collator.getInstance().apply {
                    decomposition = Collator.CANONICAL_DECOMPOSITION
                },
            ) { it.folder.name }

    private fun getDisplayFolders(
        accountId: AccountId,
        outboxFolderId: Long,
        includeHiddenFolders: Boolean,
    ): List<DisplayFolder> {
        val account = accountManager.findById(accountId)?: error("Account not found $accountId")
        val messageStore = messageStoreManager.getMessageStore(accountId)
        return messageStore.getDisplayFolders(
            includeHiddenFolders = includeHiddenFolders,
            outboxFolderId = outboxFolderId,
        ) { folder ->
            DisplayFolder(
                folder = Folder(
                    id = folder.id,
                    name = folder.name,
                    type = folder.takeIf { it.id == outboxFolderId }?.type?.toFolderType()
                        ?: FolderTypeMapper.folderTypeOf(account, folder.id),
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
        accountId: AccountId,
        includeHiddenFolders: Boolean,
    ): Flow<List<DisplayFolder>> {
        val messageStore = messageStoreManager.getMessageStore(accountId)

        return callbackFlow {
            val outboxFolderId = outboxFolderManager.getOutboxFolderId(accountId)
            send(getDisplayFolders(accountId, outboxFolderId, includeHiddenFolders))

            val folderStatusChangedListener = object : SimpleMessagingListener() {
                override fun folderStatusChanged(changedAccountId: AccountId, folderId: Long) {
                    if (changedAccountId == accountId) {
                        trySendBlocking(getDisplayFolders(accountId, outboxFolderId, includeHiddenFolders))
                    }
                }
            }
            messagingController.addListener(folderStatusChangedListener)

            val folderSettingsChangedListener = FolderSettingsChangedListener {
                withContext(ioDispatcher) {
                    trySendBlocking(getDisplayFolders(accountId, outboxFolderId, includeHiddenFolders))
                }
            }
            messageStore.addFolderSettingsChangedListener(folderSettingsChangedListener)

            awaitClose {
                messagingController.removeListener(folderStatusChangedListener)
                messageStore.removeFolderSettingsChangedListener(folderSettingsChangedListener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override fun getDisplayFoldersFlow(accountId: AccountId): Flow<List<DisplayFolder>> {
        return getDisplayFoldersFlow(accountId, includeHiddenFolders = false)
    }

    private fun LegacyFolderType.toFolderType(): FolderType =
        when (this) {
            LegacyFolderType.REGULAR -> FolderType.REGULAR
            LegacyFolderType.INBOX -> FolderType.INBOX
            LegacyFolderType.OUTBOX -> FolderType.OUTBOX
            LegacyFolderType.DRAFTS -> FolderType.DRAFTS
            LegacyFolderType.SENT -> FolderType.SENT
            LegacyFolderType.TRASH -> FolderType.TRASH
            LegacyFolderType.SPAM -> FolderType.SPAM
            LegacyFolderType.ARCHIVE -> FolderType.ARCHIVE
        }
}
