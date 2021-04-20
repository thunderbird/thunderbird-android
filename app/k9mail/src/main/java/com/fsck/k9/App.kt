package com.fsck.k9

import android.app.Application
import android.content.res.Configuration
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.external.MessageProvider
import com.fsck.k9.ui.base.AppLanguageManager
import com.fsck.k9.ui.base.ThemeManager
import com.fsck.k9.ui.base.extensions.currentLocale
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.koin.android.ext.android.inject
import timber.log.Timber

class App : Application() {
    private val messagingController: MessagingController by inject()
    private val messagingListenerProvider: MessagingListenerProvider by inject()
    private val themeManager: ThemeManager by inject()
    private val appLanguageManager: AppLanguageManager by inject()
    private val appCoroutineScope: CoroutineScope = GlobalScope + Dispatchers.Main

    override fun onCreate() {
        Core.earlyInit(this)

        super.onCreate()

        DI.start(this, coreModules + uiModules + appModules)

        K9.init(this)
        Core.init(this)
        MessageProvider.init()
        initializeAppLanguage()
        themeManager.init()

        messagingListenerProvider.listeners.forEach { listener ->
            messagingController.addListener(listener)
        }
    }

    private fun initializeAppLanguage() {
        appLanguageManager.init()
        applyOverrideLocale()
        listenForAppLanguageChanges()
    }

    private fun applyOverrideLocale() {
        appLanguageManager.getOverrideLocale()?.let { overrideLocale ->
            updateConfigurationWithLocale(resources.configuration, overrideLocale)
        }
    }

    private fun listenForAppLanguageChanges() {
        appLanguageManager.overrideLocale
            .drop(1) // We already applied the initial value
            .onEach { overrideLocale ->
                val locale = overrideLocale ?: Locale.getDefault()
                updateConfigurationWithLocale(resources.configuration, locale)
            }
            .launchIn(appCoroutineScope)
    }

    override fun onConfigurationChanged(newConfiguration: Configuration) {
        applyOverrideLocale()
        super.onConfigurationChanged(resources.configuration)
    }

    private fun updateConfigurationWithLocale(configuration: Configuration, locale: Locale) {
        Timber.d("Updating application configuration with locale '$locale'")

        val newConfiguration = Configuration(configuration).apply {
            currentLocale = locale
        }

        @Suppress("DEPRECATION")
        resources.updateConfiguration(newConfiguration, resources.displayMetrics)
    }

    companion object {
        val appConfig = AppConfig(
            componentsToDisable = listOf(MessageCompose::class.java)
        )
    }
}
