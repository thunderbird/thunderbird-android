package net.thunderbird.android

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.FeatureThemeProvider
import com.fsck.k9.AppConfig
import com.fsck.k9.activity.LauncherShortcuts
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.provider.UnreadWidgetProvider
import com.fsck.k9.widget.list.MessageListWidgetProvider
import net.thunderbird.android.auth.ThunderbirdOAuthConfigurationFactory
import net.thunderbird.android.dev.developmentModuleAdditions
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("ClientIdAppName")) { BuildConfig.CLIENT_ID_APP_NAME }
    single(named("ClientIdAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { ThunderbirdOAuthConfigurationFactory() }
    single<FeatureThemeProvider> { provideFeatureThemeProvider() }

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

private fun provideFeatureThemeProvider(): FeatureThemeProvider = FeatureThemeProvider { content ->
    ThunderbirdTheme2 {
        content()
    }
}
