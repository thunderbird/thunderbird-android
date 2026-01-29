package com.fsck.k9.ui.settings.notificationactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
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

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationActionsAdapter

    private var actionOrder: MutableList<MessageNotificationAction> = MessageNotificationAction
        .defaultOrder()
        .toMutableList()
    private var cutoff: Int = NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        initializeStateFromPreferences()
        adapter = NotificationActionsAdapter(
            themeProvider = themeProvider,
            onDragFinished = ::onDragFinished,
        ).apply {
            setItems(buildItems())
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme {
                    val listPaddingPx = with(LocalDensity.current) {
                        MainTheme.spacings.default.roundToPx()
                    }
                    NotificationActionsSettingsScreen(
                        description = stringResource(R.string.notification_actions_settings_description),
                        adapter = adapter,
                        onRecyclerViewReady = { recycler ->
                            recyclerView = recycler
                            recyclerView.layoutManager = LinearLayoutManager(requireContext())
                            recyclerView.adapter = adapter
                            recyclerView.clipToPadding = false
                            recyclerView.setPadding(0, listPaddingPx, 0, listPaddingPx)
                            adapter.attachTo(recyclerView)
                        },
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
        val orderString = actionOrder.joinToString(separator = ",") { it.token }

        generalSettingsManager.update { settings ->
            settings.copy(
                notification = settings.notification.copy(
                    messageActionsOrder = orderString,
                    messageActionsCutoff = sanitizedCutoff,
                ),
            )
        }
    }

    private fun buildItems(): List<NotificationListItem> {
        val clampedCutoff =
            cutoff.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN).coerceAtMost(actionOrder.size)
        return buildList {
            actionOrder.forEachIndexed { index, action ->
                if (index == clampedCutoff) add(NotificationListItem.Cutoff)
                add(
                    NotificationListItem.Action(
                        action = action,
                        isDimmed = index >= clampedCutoff,
                    ),
                )
            }
            if (clampedCutoff == actionOrder.size) add(NotificationListItem.Cutoff)
        }
    }

    private fun renderList() {
        adapter.setItems(buildItems())
    }

    private fun onDragFinished(items: List<NotificationListItem>) {
        actionOrder = items
            .filterIsInstance<NotificationListItem.Action>()
            .map { it.action }
            .toMutableList()
        cutoff = items.indexOfFirst { it is NotificationListItem.Cutoff }
            .coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)
        renderList()
        persist()
    }

    private fun parseOrder(raw: String): List<MessageNotificationAction> {
        val tokens = raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

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

@Composable
private fun NotificationActionsSettingsScreen(
    description: String,
    adapter: NotificationActionsAdapter,
    onRecyclerViewReady: (RecyclerView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TextBodyMedium(
                text = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MainTheme.spacings.double,
                        end = MainTheme.spacings.double,
                        top = MainTheme.spacings.double,
                        bottom = MainTheme.spacings.default,
                    ),
            )
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    RecyclerView(context).also { recyclerView ->
                        onRecyclerViewReady(recyclerView)
                    }
                },
                update = { recyclerView ->
                    if (recyclerView.adapter !== adapter) {
                        onRecyclerViewReady(recyclerView)
                    }
                },
            )
        }
    }
}
