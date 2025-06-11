package net.thunderbird.feature.notification.api

import net.thunderbird.core.common.io.KmpParcelable
import net.thunderbird.core.common.io.KmpParcelize

// TODO: Properly handle notification groups, adding summary, etc.
@KmpParcelize
data class NotificationGroup(
    val key: NotificationGroupKey,
    val summary: String,
) : KmpParcelable
