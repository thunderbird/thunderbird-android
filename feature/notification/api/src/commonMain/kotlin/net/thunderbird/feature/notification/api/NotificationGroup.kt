package net.thunderbird.feature.notification.api

import net.thunderbird.core.common.io.KmpParcelable
import net.thunderbird.core.common.io.KmpParcelize

// TODO(9419): Properly handle notification groups, adding summary, etc.
@KmpParcelize
data class NotificationGroup(
    val key: NotificationGroupKey,
    val summary: String,
    val alertBehaviour: NotificationGroupAlertBehaviour = NotificationGroupAlertBehaviour.AlertSummary,
) : KmpParcelable

enum class NotificationGroupAlertBehaviour {
    AlertAll,
    AlertSummary,
    AlertChildren,
    Silent,
}
