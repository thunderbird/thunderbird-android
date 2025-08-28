package net.thunderbird.feature.debug.settings.notification

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.notification.api.content.MailNotification

@OptIn(ExperimentalUuidApi::class)
@PreviewLightDark
@Composable
private fun DebugNotificationSectionPreview() {
    PreviewWithThemeLightDark {
        val accounts = remember {
            List(size = 10) {
                object : BaseAccount {
                    override val uuid: String = Uuid.random().toString()
                    override val name: String? = "Account $it"
                    override val email: String = "account-$it@mail.com"
                }
            }.toPersistentList()
        }
        var state by remember {
            mutableStateOf(
                DebugNotificationSectionContract.State(
                    accounts = accounts,
                    selectedAccount = accounts.first(),
                ),
            )
        }
        DebugNotificationSection(
            state = state,
            modifier = Modifier.padding(MainTheme.spacings.triple),
            onAccountSelect = { state = state.copy(selectedAccount = it) },
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@PreviewLightDark
@Composable
private fun PreviewSingleMailNotification() {
    PreviewWithThemeLightDark {
        val accounts = remember {
            List(size = 10) {
                object : BaseAccount {
                    override val uuid: String = Uuid.random().toString()
                    override val name: String? = "Account $it"
                    override val email: String = "account-$it@mail.com"
                }
            }.toPersistentList()
        }
        var state by remember {
            mutableStateOf(
                DebugNotificationSectionContract.State(
                    accounts = accounts,
                    selectedAccount = accounts.first(),
                    selectedSystemNotificationType = MailNotification.NewMailSingleMail::class,
                ),
            )
        }
        DebugNotificationSection(
            state = state,
            modifier = Modifier.padding(MainTheme.spacings.triple),
            onAccountSelect = { state = state.copy(selectedAccount = it) },
        )
    }
}
