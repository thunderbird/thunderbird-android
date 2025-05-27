package net.thunderbird.core.preferences

/**
 * Publishes changes in the settings.
 */
interface SettingsChangePublisher {

    /**
     * Publish a change in the settings.
     */
    fun publish()
}
