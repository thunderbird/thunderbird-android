package com.fsck.k9

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.k9mail.legacy.di.EarlyInit
import app.k9mail.legacy.di.inject
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.notification.NotificationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named

object Core : EarlyInit {
    private val context: Context by inject()
    private val appConfig: AppConfig by inject()
    private val jobManager: K9JobManager by inject()
    private val appCoroutineScope: CoroutineScope by inject(named("AppCoroutineScope"))
    private val preferences: Preferences by inject()
    private val notificationController: NotificationController by inject()

    /**
     * This needs to be called from [Application#onCreate][android.app.Application#onCreate] before calling through
     * to the super class's `onCreate` implementation and before initializing the dependency injection library.
     */
    fun earlyInit() {
        if (K9.DEVELOPER_MODE) {
            enableStrictMode()
        }
    }

    fun init(context: Context) {
        BinaryTempFileBody.setTempDirectory(context.cacheDir)

        setServicesEnabled(context)

        restoreNotifications()
    }

    /**
     * Called throughout the application when the number of accounts has changed. This method
     * enables or disables the Compose activity, the boot receiver and the service based on
     * whether any accounts are configured.
     */
    @JvmStatic
    fun setServicesEnabled(context: Context) {
        val appContext = context.applicationContext
        val acctLength = Preferences.getPreferences().getAccounts().size
        val enable = acctLength > 0

        setServicesEnabled(appContext, enable)
    }

    fun setServicesEnabled() {
        setServicesEnabled(context)
    }

    private fun setServicesEnabled(context: Context, enabled: Boolean) {
        val pm = context.packageManager

        for (clazz in appConfig.componentsToDisable) {
            val alreadyEnabled = pm.getComponentEnabledSetting(ComponentName(context, clazz)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED

            if (enabled != alreadyEnabled) {
                pm.setComponentEnabledSetting(
                    ComponentName(context, clazz),
                    if (enabled) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

        if (enabled) {
            jobManager.scheduleAllMailJobs()
        }
    }

    private fun restoreNotifications() {
        appCoroutineScope.launch(Dispatchers.IO) {
            val accounts = preferences.getAccounts()
            notificationController.restoreNewMailNotifications(accounts)
        }
    }
}
