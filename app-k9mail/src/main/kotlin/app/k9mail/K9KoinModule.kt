package app.k9mail

import app.k9mail.auth.K9OAuthConfigurationFactory
import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.ui.theme.api.FeatureThemeProvider
import app.k9mail.core.ui.theme.api.ThemeProvider
import app.k9mail.dev.developmentModuleAdditions
import app.k9mail.feature.funding.featureFundingModule
import app.k9mail.feature.telemetry.telemetryModule
import app.k9mail.feature.widget.shortcut.LauncherShortcutActivity
import app.k9mail.featureflag.K9FeatureFlagFactory
import app.k9mail.provider.K9AppNameProvider
import app.k9mail.provider.K9FeatureThemeProvider
import app.k9mail.widget.appWidgetModule
import com.fsck.k9.AppConfig
import com.fsck.k9.BuildConfig
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.provider.K9ThemeProvider
import com.fsck.k9.provider.UnreadWidgetProvider
import com.fsck.k9.widget.list.MessageListWidgetProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    includes(appWidgetModule)
    includes(telemetryModule)
    includes(featureFundingModule)

    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { K9OAuthConfigurationFactory() }
    single<AppNameProvider> { K9AppNameProvider(androidContext()) }
    single<ThemeProvider> { K9ThemeProvider() }
    single<FeatureThemeProvider> { K9FeatureThemeProvider() }
    single<FeatureFlagFactory> { K9FeatureFlagFactory() }

    developmentModuleAdditions()
}

val appConfig = AppConfig(
    componentsToDisable = listOf(
        MessageCompose::class.java,
        LauncherShortcutActivity::class.java,
        UnreadWidgetProvider::class.java,
        MessageListWidgetProvider::class.java,
    ),
)
