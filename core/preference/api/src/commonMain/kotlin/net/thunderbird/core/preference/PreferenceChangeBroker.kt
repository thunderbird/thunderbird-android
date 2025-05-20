package net.thunderbird.core.preference

/**
 * Broker to manage subscribers and notify them about changes in the preferences, when the
 * [PreferenceChangePublisher] publishes a change.
 */
interface PreferenceChangeBroker {

    /**
     * Subscribe to preference changes.
     *
     * @param subscriber The subscriber to be notified about preference changes.
     */
    fun subscribe(subscriber: PreferenceChangeSubscriber)

    /**
     * Unsubscribe from preference changes.
     *
     * @param subscriber The subscriber that no longer wants to be notified about preference changes.
     */
    fun unsubscribe(subscriber: PreferenceChangeSubscriber)
}
