package net.thunderbird.core.android.common.activity

import android.app.Activity

/**
 * Interface for providing the current [Activity].
 */
fun interface ActivityProvider {

    /**
     * Returns the current [Activity].
     *
     * @return The current activity, or `null` if no activity is available.
     */
    fun getCurrent(): Activity?
}
