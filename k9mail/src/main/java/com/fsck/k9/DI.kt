package com.fsck.k9

import android.app.Application
import android.content.Context
import com.fsck.k9.autocrypt.autocryptModule
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.crypto.openPgpModule
import com.fsck.k9.mail.TransportProvider
import com.fsck.k9.ui.endtoend.endToEndUiModule
import com.fsck.k9.ui.settings.settingsUiModule
import com.fsck.k9.widget.unread.unreadWidgetModule
import org.koin.Koin
import org.koin.KoinContext
import org.koin.android.ext.koin.with
import org.koin.android.logger.AndroidLogger
import org.koin.core.parameter.Parameters
import org.koin.dsl.module.applicationContext
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext

object DI {
    private val mainModule = applicationContext {
        bean { Preferences.getPreferences(get()) }
        bean { MessagingController.getInstance(get()) }
        bean { TransportProvider() }
        bean { (get() as Context).resources }
    }

    val appModules = listOf(
            mainModule,
            settingsUiModule,
            unreadWidgetModule,
            endToEndUiModule,
            openPgpModule,
            autocryptModule
    )

    @JvmStatic fun start(application: Application) {
        @Suppress("ConstantConditionIf")
        Koin.logger = if (BuildConfig.DEBUG) AndroidLogger() else EmptyLogger()

        StandAloneContext.startKoin(appModules) with application
    }

    @JvmOverloads
    @JvmStatic
    fun <T : Any> get(clazz: Class<T>, name: String = "", parameters: Parameters = { emptyMap() }): T {
        val koinContext = StandAloneContext.koinContext as KoinContext
        val kClass = clazz.kotlin

        return if (name.isEmpty()) {
            koinContext.resolveInstance(kClass, parameters) { koinContext.beanRegistry.searchAll(kClass) }
        } else {
            koinContext.resolveInstance(kClass, parameters) { koinContext.beanRegistry.searchByName(name) }
        }
    }
}
