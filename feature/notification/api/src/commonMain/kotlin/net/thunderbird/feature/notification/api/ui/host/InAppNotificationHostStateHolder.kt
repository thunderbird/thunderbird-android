package net.thunderbird.feature.notification.api.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.common.collections.maxPriorityQueueOf
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.host.visual.BannerGlobalVisual
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual
import net.thunderbird.feature.notification.api.ui.host.visual.InAppNotificationHostState
import net.thunderbird.feature.notification.api.ui.host.visual.InAppNotificationVisual
import net.thunderbird.feature.notification.api.ui.host.visual.SnackbarVisual

@Stable
class InAppNotificationHostStateHolder(private val enabled: ImmutableSet<DisplayInAppNotificationFlag>) {
    private val internalState =
        MutableStateFlow<InAppNotificationHostStateImpl>(value = InAppNotificationHostStateImpl())
    internal val currentInAppNotificationHostState: StateFlow<InAppNotificationHostState> = internalState.asStateFlow()
    private val bannerGlobalVisuals = maxPriorityQueueOf<BannerGlobalVisual>()

    fun showInAppNotification(
        notification: InAppNotification,
    ) {
        val newData = notification.toInAppNotificationData()
        if (isEnabled(DisplayInAppNotificationFlag.BannerGlobalNotifications)) {
            newData.bannerGlobalVisual?.let { bannerGlobalVisual ->
                bannerGlobalVisuals.add(bannerGlobalVisual)
                internalState.update { it.copy(bannerGlobalVisual = bannerGlobalVisuals.peek()) }
            }
        }
        internalState.update {
            newData.bannerInlineVisuals.showIfNeeded()
        }
        internalState.update {
            newData.snackbarVisual.showIfNeeded(
                ifFlagEnabled = DisplayInAppNotificationFlag.SnackbarNotifications,
                select = { snackbarVisual },
                transformIfDifferent = { copy(snackbarVisual = it) },
            )
        }
    }

    private fun ImmutableSet<BannerInlineVisual>.showIfNeeded(): InAppNotificationHostStateImpl {
        if (!isEnabled(flag = DisplayInAppNotificationFlag.BannerInlineNotifications)) {
            return internalState.value
        }
        val new = this
        val current = internalState.value

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
        select: InAppNotificationHostStateImpl.() -> TVisual?,
        transformIfDifferent: InAppNotificationHostStateImpl.(TVisual) -> InAppNotificationHostStateImpl,
    ): InAppNotificationHostStateImpl {
        if (!isEnabled(ifFlagEnabled)) {
            return internalState.value
        }
        val new = this
        val current = internalState.value
        return if (new != null && new != current.select()) {
            current.transformIfDifferent(new)
        } else {
            current
        }
    }

    /**
     * Dismisses all visual representations of the given in-app notification.
     *
     * This function will attempt to dismiss the global banner, inline banners,
     * and snackbar associated with the provided notification.
     *
     * @param notification The [InAppNotification] to dismiss.
     */
    fun dismiss(notification: InAppNotification) {
        println("---- InAppNotificationHostStateHolder.dismiss called with notification: $notification")
        val data = notification.toInAppNotificationData()
        data.bannerInlineVisuals.singleOrNull()?.let(::dismiss)
        data.bannerGlobalVisual?.let(::dismiss)
        data.snackbarVisual?.let(::dismiss)
    }

    /**
     * Dismisses the given in-app notification visual.
     *
     * This function is responsible for removing the specified notification visual
     * from the display.
     *
     * @param visual The [InAppNotificationVisual] to dismiss.
     */
    fun dismiss(visual: InAppNotificationVisual) {
        println("---- InAppNotificationHostStateHolder.dismiss called with visual: $visual")
        internalState.update { current ->
            current.copy(
                bannerGlobalVisual = if (visual is BannerGlobalVisual && bannerGlobalVisuals.remove(visual)) {
                    bannerGlobalVisuals.peek()
                } else {
                    current.bannerGlobalVisual
                },
                bannerInlineVisuals = (visual as? BannerInlineVisual)?.let { bannerInlineVisual ->
                    (current.bannerInlineVisuals - bannerInlineVisual).toPersistentSet()
                } ?: current.bannerInlineVisuals,
                snackbarVisual = visual.nullIfDifferent(otherwise = current.snackbarVisual),
            )
        }
    }

    private inline fun <reified T : InAppNotificationVisual> InAppNotificationVisual?.nullIfDifferent(
        otherwise: T?,
    ): T? {
        val current = (this as? T?) ?: otherwise
        return if (this == current) null else otherwise
    }

    private fun isEnabled(flag: DisplayInAppNotificationFlag): Boolean {
        return enabled.any { it == flag } || enabled == DisplayInAppNotificationFlag.AllNotifications
    }

    private data class InAppNotificationHostStateImpl(
        override val bannerGlobalVisual: BannerGlobalVisual? = null,
        override val bannerInlineVisuals: ImmutableSet<BannerInlineVisual> = persistentSetOf(),
        override val snackbarVisual: SnackbarVisual? = null,
    ) : InAppNotificationHostState

    private fun InAppNotification.toInAppNotificationData(): InAppNotificationHostState =
        InAppNotificationHostStateImpl(
            bannerGlobalVisual = BannerGlobalVisual.from(notification = this),
            bannerInlineVisuals = BannerInlineVisual.from(notification = this)
                ?.let { persistentSetOf(it) }
                ?: persistentSetOf(),
            snackbarVisual = SnackbarVisual.from(notification = this),
        )

    override fun toString(): String {
        return "InAppNotificationHostState(" +
            "enabled=$enabled, currentInAppNotificationData=${currentInAppNotificationHostState.value})"
    }
}

@Composable
fun rememberInAppNotificationHostStateHolder(
    enabled: ImmutableSet<DisplayInAppNotificationFlag> = DisplayInAppNotificationFlag.AllNotifications,
): InAppNotificationHostStateHolder {
    return remember { InAppNotificationHostStateHolder(enabled) }
}
