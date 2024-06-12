package app.k9mail

import app.k9mail.auth.K9OAuthConfigurationFactory
import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.dev.developmentModuleAdditions
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.FeatureThemeProvider
import app.k9mail.feature.widget.unread.UnreadWidgetClassProvider
import app.k9mail.provider.K9AppNameProvider
import app.k9mail.provider.K9FeatureThemeProvider
import com.fsck.k9.AppConfig
import com.fsck.k9.BuildConfig
import com.fsck.k9.activity.LauncherShortcuts
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.provider.UnreadWidgetProvider
import com.fsck.k9.widget.list.MessageListWidgetProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("ClientIdAppName")) { BuildConfig.CLIENT_ID_APP_NAME }
    single(named("ClientIdAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { K9OAuthConfigurationFactory() }
    single<AppNameProvider> { K9AppNameProvider(androidContext()) }
    single<FeatureThemeProvider> { K9FeatureThemeProvider() }
    single<UnreadWidgetClassProvider> {
        UnreadWidgetClassProvider { UnreadWidgetProvider::class.java }
    }

    developmentModuleAdditions()
}

val appConfig = AppConfig(
    componentsToDisable = listOf(
        MessageCompose::class.java,
        LauncherShortcuts::class.java,
        UnreadWidgetProvider::class.java,
        MessageListWidgetProvider::class.java,
    ),
)
