package net.thunderbird.feature.notification.api.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarHost
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarHostState
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.rememberSnackbarHostState
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.designsystem.template.ScaffoldFabPosition
import kotlinx.collections.immutable.ImmutableSet
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffoldDefaults.TEST_TAG_ERROR_NOTIFICATIONS_DIALOG
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffoldDefaults.TEST_TAG_INNER_SCAFFOLD
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffoldDefaults.TEST_TAG_IN_APP_NOTIFICATION_HOST
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffoldDefaults.TEST_TAG_SNACKBAR_HOST
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialog
import net.thunderbird.feature.notification.api.ui.host.DisplayInAppNotificationFlag
import net.thunderbird.feature.notification.api.ui.host.rememberInAppNotificationHostStateHolder
import net.thunderbird.feature.notification.api.ui.host.visual.SnackbarVisual
import net.thunderbird.feature.notification.api.ui.style.SnackbarDuration
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarDuration as UiSnackbarDuration

/**
 * A scaffold that displays in-app notifications.
 *
 * This composable function is a wrapper around [Scaffold] that adds support for displaying in-app notifications.
 * It uses an [InAppNotificationHost] to display the notifications.
 *
 * @param modifier the modifier to apply to this layout.
 * @param enabled a set of [DisplayInAppNotificationFlag] that determines which types of notifications are displayed.
 * @param topBar top app bar of the screen.
 * @param bottomBar bottom bar of the screen.
 * @param snackbarHostState the [SnackbarHostState] used to show snackbars.
 * @param floatingActionButton the main action button of the screen.
 * @param floatingActionButtonPosition the position of the floating action button.
 * @param onNotificationActionClick invoked when an in-app notification action is clicked.
 * @param content content of the screen. The lambda receives a [PaddingValues] that should be
 *   applied to the content root via [Modifier.padding] and [Modifier.consumeWindowInsets] to
 *   properly offset top and bottom bars. If using [Modifier.verticalScroll], apply this modifier to
 *   the child of the scroll, and not on the scroll itself.
 */
@Composable
fun InAppNotificationScaffold(
    modifier: Modifier = Modifier,
    enabled: ImmutableSet<DisplayInAppNotificationFlag> = DisplayInAppNotificationFlag.AllNotifications,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: ScaffoldFabPosition = ScaffoldFabPosition.End,
    onNotificationActionClick: (NotificationAction) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val hostStateHolder = rememberInAppNotificationHostStateHolder(enabled)
    var showErrorNotificationDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.testTagAsResourceId(TEST_TAG_INNER_SCAFFOLD),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTagAsResourceId(TEST_TAG_SNACKBAR_HOST),
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
    ) { paddingValues ->
        InAppNotificationHost(
            onActionClick = { action ->
                when (action) {
                    NotificationAction.OpenNotificationCentre -> showErrorNotificationDialog = true
                    else -> onNotificationActionClick(action)
                }
            },
            contentPadding = paddingValues,
            hostStateHolder = hostStateHolder,
            onSnackbarNotificationEvent = { visual: SnackbarVisual ->
                snackbarHostState.showSnackbar(
                    message = visual.message,
                    actionLabel = visual.action?.resolveTitle(),
                    duration = when (visual.duration) {
                        SnackbarDuration.Short -> UiSnackbarDuration.Short
                        SnackbarDuration.Long -> UiSnackbarDuration.Long
                        SnackbarDuration.Indefinite -> UiSnackbarDuration.Indefinite
                    },
                )
            },
            modifier = Modifier.testTagAsResourceId(TEST_TAG_IN_APP_NOTIFICATION_HOST),
            content = content,
        )

        if (showErrorNotificationDialog) {
            val state by hostStateHolder.currentInAppNotificationHostState.collectAsStateWithLifecycle()
            ErrorNotificationsDialog(
                visuals = state.bannerInlineVisuals,
                onDismiss = { showErrorNotificationDialog = false },
                onNotificationActionClick = onNotificationActionClick,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TEST_TAG_ERROR_NOTIFICATIONS_DIALOG),
            )
        }
    }
}

internal object InAppNotificationScaffoldDefaults {
    internal const val TEST_TAG_INNER_SCAFFOLD = "ins_inner_scaffold"
    internal const val TEST_TAG_IN_APP_NOTIFICATION_HOST = "ins_in_app_notification_host"
    internal const val TEST_TAG_SNACKBAR_HOST = "ins_snackbar_host"
    internal const val TEST_TAG_ERROR_NOTIFICATIONS_DIALOG = "ins_error_notifications_dialog"
}
