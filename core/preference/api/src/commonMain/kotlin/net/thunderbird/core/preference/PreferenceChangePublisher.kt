package net.thunderbird.core.preference

/**
 * Publishes changes of preferences to all subscribers.
 */
interface PreferenceChangePublisher {

    /**
     * Publish a change in the preferences.
     */
    fun publish()
}
