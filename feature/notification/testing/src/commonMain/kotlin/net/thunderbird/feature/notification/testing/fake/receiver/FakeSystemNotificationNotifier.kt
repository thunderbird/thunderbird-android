package net.thunderbird.feature.notification.testing.fake.receiver

import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry

open class FakeSystemNotificationNotifier(
    registry: NotificationRegistry = FakeNotificationRegistry(),
) : BaseFakeNotificationNotifier<SystemNotification>(registry)
