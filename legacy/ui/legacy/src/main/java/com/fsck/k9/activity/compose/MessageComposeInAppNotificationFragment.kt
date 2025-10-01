package com.fsck.k9.activity.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.collections.immutable.persistentSetOf
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.notification.api.ui.InAppNotificationHost
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.host.DisplayInAppNotificationFlag
import net.thunderbird.feature.notification.api.ui.host.visual.SnackbarVisual
import net.thunderbird.feature.notification.api.ui.style.SnackbarDuration
import org.koin.android.ext.android.inject

private const val TAG = "MessageComposeInAppNotificationFragment"

class MessageComposeInAppNotificationFragment : Fragment() {
    private val themeProvider: FeatureThemeProvider by inject()
    private val logger: Logger by inject()
    private var parentView: View? = null
    private var accountId: AccountId? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { arg ->
            accountId = requireNotNull(arg.getString(ARG_ACCOUNT_ID)?.let { AccountIdFactory.of(it) }) {
                "Argument $ARG_ACCOUNT_ID is required"
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            parentView = container
            setContent {
                themeProvider.WithTheme {
                    InAppNotificationHost(
                        onActionClick = ::onNotificationActionClick,
                        enabled = persistentSetOf(
                            DisplayInAppNotificationFlag.BannerGlobalNotifications,
                            DisplayInAppNotificationFlag.SnackbarNotifications,
                        ),
                        onSnackbarNotificationEvent = ::onSnackbarInAppNotificationEvent,
                        eventFilter = { event ->
                            val accountUuid = event.notification.accountUuid
                            accountUuid != null && accountUuid == accountId?.asRaw()
                        },
                        modifier = Modifier.animateContentSize(),
                    )
                }
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        parentView = null
    }

    private suspend fun onSnackbarInAppNotificationEvent(visual: SnackbarVisual) {
        parentView?.let { view ->
            val (message, action, duration) = visual
            Snackbar.make(
                view,
                message,
                when (duration) {
                    SnackbarDuration.Short -> Snackbar.LENGTH_SHORT
                    SnackbarDuration.Long -> Snackbar.LENGTH_LONG
                    SnackbarDuration.Indefinite -> Snackbar.LENGTH_INDEFINITE
                },
            ).apply {
                if (action != null) {
                    setAction(action.resolveTitle()) {
                        // TODO.
                    }
                }
            }.show()
        }
    }

    private fun onNotificationActionClick(action: NotificationAction) {
        logger.verbose(TAG) { "onNotificationActionClick() called with: action = $action" }
    }

    companion object {
        private const val ARG_ACCOUNT_ID = "MessageComposeInAppNotificationFragment_account_id"
        const val FRAGMENT_TAG = "MessageComposeInAppNotificationFragment"

        fun newInstance(accountId: AccountId): MessageComposeInAppNotificationFragment =
            MessageComposeInAppNotificationFragment().apply {
                arguments = bundleOf(ARG_ACCOUNT_ID to accountId.asRaw())
            }

        @JvmStatic
        fun newInstance(accountUuid: String): MessageComposeInAppNotificationFragment =
            newInstance(AccountIdFactory.of(accountUuid))
    }
}
