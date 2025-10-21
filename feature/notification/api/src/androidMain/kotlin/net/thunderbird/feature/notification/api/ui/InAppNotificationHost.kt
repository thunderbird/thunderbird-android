package net.thunderbird.feature.notification.api.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import kotlinx.collections.immutable.ImmutableSet
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.host.DisplayInAppNotificationFlag
import net.thunderbird.feature.notification.api.ui.host.InAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.host.rememberInAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.host.visual.SnackbarVisual
import net.thunderbird.feature.notification.api.ui.layout.InAppNotificationHostLayout
import net.thunderbird.feature.notification.api.ui.layout.rememberBannerInlineScrollBehaviour
import org.koin.compose.koinInject

/**
 * Host used to properly show and dismiss in-app notifications listening notification changes using
 * [InAppNotificationReceiverEffect].
 *
 * This component takes in consideration the [InAppNotificationHostStateHolder]'s enabled flags to
 * decide which kind of notifications to show.
 *
 * Additionally, the events can be filtered by passing a [eventFilter] lambda.
 *
 * @param onActionClick invoked when a notification action is clicked.
 * @param modifier the modifier to apply to this layout.
 * @param enabled a set of [DisplayInAppNotificationFlag] that determines which types of notifications are displayed.
 * @param contentPadding a padding around the whole content.
 * @param onSnackbarNotificationEvent invoked when a snackbar notification event is triggered. This is
 *  required as the snackbar component can be shared among other screens via [InAppNotificationScaffold] or [Scaffold]
 * @param eventFilter a lambda to filter in-app notification events. Only events for which this lambda returns
 * true will be processed.
 * @param content a block which describes the content. This composable will rearrange the content to properly
 * display the in-app notifications.
 */
@Composable
fun InAppNotificationHost(
    onActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
    enabled: ImmutableSet<DisplayInAppNotificationFlag> = DisplayInAppNotificationFlag.AllNotifications,
    contentPadding: PaddingValues = PaddingValues(),
    onSnackbarNotificationEvent: suspend (SnackbarVisual) -> Unit = {},
    eventFilter: (InAppNotification) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit = {},
) {
    InAppNotificationHost(
        onActionClick = onActionClick,
        modifier = modifier,
        contentPadding = contentPadding,
        hostStateHolder = rememberInAppNotificationHostStateHolder(enabled),
        onSnackbarNotificationEvent = onSnackbarNotificationEvent,
        eventFilter = eventFilter,
        content = content,
    )
}

/**
 * Host used to properly show and dismiss in-app notifications listening notification changes using
 * [InAppNotificationReceiverEffect].
 *
 * This component takes in consideration the [InAppNotificationHostStateHolder]'s enabled flags to
 * decide which kind of notifications to show.
 *
 * Additionally, the events can be filtered by passing a [eventFilter] lambda.
 *
 * @param onActionClick invoked when a notification action is clicked.
 * @param modifier the modifier to apply to this layout.
 * @param hostStateHolder the state holder for the in-app notification host.
 * @param contentPadding a padding around the whole content.
 * @param onSnackbarNotificationEvent invoked when a snackbar notification event is triggered. This is
 *  required as the snackbar component can be shared among other screens via [InAppNotificationScaffold] or [Scaffold]
 * @param eventFilter a lambda to filter in-app notification events. Only events for which this lambda returns
 * true will be processed.
 * @param content a block which describes the content. This composable will rearrange the content to properly
 * display the in-app notifications.
 */
@Composable
fun InAppNotificationHost(
    onActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
    hostStateHolder: InAppNotificationHostStateHolder = rememberInAppNotificationHostStateHolder(
        enabled = DisplayInAppNotificationFlag.AllNotifications,
    ),
    contentPadding: PaddingValues = PaddingValues(),
    onSnackbarNotificationEvent: suspend (SnackbarVisual) -> Unit = {},
    eventFilter: (InAppNotification) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit,
) {
    val state by hostStateHolder.currentInAppNotificationHostState.collectAsState()
    InAppNotificationReceiverEffect(eventFilter, hostStateHolder)

    LaunchedEffect(state.snackbarVisual, onSnackbarNotificationEvent) {
        val snackbarVisual = state.snackbarVisual
        if (snackbarVisual != null) {
            onSnackbarNotificationEvent(snackbarVisual)
            hostStateHolder.dismiss(snackbarVisual)
        }
    }

    InAppNotificationHostLayout(
        behaviour = rememberBannerInlineScrollBehaviour(),
        scaffoldPaddingValues = contentPadding,
        bannerGlobal = {
            BannerGlobalNotificationHost(
                hostStateHolder = hostStateHolder,
                onActionClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        bannerInlineList = {
            BannerInlineNotificationListHost(
                hostStateHolder = hostStateHolder,
                onActionClick = onActionClick,
                onOpenErrorNotificationsClick = { onActionClick(NotificationAction.OpenNotificationCentre) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        content = content,
        modifier = modifier,
    )
}

@Composable
private fun InAppNotificationReceiverEffect(
    eventFilter: (InAppNotification) -> Boolean,
    hostStateHolder: InAppNotificationHostStateHolder,
) {
    val registry = koinInject<NotificationRegistry>()
    val inAppNotifications by registry.registrar.collectAsStateWithLifecycle()
    var pastNotifications by remember { mutableStateOf<Set<InAppNotification>>(emptySet()) }

    LaunchedEffect(inAppNotifications, eventFilter) {
        val newNotifications = inAppNotifications
            .values
            .asSequence()
            .filterIsInstance<InAppNotification>()
            .filter(eventFilter)
            .toSet()
            .onEach { notification ->
                hostStateHolder.showInAppNotification(notification)
            }

        // dismiss notifications that are not in the new list
        pastNotifications
            .filterNot { it in newNotifications }
            .forEach { notification ->
                hostStateHolder.dismiss(notification)
            }

        pastNotifications = newNotifications
    }
}
