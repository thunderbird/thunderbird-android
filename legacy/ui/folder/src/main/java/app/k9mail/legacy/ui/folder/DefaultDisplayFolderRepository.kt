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
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import com.fsck.k9.mail.FolderType as LegacyFolderType

class DefaultDisplayFolderRepository(
    private val accountManager: LegacyAccountDtoManager,
    private val messagingController: MessagingControllerRegistry,
    private val messageStoreManager: MessageStoreManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : DisplayFolderRepository {
    private val sortForDisplay =
        compareByDescending<DisplayFolder> { it.folder.type == FolderType.INBOX }
            .thenByDescending { it.folder.type == FolderType.OUTBOX }
            .thenByDescending { it.folder.type != FolderType.REGULAR }
            .thenByDescending { it.isInTopGroup }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.folder.name }

    private fun getDisplayFolders(
        account: LegacyAccountDto,
        outboxFolderId: Long,
        includeHiddenFolders: Boolean,
    ): List<DisplayFolder> {
        val messageStore = messageStoreManager.getMessageStore(account.uuid)
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
        account: LegacyAccountDto,
        includeHiddenFolders: Boolean,
    ): Flow<List<DisplayFolder>> {
        val messageStore = messageStoreManager.getMessageStore(account.uuid)

        return callbackFlow {
            val outboxFolderId = outboxFolderManager.getOutboxFolderId(account.id)
            send(getDisplayFolders(account, outboxFolderId, includeHiddenFolders))

            val folderStatusChangedListener = object : SimpleMessagingListener() {
                override fun folderStatusChanged(statusChangedAccount: LegacyAccountDto, folderId: Long) {
                    if (statusChangedAccount.uuid == account.uuid) {
                        trySendBlocking(getDisplayFolders(account, outboxFolderId, includeHiddenFolders))
                    }
                }
            }
            messagingController.addListener(folderStatusChangedListener)

            val folderSettingsChangedListener = FolderSettingsChangedListener {
                trySendBlocking(getDisplayFolders(account, outboxFolderId, includeHiddenFolders))
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
