package net.thunderbird.feature.notification.api.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.coroutineScope
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.receiver.InAppNotificationReceiver
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.host.DisplayInAppNotificationFlag
import net.thunderbird.feature.notification.api.ui.host.InAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.host.rememberInAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.host.visual.SnackbarVisual
import net.thunderbird.feature.notification.api.ui.layout.InAppNotificationHostLayout
import net.thunderbird.feature.notification.api.ui.layout.rememberBannerInlineScrollBehaviour
import org.koin.compose.koinInject

/**
 * Host used to properly show, hide and dismiss in-app notifications listening events from
 * [InAppNotificationReceiver].
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
    eventFilter: (InAppNotificationEvent) -> Boolean = { true },
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
 * Host used to properly show, hide and dismiss in-app notifications listening events from
 * [InAppNotificationReceiver].
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
    eventFilter: (InAppNotificationEvent) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit,
) {
    val receiver = koinInject<InAppNotificationReceiver>()
    val state by hostStateHolder.currentInAppNotificationHostState.collectAsState()

    LifecycleStartEffect(receiver, eventFilter) {
        val job = lifecycle.coroutineScope.launch {
            receiver
                .events
                .filter(eventFilter)
                .collect { event ->
                    when (event) {
                        is InAppNotificationEvent.Dismiss -> hostStateHolder.dismiss(event.notification)
                        is InAppNotificationEvent.Show -> hostStateHolder.showInAppNotification(event.notification)
                    }
                }
        }
        onStopOrDispose { job.cancel() }
    }

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
