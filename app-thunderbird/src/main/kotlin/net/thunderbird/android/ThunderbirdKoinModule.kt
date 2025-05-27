package net.thunderbird.android

import app.k9mail.feature.widget.shortcut.LauncherShortcutActivity
import com.fsck.k9.AppConfig
import com.fsck.k9.activity.MessageCompose
import net.thunderbird.android.auth.TbOAuthConfigurationFactory
import net.thunderbird.android.dev.developmentModuleAdditions
import net.thunderbird.android.feature.featureModule
import net.thunderbird.android.featureflag.TbFeatureFlagFactory
import net.thunderbird.android.provider.providerModule
import net.thunderbird.android.widget.provider.MessageListWidgetProvider
import net.thunderbird.android.widget.provider.UnreadWidgetProvider
import net.thunderbird.android.widget.widgetModule
import net.thunderbird.app.common.appCommonModule
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.featureflag.FeatureFlagFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    includes(appCommonModule)

    includes(widgetModule)
    includes(featureModule)
    includes(providerModule)

    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { TbOAuthConfigurationFactory() }
    single<FeatureFlagFactory> { TbFeatureFlagFactory() }

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
