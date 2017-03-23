package com.fsck.k9.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import timber.log.Timber;

import com.fsck.k9.K9;

/**
 * Capture the system shutdown event in order to properly free resources.
 *
 * <p>
 * It is advised not to statically register (from AndroidManifest.xml) this
 * receiver in order to avoid unnecessary K-9 launch (which would defeat the
 * purpose of that receiver). Using AndroidManifest.xml instructs Android to
 * launch K-9 if not running, defeating the purpose of this receiver. <br>
 * The recommended way is to register this receiver using
 * {@link Context#registerReceiver(BroadcastReceiver, android.content.IntentFilter)}
 * </p>
 */
public class ShutdownReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
            Timber.i("System is shutting down, releasing resources");

            // prevent any scheduled intent from waking up K-9
            BootReceiver.purgeSchedule(context);

            /*
             * TODO invoke proper shutdown methods (stop any running thread)
             *
             * 20101111: this can't be done now as we don't have proper
             * startup/shutdown sequences
             */
        }
    }

}
