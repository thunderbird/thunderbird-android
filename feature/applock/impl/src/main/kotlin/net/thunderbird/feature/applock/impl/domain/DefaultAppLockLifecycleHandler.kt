package net.thunderbird.feature.applock.impl.domain

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Default implementation that registers with ProcessLifecycleOwner
 * and listens for screen-off broadcasts.
 */
internal class DefaultAppLockLifecycleHandler(
    private val application: Application,
) : AppLockLifecycleHandler {

    private var screenOffReceiver: BroadcastReceiver? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null

    override fun register(observer: DefaultLifecycleObserver, onScreenOff: () -> Unit) {
        // Clean up any previous registration to make this idempotent
        unregister()

        lifecycleObserver = observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    onScreenOff()
                }
            }
        }
        screenOffReceiver = receiver

        ContextCompat.registerReceiver(
            application,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun unregister() {
        screenOffReceiver?.let { application.unregisterReceiver(it) }
        screenOffReceiver = null

        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
        lifecycleObserver = null
    }
}
