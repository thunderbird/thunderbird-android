package com.fsck.k9

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import com.fsck.k9.autocrypt.autocryptModule
import com.fsck.k9.controller.controllerModule
import com.fsck.k9.crypto.openPgpModule
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mailstore.mailStoreModule
import com.fsck.k9.message.extractors.extractorModule
import com.fsck.k9.message.html.htmlModule
import com.fsck.k9.message.quote.quoteModule
import com.fsck.k9.notification.coreNotificationModule
import com.fsck.k9.power.DeviceIdleManager
import com.fsck.k9.search.searchModule
import com.fsck.k9.service.BootReceiver
import com.fsck.k9.service.MailService
import com.fsck.k9.service.ShutdownReceiver
import com.fsck.k9.service.StorageGoneReceiver
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.SynchronousQueue

object Core : KoinComponent {
    private val appConfig: AppConfig by inject()

    private val componentsToDisable = listOf(BootReceiver::class.java, MailService::class.java)

    @JvmStatic
    val coreModules = listOf(
            mainModule,
            openPgpModule,
            autocryptModule,
            mailStoreModule,
            searchModule,
            extractorModule,
            htmlModule,
            quoteModule,
            coreNotificationModule,
            controllerModule
    )

    /**
     * This needs to be called from [Application#onCreate][android.app.Application#onCreate] before calling through
     * to the super class's `onCreate` implementation and before initializing the dependency injection library.
     */
    fun earlyInit(context: Context) {
        if (K9.DEVELOPER_MODE) {
            StrictMode.enableDefaults()
        }

        PRNGFixes.apply()

        val packageName = context.packageName
        K9.Intents.init(packageName)
    }

    fun init(context: Context) {
        BinaryTempFileBody.setTempDirectory(context.cacheDir)
        LocalKeyStore.setKeyStoreLocation(context.getDir("KeyStore", Context.MODE_PRIVATE).toString())

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

        setServicesEnabled(appContext, enable, null)

        updateDeviceIdleReceiver(appContext, enable)
    }


    private fun updateDeviceIdleReceiver(context: Context, enable: Boolean) {
        val deviceIdleManager = DeviceIdleManager.getInstance(context)
        if (enable) {
            deviceIdleManager.registerReceiver()
        } else {
            deviceIdleManager.unregisterReceiver()
        }
    }

    private fun setServicesEnabled(context: Context, enabled: Boolean, wakeLockId: Int?) {
        val pm = context.packageManager

        if (!enabled && pm.getComponentEnabledSetting(ComponentName(context, MailService::class.java)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * If no accounts now exist but the service is still enabled we're about to disable it
             * so we'll reschedule to kill off any existing alarms.
             */
            MailService.actionReset(context, wakeLockId)
        }

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

        if (enabled && pm.getComponentEnabledSetting(ComponentName(context, MailService::class.java)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * And now if accounts do exist then we've just enabled the service and we want to
             * schedule alarms for the new accounts.
             */
            MailService.actionReset(context, wakeLockId)
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
