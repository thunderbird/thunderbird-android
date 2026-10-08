package net.thunderbird.core.preference

/**
 * Subscribe to be notified about changes in the preferences.
 */
fun interface PreferenceChangeSubscriber {

    /**
     * Called when preferences within a given scope have changed.
     * @param scope The [PreferenceScope] indicating which category of preferences
     * has been updated.
     */
    fun receive(scope: PreferenceScope)
}
