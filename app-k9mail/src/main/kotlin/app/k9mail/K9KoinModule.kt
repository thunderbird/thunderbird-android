package app.k9mail

import app.k9mail.auth.K9OAuthConfigurationFactory

import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.ui.theme.api.FeatureThemeProvider
import app.k9mail.core.ui.theme.api.ThemeProvider
import app.k9mail.dev.developmentModuleAdditions
import app.k9mail.feature.featureModule
import app.k9mail.feature.widget.shortcut.LauncherShortcutActivity
import app.k9mail.featureflag.K9FeatureFlagFactory
import app.k9mail.provider.providerModule
import app.k9mail.widget.widgetModule
import app.k9mail.provider.K9AppNameProvider
import app.k9mail.provider.K9AppNotificationIconProvider
import app.k9mail.provider.K9FeatureThemeProvider
import app.k9mail.widget.appWidgetModule
import com.fsck.k9.AppConfig
import com.fsck.k9.BuildConfig
import com.fsck.k9.DefaultAppConfig
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.provider.UnreadWidgetProvider
import com.fsck.k9.widget.list.MessageListWidgetProvider
import net.thunderbird.app.common.appCommonModule
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.featureflag.FeatureFlagFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.single

val appModule = module {
    includes(appCommonModule)

    includes(widgetModule)
    includes(featureModule)
    includes(providerModule)

    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { K9OAuthConfigurationFactory() }
    single {
        K9AppNameProvider(androidContext())
    } binds arrayOf(AppNameProvider::class, BrandNameProvider::class, FilePrefixProvider::class)

    single<NotificationIconResourceProvider> {
        K9AppNotificationIconProvider(androidContext())
    }

    single<ThemeProvider> { K9ThemeProvider() }
    single<FeatureThemeProvider> { K9FeatureThemeProvider() }
    single<FeatureFlagFactory> { K9FeatureFlagFactory() }

    developmentModuleAdditions()
}

val appConfig = DefaultAppConfig(
    componentsToDisable = listOf(
        MessageCompose::class.java,
        LauncherShortcutActivity::class.java,
        UnreadWidgetProvider::class.java,
        MessageListWidgetProvider::class.java,
    ),
)
