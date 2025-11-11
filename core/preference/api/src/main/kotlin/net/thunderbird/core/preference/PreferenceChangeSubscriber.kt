package net.thunderbird.core.preference

/**
 * Subscribe to be notified about changes in the preferences.
 */
fun interface PreferenceChangeSubscriber {

    /**
     * Called when preferences change.
     */
    fun receive()
}
