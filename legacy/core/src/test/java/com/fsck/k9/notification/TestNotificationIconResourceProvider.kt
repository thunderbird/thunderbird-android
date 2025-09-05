package com.fsck.k9.notification

import app.k9mail.core.android.common.provider.NotificationIconResourceProvider

class TestNotificationIconResourceProvider(
    override val pushNotificationIcon: Int = 9999,
) : NotificationIconResourceProvider
