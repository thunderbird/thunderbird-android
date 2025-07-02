package net.thunderbird.feature.debugSettings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.debugSettings.notification.DebugNotificationSectionViewModel
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Success
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.sender.NotificationSender

@PreviewLightDark
@Composable
private fun SecretDebugSettingsScreenPreview() {
    koinPreview {
        single<DebugNotificationSectionViewModel> {
            DebugNotificationSectionViewModel(
                accountManager = object : AccountManager<BaseAccount> {
                    override fun getAccounts(): List<BaseAccount> = listOf()
                    override fun getAccountsFlow(): Flow<List<BaseAccount>> = flowOf(listOf())
                    override fun getAccount(accountUuid: String): BaseAccount? = null
                    override fun getAccountFlow(accountUuid: String): Flow<BaseAccount?> = flowOf(null)
                    override fun moveAccount(
                        account: BaseAccount,
                        newPosition: Int,
                    ) = Unit

                    override fun saveAccount(account: BaseAccount) = Unit
                },
                notificationSender = object : NotificationSender {
                    override fun send(
                        notification: Notification,
                    ): Flow<Outcome<Success<Notification>, Failure<Notification>>> =
                        error("not implementd")
                },
            )
        }
    } WithContent {
        PreviewWithThemesLightDark {
            SecretDebugSettingsScreen(
                onNavigateBack = { },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
