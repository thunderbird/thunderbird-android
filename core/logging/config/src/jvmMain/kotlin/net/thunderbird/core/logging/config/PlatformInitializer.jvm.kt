package net.thunderbird.core.logging.config

actual class PlatformInitializer {
    actual fun setUp(plantTimber: Boolean) {
        error("PlatformInitializer is not implemented for JVM platform")
    }
}
