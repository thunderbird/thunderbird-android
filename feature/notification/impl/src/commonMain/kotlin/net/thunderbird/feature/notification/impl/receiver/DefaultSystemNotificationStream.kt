package net.thunderbird.feature.notification.impl.receiver

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.SystemNotificationStream
import net.thunderbird.feature.notification.impl.DefaultNotificationRegistry

class DefaultSystemNotificationStream(
    registry: DefaultNotificationRegistry,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : SystemNotificationStream {
    private val scope: CoroutineScope = CoroutineScope(mainDispatcher)
    override val notifications: StateFlow<Set<SystemNotification>> = registry
        .registrar
        .map { registrar ->
            registrar
                .values
                .filterIsInstance<SystemNotification>()
                .toSet()
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, emptySet())
}
