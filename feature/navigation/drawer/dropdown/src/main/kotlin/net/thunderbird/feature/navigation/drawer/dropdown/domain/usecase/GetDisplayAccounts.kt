package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.notification.api.content.AuthenticationErrorNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream

internal class GetDisplayAccounts(
    private val accountManager: LegacyAccountDtoManager,
    private val messageCountsProvider: MessageCountsProvider,
    private val messageListRepository: MessageListRepository,
    private val notificationStream: InAppNotificationStream,
    private val featureFlagProvider: FeatureFlagProvider,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : UseCase.GetDisplayAccounts {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(showUnifiedAccount: Boolean): Flow<List<DisplayAccount>> {
        return accountManager
            .getAccountsFlow()
            .flatMapLatest { accounts -> accounts.associateWithAuthErrorIndication() }
            .flatMapLatest { accountsMap ->
                val accounts = accountsMap.keys.toList()
                val messageCountsFlows: List<Flow<MessageCounts>> = accounts.map { account ->
                    getMessageCountsFlow(account)
                }

                combine(messageCountsFlows) { messageCountsList ->
                    val displayAccounts = messageCountsList.mapIndexed { index, messageCounts ->
                        val account = accounts[index]
                        MailDisplayAccount(
                            id = account.uuid,
                            name = account.displayName,
                            email = account.email,
                            color = account.chipColor,
                            unreadMessageCount = messageCounts.unread,
                            starredMessageCount = messageCounts.starred,
                            hasError = accountsMap[account] == true,
                        )
                    }

                    if (showUnifiedAccount) {
                        withUnifiedAccount(displayAccounts)
                    } else {
                        displayAccounts
                    }
                }
            }
    }

    private fun List<LegacyAccountDto>.associateWithAuthErrorIndication(): Flow<Map<LegacyAccountDto, Boolean>> {
        return if (featureFlagProvider.provide(FeatureFlagKey.DisplayInAppNotifications).isDisabledOrUnavailable()) {
            flowOf(associateWith { false })
        } else {
            val uuids = map { it.uuid }
            notificationStream
                .notifications
                .map { notifications ->
                    notifications
                        .filter { it is AuthenticationErrorNotification && it.accountUuid in uuids }
                        .associateBy { it.accountUuid }
                }
                .map { notifications ->
                    associateWith { account -> notifications[account.uuid] != null }
                }
        }
    }

    private fun withUnifiedAccount(accounts: List<DisplayAccount>): List<DisplayAccount> {
        val unified = UnifiedDisplayAccount(
            unreadMessageCount = accounts.sumOf { it.unreadMessageCount },
            starredMessageCount = accounts.sumOf { it.starredMessageCount },
            hasError = accounts.any { it.hasError },
        )

        return listOf(unified) + accounts
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
