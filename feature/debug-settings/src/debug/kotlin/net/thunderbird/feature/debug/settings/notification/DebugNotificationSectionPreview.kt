package net.thunderbird.feature.debug.settings.notification

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.components.ui.bolt.PreviewWithThemeLightDark
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.notification.api.content.MailNotification

@OptIn(ExperimentalUuidApi::class)
@PreviewLightDark
@Composable
private fun DebugNotificationSectionPreview() {
    PreviewWithThemeLightDark {
        val accounts = remember {
            List(size = 10) {
                val id = AccountIdFactory.create()
                AccountProfile(
                    id = id,
                    name = "Account $it",
                    color = 0,
                    avatar = Avatar.Monogram(accountId = id, value = "A$it"),
                )
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
            modifier = Modifier.padding(BoltTheme.spacings.triple),
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
                val id = AccountIdFactory.create()
                AccountProfile(
                    id = id,
                    name = "Account $it",
                    color = 0,
                    avatar = Avatar.Monogram(accountId = id, value = "A$it"),
                )
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
            modifier = Modifier.padding(BoltTheme.spacings.triple),
            onAccountSelect = { state = state.copy(selectedAccount = it) },
        )
    }
}
