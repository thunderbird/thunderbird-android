package com.fsck.k9.notification

import net.thunderbird.core.android.common.provider.NotificationIconResourceProvider

class TestNotificationIconResourceProvider(
    override val pushNotificationIcon: Int = 9999,
) : NotificationIconResourceProvider
