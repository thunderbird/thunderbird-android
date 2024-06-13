package net.thunderbird.android

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.FeatureThemeProvider
import app.k9mail.feature.widget.unread.UnreadWidgetClassProvider
import com.fsck.k9.AppConfig
import com.fsck.k9.activity.LauncherShortcuts
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.widget.list.MessageListWidgetProvider
import net.thunderbird.android.auth.ThunderbirdOAuthConfigurationFactory
import net.thunderbird.android.dev.developmentModuleAdditions
import net.thunderbird.android.provider.TbAppNameProvider
import net.thunderbird.android.provider.TbFeatureThemeProvider
import net.thunderbird.android.widget.provider.UnreadWidgetProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { ThunderbirdOAuthConfigurationFactory() }
    single<AppNameProvider> { TbAppNameProvider(androidContext()) }
    single<FeatureThemeProvider> { TbFeatureThemeProvider() }
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
