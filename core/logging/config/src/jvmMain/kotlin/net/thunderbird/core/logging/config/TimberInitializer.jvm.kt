package net.thunderbird.core.logging.config

actual class TimberInitializer {
    actual fun setUp(plantTimber: Boolean) {
        error("TimberInitializer is not implemented for JVM platform")
    }
}
