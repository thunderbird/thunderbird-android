package net.thunderbird.feature.notification.impl.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentSet
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialog
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialogFragmentActionListener
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialogFragmentFactory
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle
import org.koin.android.ext.android.inject

internal class ErrorNotificationsDialogFragment : DialogFragment() {
    private val stream: InAppNotificationStream by inject()
    private var listener: ErrorNotificationsDialogFragmentActionListener? = null

    private val themeProvider: FeatureThemeProvider by inject<FeatureThemeProvider>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return ComposeView(requireContext()).apply {
            setContent {
                themeProvider.WithTheme {
                    val inAppNotifications by stream.notifications.collectAsStateWithLifecycle()
                    val visuals by remember {
                        derivedStateOf {
                            inAppNotifications
                                .asSequence()
                                .filter { it.inAppNotificationStyle is InAppNotificationStyle.BannerInlineNotification }
                                .mapNotNull { BannerInlineVisual.from(it) }
                                .toPersistentSet()
                        }
                    }
                    ErrorNotificationsDialog(
                        visuals = visuals,
                        onDismiss = { dismiss() },
                        onNotificationActionClick = { notificationAction ->
                            dismiss()
                            listener?.onNotificationActionClick(notificationAction)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? ErrorNotificationsDialogFragmentActionListener
            ?: context as? ErrorNotificationsDialogFragmentActionListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object Factory : ErrorNotificationsDialogFragmentFactory {
        private const val TAG = "ErrorNotificationsDialogFragment"

        override fun show(fragmentManager: FragmentManager) {
            ErrorNotificationsDialogFragment().show(fragmentManager, TAG)
        }
    }
}
