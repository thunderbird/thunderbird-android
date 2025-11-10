package net.thunderbird.core.logging.config

import timber.log.Timber

actual class PlatformInitializer {
    actual fun setUp(plantTimber: Boolean) {
        Timber.uprootAll()
        if (plantTimber) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
