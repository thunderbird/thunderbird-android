package net.thunderbird.feature.notification.api.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.host.visual.BannerGlobalVisual
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual
import net.thunderbird.feature.notification.api.ui.host.visual.InAppNotificationHostState
import net.thunderbird.feature.notification.api.ui.host.visual.InAppNotificationVisual
import net.thunderbird.feature.notification.api.ui.host.visual.SnackbarVisual

@Stable
class InAppNotificationHostStateHolder(private val enabled: DisplayInAppNotificationFlag) {
    internal var currentInAppNotificationHostState by mutableStateOf<InAppNotificationHostState>(
        value = InAppNotificationHostStateImpl(onDismissVisual = ::dismiss),
    )
        private set

    fun showInAppNotification(
        notification: InAppNotification,
    ) {
        println(
            "showInAppNotification() called with: notification = $notification, " +
                "currentInAppNotificationData = $currentInAppNotificationHostState",
        )
        val newData = notification.toInAppNotificationData()
        // TODO(#9572): If global is already present, show the one with the highest priority
        //              show the previous one back once the higher priority has fixed and the
        //              other wasn't
        currentInAppNotificationHostState = newData.bannerGlobalVisual.showIfNeeded(
            ifFlagEnabled = DisplayInAppNotificationFlag.BannerGlobalNotifications,
            select = { bannerGlobalVisual },
            transformIfDifferent = { copy(bannerGlobalVisual = it) },
        )
        currentInAppNotificationHostState = newData.bannerInlineVisuals.showIfNeeded()
        currentInAppNotificationHostState = newData.snackbarVisual.showIfNeeded(
            ifFlagEnabled = DisplayInAppNotificationFlag.SnackbarNotifications,
            select = { snackbarVisual },
            transformIfDifferent = { copy(snackbarVisual = it) },
        )
    }

    private fun ImmutableSet<BannerInlineVisual>.showIfNeeded(): InAppNotificationHostState {
        if (!isEnabled(flag = DisplayInAppNotificationFlag.BannerInlineNotifications)) {
            return currentInAppNotificationHostState
        }
        val new = this
        val current = currentInAppNotificationHostState as InAppNotificationHostStateImpl

        return if (!current.bannerInlineVisuals.containsAll(new)) {
            current.copy(
                bannerInlineVisuals = (current.bannerInlineVisuals + new).toPersistentSet(),
            )
        } else {
            current
        }
    }

    private inline fun <TVisual : InAppNotificationVisual> TVisual?.showIfNeeded(
        ifFlagEnabled: DisplayInAppNotificationFlag,
        select: InAppNotificationHostState.() -> TVisual?,
        transformIfDifferent: InAppNotificationHostStateImpl.(TVisual) -> InAppNotificationHostState,
    ): InAppNotificationHostState {
        if (!isEnabled(ifFlagEnabled)) {
            return currentInAppNotificationHostState
        }
        val new = this
        val current = currentInAppNotificationHostState as InAppNotificationHostStateImpl
        return if (new != null && new != current.select()) {
            current.transformIfDifferent(new)
        } else {
            current
        }
    }

    private fun dismiss(visual: InAppNotificationVisual) {
        println("dismiss() called with: visual = $visual")
        val current = currentInAppNotificationHostState as InAppNotificationHostStateImpl

        currentInAppNotificationHostState = current.copy(
            bannerGlobalVisual = visual.takeIfDifferent(otherwise = current.bannerGlobalVisual),
            bannerInlineVisuals = (visual as? BannerInlineVisual)?.let { bannerInlineVisual ->
                (current.bannerInlineVisuals - bannerInlineVisual).toPersistentSet()
            } ?: current.bannerInlineVisuals,
            snackbarVisual = visual.takeIfDifferent(otherwise = current.snackbarVisual),
        )
    }

    private inline fun <reified T : InAppNotificationVisual> InAppNotificationVisual?.takeIfDifferent(
        otherwise: T?,
    ): T? {
        val current = (this as? T?) ?: otherwise
        return if (this == current) current else otherwise
    }

    private fun isEnabled(flag: DisplayInAppNotificationFlag): Boolean {
        return enabled and flag == flag
    }

    private data class InAppNotificationHostStateImpl(
        override val bannerGlobalVisual: BannerGlobalVisual? = null,
        override val bannerInlineVisuals: ImmutableSet<BannerInlineVisual> = persistentSetOf(),
        override val snackbarVisual: SnackbarVisual? = null,
        private val onDismissVisual: (InAppNotificationVisual) -> Unit = {},
    ) : InAppNotificationHostState {
        override fun dismiss(visual: InAppNotificationVisual) {
            println("dismiss() called with: visual = $visual")
            onDismissVisual(visual)
        }
    }

    private fun InAppNotification.toInAppNotificationData(): InAppNotificationHostState =
        InAppNotificationHostStateImpl(
            bannerGlobalVisual = BannerGlobalVisual.from(notification = this),
            bannerInlineVisuals = BannerInlineVisual.from(notification = this).toPersistentSet(),
            snackbarVisual = SnackbarVisual.from(notification = this),
        )

    override fun toString(): String {
        return "InAppNotificationHostState(" +
            "enabled=$enabled, currentInAppNotificationData=$currentInAppNotificationHostState)"
    }
}

@Composable
fun rememberInAppNotificationHostState(
    enabled: DisplayInAppNotificationFlag = DisplayInAppNotificationFlag.AllNotifications,
): InAppNotificationHostStateHolder {
    return remember { InAppNotificationHostStateHolder(enabled) }
}
