package net.thunderbird.feature.notification.testing.fake.receiver

import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry

open class FakeInAppNotificationNotifier(
    registry: NotificationRegistry = FakeNotificationRegistry(),
) : BaseFakeNotificationNotifier<InAppNotification>(registry)
