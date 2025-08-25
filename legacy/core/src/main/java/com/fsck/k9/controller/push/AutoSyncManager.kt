package com.fsck.k9.controller.push

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.fsck.k9.K9
import net.thunderbird.core.logging.legacy.Log

/**
 * Listen for changes to the system's auto sync setting.
 */
internal class AutoSyncManager(private val context: Context) {
    val isAutoSyncDisabled: Boolean
        get() = respectSystemAutoSync && !ContentResolver.getMasterSyncAutomatically()

    val respectSystemAutoSync: Boolean
        get() = K9.backgroundOps == K9.BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC

    private var isRegistered = false
    private var listener: AutoSyncListener? = null

    private val intentFilter = IntentFilter().apply {
        addAction("com.android.sync.SYNC_CONN_STATUS_CHANGED")
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val listener = synchronized(this@AutoSyncManager) { listener }
            listener?.onAutoSyncChanged()
        }
    }

    @Synchronized
    fun registerListener(listener: AutoSyncListener) {
        if (!isRegistered) {
            Log.v("Registering auto sync listener")
            isRegistered = true
            this.listener = listener
            ContextCompat.registerReceiver(context, receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    @Synchronized
    fun unregisterListener() {
        if (isRegistered) {
            Log.v("Unregistering auto sync listener")
            isRegistered = false
            context.unregisterReceiver(receiver)
        }
    }
}

internal fun interface AutoSyncListener {
    fun onAutoSyncChanged()
}
