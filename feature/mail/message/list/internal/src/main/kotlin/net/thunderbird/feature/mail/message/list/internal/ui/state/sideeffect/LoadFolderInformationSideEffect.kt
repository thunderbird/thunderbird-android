package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import androidx.compose.ui.graphics.Color
import app.k9mail.legacy.mailstore.FolderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.FolderEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

private const val TAG = "LoadFolderInformationSideEffect"

internal class LoadFolderInformationSideEffect(
    private val accountIds: Set<AccountId>,
    private val folderId: Long?,
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val folderRepository: FolderRepository,
    private val profileRepository: AccountProfileRepository,
) : MessageListStateSideEffectHandler(logger, dispatch) {
    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        event == MessageListEvent.LoadConfigurations

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        return if (folderId == null) {
            consumeUnifiedFolder()
        } else {
            consumeSingleAccountFolder(folderId)
        }
    }

    private suspend fun consumeUnifiedFolder(): ConsumeResult {
        dispatch(
            FolderEvent.FolderLoaded(
                folder = Folder(
                    id = "unified_inbox",
                    account = Account(id = UnifiedAccountId, color = Color.Unspecified),
                    name = "Unified Inbox",
                    type = FolderType.INBOX,
                ),
            ),
        )

        return ConsumeResult.Consumed
    }

    private suspend fun consumeSingleAccountFolder(folderId: Long): ConsumeResult {
        val accountId = accountIds.first()
        val folder = folderRepository.getFolder(accountId, folderId)
        return if (folder != null) {
            val remoteFolder = if (!folder.isLocalOnly) {
                folderRepository.getRemoteFolders(accountId).first { it.id == folderId }
            } else {
                null
            }

            val profile = profileRepository.getById(accountId).first()
            val color = profile?.color?.let(::Color) ?: Color.Unspecified
            dispatch(
                FolderEvent.FolderLoaded(
                    folder = Folder(
                        id = remoteFolder?.serverId ?: "local_folder",
                        account = Account(id = accountId, color = color),
                        name = folder.name,
                        type = folder.type,
                    ),
                ),
            )
            ConsumeResult.Consumed
        } else {
            ConsumeResult.Ignored
        }
    }

    class Factory(
        private val accountIds: Set<AccountId>,
        private val folderId: Long?,
        private val logger: Logger,
        private val folderRepository: FolderRepository,
        private val profileRepository: AccountProfileRepository,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = LoadFolderInformationSideEffect(
            accountIds = accountIds,
            folderId = folderId,
            dispatch = dispatch,
            logger = logger,
            folderRepository = folderRepository,
            profileRepository = profileRepository,
        )
    }
}
