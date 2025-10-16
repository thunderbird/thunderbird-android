package net.thunderbird.feature.navigation.drawer.siderail.domain.usecase

import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessageCountsProvider
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.navigation.drawer.siderail.domain.DomainContract
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayAccount

internal class GetDisplayAccounts(
    private val accountManager: LegacyAccountDtoManager,
    private val messageCountsProvider: MessageCountsProvider,
    private val messageListRepository: MessageListRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : DomainContract.UseCase.GetDisplayAccounts {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<List<DisplayAccount>> {
        return accountManager.getAccountsFlow()
            .flatMapLatest { accounts ->
                val messageCountsFlows: List<Flow<MessageCounts>> = accounts.map { account ->
                    getMessageCountsFlow(account)
                }

                combine(messageCountsFlows) { messageCountsList ->
                    messageCountsList.mapIndexed { index, messageCounts ->
                        DisplayAccount(
                            id = accounts[index].uuid,
                            name = accounts[index].displayName,
                            email = accounts[index].email,
                            color = accounts[index].chipColor,
                            unreadMessageCount = messageCounts.unread,
                            starredMessageCount = messageCounts.starred,
                        )
                    }
                }
            }
    }

    private fun getMessageCountsFlow(account: LegacyAccountDto): Flow<MessageCounts> {
        return callbackFlow {
            send(messageCountsProvider.getMessageCounts(account))

            val listener = MessageListChangedListener {
                launch {
                    send(messageCountsProvider.getMessageCounts(account))
                }
            }
            messageListRepository.addListener(account.uuid, listener)

            awaitClose {
                messageListRepository.removeListener(listener)
            }
        }.flowOn(coroutineContext)
    }
}
