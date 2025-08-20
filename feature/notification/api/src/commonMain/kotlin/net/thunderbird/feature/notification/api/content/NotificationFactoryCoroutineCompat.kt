package net.thunderbird.feature.notification.api.content

import androidx.annotation.Discouraged
import kotlinx.coroutines.runBlocking

object NotificationFactoryCoroutineCompat {
    @JvmStatic
    @Discouraged("Should not be used outside a Java class.")
    fun <T> create(builder: suspend () -> T): T = runBlocking { builder() }
}
