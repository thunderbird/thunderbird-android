package com.fsck.k9

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.service.BootReceiver
import com.fsck.k9.service.ShutdownReceiver
import com.fsck.k9.service.StorageGoneReceiver
import java.util.concurrent.SynchronousQueue
import timber.log.Timber

object Core : EarlyInit {
    private val context: Context by inject()
    private val appConfig: AppConfig by inject()
    private val jobManager: K9JobManager by inject()

    private val componentsToDisable = listOf(BootReceiver::class.java)

    /**
     * This needs to be called from [Application#onCreate][android.app.Application#onCreate] before calling through
     * to the super class's `onCreate` implementation and before initializing the dependency injection library.
     */
    fun earlyInit(context: Context) {
        if (K9.DEVELOPER_MODE) {
            enableStrictMode()
        }

        val packageName = context.packageName
        K9.Intents.init(packageName)
    }

    fun init(context: Context) {
        BinaryTempFileBody.setTempDirectory(context.cacheDir)

        setServicesEnabled(context)
        registerReceivers(context)
    }

    /**
     * Called throughout the application when the number of accounts has changed. This method
     * enables or disables the Compose activity, the boot receiver and the service based on
     * whether any accounts are configured.
     */
    @JvmStatic
    fun setServicesEnabled(context: Context) {
        val appContext = context.applicationContext
        val acctLength = Preferences.getPreferences(appContext).availableAccounts.size
        val enable = acctLength > 0

        setServicesEnabled(appContext, enable)
    }

    fun setServicesEnabled() {
        setServicesEnabled(context)
    }

    private fun setServicesEnabled(context: Context, enabled: Boolean) {
        val pm = context.packageManager

        val classes = componentsToDisable + appConfig.componentsToDisable
        for (clazz in classes) {
            val alreadyEnabled = pm.getComponentEnabledSetting(ComponentName(context, clazz)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED

            if (enabled != alreadyEnabled) {
                pm.setComponentEnabledSetting(
                        ComponentName(context, clazz),
                        if (enabled)
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP)
            }
        }

        if (enabled) {
            jobManager.scheduleAllMailJobs()
        }
    }

    /**
     * Register BroadcastReceivers programmatically because doing it from manifest
     * would make K-9 auto-start. We don't want auto-start because the initialization
     * sequence isn't safe while some events occur (SD card unmount).
     */
    private fun registerReceivers(context: Context) {
        val receiver = StorageGoneReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_MEDIA_EJECT)
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        filter.addDataScheme("file")

        val queue = SynchronousQueue<Handler>()

        // starting a new thread to handle unmount events
        Thread(Runnable {
            Looper.prepare()
            try {
                queue.put(Handler())
            } catch (e: InterruptedException) {
                Timber.e(e)
            }

            Looper.loop()
        }, "Unmount-thread").start()

        try {
            val storageGoneHandler = queue.take()
            context.registerReceiver(receiver, filter, null, storageGoneHandler)
            Timber.i("Registered: unmount receiver")
        } catch (e: InterruptedException) {
            Timber.e(e, "Unable to register unmount receiver")
        }

        context.registerReceiver(ShutdownReceiver(), IntentFilter(Intent.ACTION_SHUTDOWN))
        Timber.i("Registered: shutdown receiver")
    }
}
