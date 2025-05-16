package net.thunderbird.core.preferences

/**
 * Subscribe to be notified about changes in the settings.
 */
fun interface SettingsChangeSubscriber {

    /**
     * Called when settings change.
     */
    fun receive()
}
