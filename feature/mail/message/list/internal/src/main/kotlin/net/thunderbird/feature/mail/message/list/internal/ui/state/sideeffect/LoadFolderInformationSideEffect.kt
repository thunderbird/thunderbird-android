package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import androidx.compose.ui.graphics.Color
import app.k9mail.legacy.mailstore.FolderRepository
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.ui.MessageListStateSideEffectHandlerFactory
import net.thunderbird.feature.mail.message.list.ui.event.FolderEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "LoadFolderInformationSideEffect"

class LoadFolderInformationSideEffect(
    private val accountIds: Set<AccountId>,
    private val folderId: Long?,
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val folderRepository: FolderRepository,
) : StateSideEffectHandler<MessageListState, MessageListEvent>(logger, dispatch) {
    override fun accept(
        event: MessageListEvent,
        newState: MessageListState,
    ): Boolean = accountIds.size == 1 && folderId != null && event == MessageListEvent.LoadConfigurations

    override suspend fun handle(oldState: MessageListState, newState: MessageListState) {
        val accountId = accountIds.first()
        val folderId = requireNotNull(folderId)
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        val folder = folderRepository.getFolder(accountId, folderId)
        if (folder != null) {
            val remoteFolder = if (!folder.isLocalOnly) {
                folderRepository.getRemoteFolders(accountId).first { it.id == folderId }
            } else {
                null
            }

            dispatch(
                FolderEvent.FolderLoaded(
                    folder = Folder(
                        id = remoteFolder?.serverId ?: "local_folder",
                        account = Account(id = accountId, color = Color.Unspecified), // TODO: fetch color
                        name = folder.name,
                        type = folder.type,
                    ),
                ),
            )
        }
    }

    class Factory(
        private val accountIds: Set<AccountId>,
        private val folderId: Long?,
        private val logger: Logger,
        private val folderRepository: FolderRepository,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
        ): StateSideEffectHandler<MessageListState, MessageListEvent> = LoadFolderInformationSideEffect(
            accountIds = accountIds,
            folderId = folderId,
            dispatch = dispatch,
            logger = logger,
            folderRepository = folderRepository,
        )
    }
}
