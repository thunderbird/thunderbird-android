package app.k9mail.legacy.preferences

/**
 * Broker to manage subscribers and notify them about changes in the settings, when the
 * [SettingsChangePublisher] publishes a change.
 */
interface SettingsChangeBroker {

    /**
     * Subscribe to settings changes.
     *
     * @param subscriber The subscriber to be notified about settings changes.
     */
    fun subscribe(subscriber: SettingsChangeSubscriber)

    /**
     * Unsubscribe from settings changes.
     *
     * @param subscriber The subscriber that no longer wants to be notified about settings changes.
     */
    fun unsubscribe(subscriber: SettingsChangeSubscriber)
}
