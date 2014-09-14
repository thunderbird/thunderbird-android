package com.fsck.k9.activity.misc;

import android.app.Activity;


public interface NonConfigurationInstance {
    /**
     * Decide whether to retain this {@code NonConfigurationInstance} and clean up resources if
     * necessary.
     *
     * <p>
     * This needs to be called when the current activity is being destroyed during an activity
     * restart due to a configuration change.<br>
     * Implementations should make sure that references to the {@code Activity} instance that is
     * about to be destroyed are cleared to avoid memory leaks. This includes all UI elements that
     * are bound to an activity (e.g. dialogs). They can be re-created in
     * {@link #restore(Activity)}.
     * </p>
     *
     * @return {@code true} if this instance should be retained; {@code false} otherwise.
     *
     * @see Activity#onRetainNonConfigurationInstance()
     */
    public boolean retain();

    /**
     * Connect this retained {@code NonConfigurationInstance} to the new {@link Activity} instance
     * after the activity was restarted due to a configuration change.
     *
     * <p>
     * This also creates a new progress dialog that is bound to the new activity.
     * </p>
     *
     * @param activity
     *         The new {@code Activity} instance. Never {@code null}.
     */
    public void restore(Activity activity);
}
