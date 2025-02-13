package app.k9mail.legacy.preferences

/**
 * Publishes changes in the settings.
 */
interface SettingsChangePublisher {

    /**
     * Publish a change in the settings.
     */
    fun publish()
}
