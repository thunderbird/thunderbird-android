package net.thunderbird.core.preference

/**
 * Publishes changes of preferences to all subscribers.
 * The change can be scoped to a specific category of preferences or applied globally.
 * @see PreferenceScope for available scopes of preference changes.
 */
interface PreferenceChangePublisher {

    /**
     * Publish a change in the preferences.
     * @param scope Defines which category of preferences has changed.
     * By default, [PreferenceScope.ALL] is used, indicating a global change affecting all preferences.
     */
    fun publish(scope: PreferenceScope = PreferenceScope.ALL)
}
