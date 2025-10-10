package net.thunderbird.feature.notification.impl.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialog
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialogFragmentActionListener
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialogFragmentFactory
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle
import org.koin.android.ext.android.inject

internal class ErrorNotificationsDialogFragment : DialogFragment() {
    private val registry: NotificationRegistry by inject()
    private var visuals = getVisuals()
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

    override fun onResume() {
        super.onResume()
        visuals = getVisuals()
    }

    private fun getVisuals(): ImmutableSet<BannerInlineVisual> = registry
        .registrar
        .values
        .asSequence()
        .filterIsInstance<InAppNotification>()
        .filter { it.inAppNotificationStyle is InAppNotificationStyle.BannerInlineNotification }
        .mapNotNull { BannerInlineVisual.from(it) }
        .toPersistentSet()

    companion object Factory : ErrorNotificationsDialogFragmentFactory {
        private const val TAG = "ErrorNotificationsDialogFragment"

        override fun show(fragmentManager: FragmentManager) {
            ErrorNotificationsDialogFragment().show(fragmentManager, TAG)
        }
    }
}
