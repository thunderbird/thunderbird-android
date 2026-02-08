package com.fsck.k9.ui.settings.notificationactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.fsck.k9.ui.R
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
import net.thunderbird.core.preference.update
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject

/**
 * Lets users reorder notification actions and position the cutoff line.
 */
class NotificationActionsSettingsFragment : androidx.fragment.app.Fragment() {
    private val generalSettingsManager: GeneralSettingsManager by inject()
    private val themeProvider: FeatureThemeProvider by inject()

    private var actionOrder: MutableList<MessageNotificationAction> = MessageNotificationAction
        .defaultOrder()
        .toMutableList()
    private var cutoff: Int = NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        initializeStateFromPreferences()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme {
                    NotificationActionsSettingsScreen(
                        description = stringResource(R.string.notification_actions_settings_description),
                        initialActionOrder = actionOrder,
                        initialCutoff = cutoff,
                        onItemsChanged = ::onItemsChanged,
                    )
                }
            }
        }
    }

    private fun initializeStateFromPreferences() {
        val notificationPrefs = generalSettingsManager.getConfig().notification

        actionOrder = parseOrder(notificationPrefs.messageActionsOrder).toMutableList()
        cutoff = notificationPrefs.messageActionsCutoff.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
    }

    private fun persist() {
        val sanitizedCutoff = cutoff.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
        generalSettingsManager.update { settings ->
            settings.copy(
                notification = settings.notification.copy(
                    messageActionsOrder = actionOrder.map { it.token },
                    messageActionsCutoff = sanitizedCutoff,
                ),
            )
        }
    }

    private fun onItemsChanged(actions: List<MessageNotificationAction>, cutoffIndex: Int) {
        actionOrder = actions.toMutableList()
        cutoff = cutoffIndex.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
        persist()
    }

    private fun parseOrder(tokens: List<String>): List<MessageNotificationAction> {
        val seen = LinkedHashSet<MessageNotificationAction>()
        for (token in tokens) {
            MessageNotificationAction.fromToken(token)?.let { seen.add(it) }
        }

        for (action in MessageNotificationAction.defaultOrder()) {
            seen.add(action)
        }

        return seen.toList()
    }

}
