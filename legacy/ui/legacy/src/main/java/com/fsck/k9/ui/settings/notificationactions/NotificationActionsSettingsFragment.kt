package com.fsck.k9.ui.settings.notificationactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.fsck.k9.ui.R
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF
import net.thunderbird.core.preference.update
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject

/**
 * Lets users reorder notification actions and position the cutoff line.
 */
class NotificationActionsSettingsFragment : Fragment() {
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
                        initialActions = actionOrder.toImmutableList(),
                        initialCutoff = cutoff,
                        onStateChanged = ::onStateChanged,
                    )
                }
            }
        }
    }

    private fun initializeStateFromPreferences() {
        val notificationPrefs = generalSettingsManager.getConfig().notification

        actionOrder = parseOrder(notificationPrefs.messageActionsOrder).toMutableList()
        cutoff = notificationPrefs.messageActionsCutoff
    }

    private fun persist() {
        generalSettingsManager.update { settings ->
            settings.copy(
                notification = settings.notification.copy(
                    messageActionsOrder = actionOrder.map { it.token },
                    messageActionsCutoff = cutoff,
                ),
            )
        }
    }

    private fun onStateChanged(
        actions: List<MessageNotificationAction>,
        cutoff: Int,
    ) {
        actionOrder = actions.toMutableList()
        this.cutoff = cutoff
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
