package net.thunderbird.feature.debugSettings.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.mvi.observeWithoutEffect
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.feature.debugSettings.DebugSection
import net.thunderbird.feature.debugSettings.DebugSubSection
import net.thunderbird.feature.debugSettings.R
import net.thunderbird.feature.debugSettings.notification.DebugNotificationSectionContract.Event
import net.thunderbird.feature.debugSettings.notification.DebugNotificationSectionContract.ViewModel
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.notification.api.content.MailNotification
import net.thunderbird.feature.notification.api.content.Notification
import org.koin.androidx.compose.koinViewModel

private const val UUID_MAX_CHAR_DISPLAY = 4

@Composable
internal fun DebugNotificationSection(
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<DebugNotificationSectionViewModel>(),
) {
    val (state, dispatchEvent) = viewModel.observeWithoutEffect()

    DebugNotificationSection(
        state = state.value,
        modifier = modifier,
        onAccountSelect = { account ->
            dispatchEvent(Event.SelectAccount(account))
        },
        onOptionChange = { notificationType ->
            dispatchEvent(Event.SelectNotificationType(notificationType))
        },
        onTriggerSystemNotificationClick = { dispatchEvent(Event.TriggerSystemNotification) },
        onTriggerInAppNotificationClick = { dispatchEvent(Event.TriggerInAppNotification) },
        onSenderChange = { dispatchEvent(Event.OnSenderChange(it)) },
        onSubjectChange = { dispatchEvent(Event.OnSubjectChange(it)) },
        onSummaryChange = { dispatchEvent(Event.OnSummaryChange(it)) },
        onPreviewChange = { dispatchEvent(Event.OnPreviewChange(it)) },
    )
}

@Composable
internal fun DebugNotificationSection(
    state: DebugNotificationSectionContract.State,
    modifier: Modifier = Modifier,
    onAccountSelect: (BaseAccount) -> Unit = {},
    onOptionChange: (KClass<out Notification>) -> Unit = {},
    onTriggerSystemNotificationClick: () -> Unit = {},
    onTriggerInAppNotificationClick: () -> Unit = {},
    onSenderChange: (String) -> Unit = {},
    onSubjectChange: (String) -> Unit = {},
    onSummaryChange: (String) -> Unit = {},
    onPreviewChange: (String) -> Unit = {},
) {
    DebugSection(
        title = stringResource(R.string.debug_settings_notifications_title),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quadruple),
        ) {
            CommonNotificationInformation(state, onAccountSelect)
            SystemNotificationSection(
                state = state,
                onOptionChange = onOptionChange,
                onClick = onTriggerSystemNotificationClick,
                onSenderChange = onSenderChange,
                onSubjectChange = onSubjectChange,
                onSummaryChange = onSummaryChange,
                onPreviewChange = onPreviewChange,
            )
            InAppNotificationSection(
                selectedNotificationType = state.selectedInAppNotificationType,
                options = state.inAppNotificationTypes,
                onOptionChange = onOptionChange,
                onClick = onTriggerInAppNotificationClick,
            )
        }
    }
}

@Composable
private fun CommonNotificationInformation(
    state: DebugNotificationSectionContract.State,
    onAccountSelect: (BaseAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugSubSection(
        title = stringResource(R.string.debug_settings_notifications_common_notification_information),
        modifier = modifier.padding(start = MainTheme.spacings.double),
    ) {
        val loadingText = stringResource(R.string.debug_settings_notifications_loading)
        SelectInput(
            options = state.accounts,
            selectedOption = state.selectedAccount,
            onOptionChange = { account ->
                account?.let(onAccountSelect)
            },
            optionToStringTransformation = { account ->
                account?.let { account ->
                    val uuidStart = account.uuid.take(UUID_MAX_CHAR_DISPLAY)
                    val uuidEnd = account.uuid.take(UUID_MAX_CHAR_DISPLAY)
                    val accountDisplay = account.name ?: account.email
                    "$uuidStart..$uuidEnd - $accountDisplay"
                } ?: loadingText
            },
        )
    }
}

@Composable
private fun SystemNotificationSection(
    state: DebugNotificationSectionContract.State,
    onOptionChange: (KClass<out Notification>) -> Unit,
    onClick: () -> Unit,
    onSenderChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onSummaryChange: (String) -> Unit,
    onPreviewChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugSubSection(
        title = stringResource(R.string.debug_settings_notifications_system_notification),
        modifier = modifier.padding(start = MainTheme.spacings.double),
    ) {
        Column {
            TriggerNotificationSection(
                selectedNotificationType = state.selectedSystemNotificationType,
                options = state.systemNotificationTypes,
                onOptionChange = onOptionChange,
                onClick = onClick,
            )
            AnimatedVisibility(state.selectedSystemNotificationType == MailNotification.NewMail.SingleMail::class) {
                Column {
                    TextInput(
                        onTextChange = onSenderChange,
                        text = state.singleNotificationData.sender,
                        label = stringResource(R.string.debug_settings_notifications_single_mail_sender),
                    )
                    TextInput(
                        onTextChange = onSubjectChange,
                        text = state.singleNotificationData.subject,
                        label = stringResource(R.string.debug_settings_notifications_single_mail_subject),
                    )
                    TextInput(
                        onTextChange = onSummaryChange,
                        text = state.singleNotificationData.summary,
                        label = stringResource(R.string.debug_settings_notifications_single_mail_summary),
                    )
                    TextInput(
                        onTextChange = onPreviewChange,
                        text = state.singleNotificationData.preview,
                        label = stringResource(R.string.debug_settings_notifications_single_mail_preview),
                    )
                }
            }
        }
    }
}

@Composable
private fun InAppNotificationSection(
    selectedNotificationType: KClass<out Notification>?,
    options: ImmutableList<KClass<out Notification>>,
    onOptionChange: (KClass<out Notification>) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugSubSection(
        title = stringResource(R.string.debug_settings_notifications_in_app_notification),
        modifier = modifier.padding(start = MainTheme.spacings.double),
    ) {
        TriggerNotificationSection(
            selectedNotificationType = selectedNotificationType,
            options = options,
            onOptionChange = onOptionChange,
            onClick = onClick,
        )
    }
}

@Composable
private fun TriggerNotificationSection(
    selectedNotificationType: KClass<out Notification>?,
    options: ImmutableList<KClass<out Notification>>,
    onOptionChange: (KClass<out Notification>) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.oneHalf),
        modifier = modifier,
    ) {
        val selectedOption = remember(selectedNotificationType, options) {
            selectedNotificationType ?: options.firstOrNull()
        }
        val loadingText = stringResource(R.string.debug_settings_notifications_loading)
        SelectInput(
            options = options,
            selectedOption = selectedOption,
            onOptionChange = { it?.let(onOptionChange) },
            optionToStringTransformation = { kClass -> kClass?.realName ?: loadingText },
        )

        ButtonFilled(
            text = stringResource(R.string.debug_settings_notifications_trigger_notification),
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
