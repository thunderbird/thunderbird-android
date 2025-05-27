package app.k9mail

import app.k9mail.auth.K9OAuthConfigurationFactory
import app.k9mail.dev.developmentModuleAdditions
import app.k9mail.feature.featureModule
import app.k9mail.feature.widget.shortcut.LauncherShortcutActivity
import app.k9mail.featureflag.K9FeatureFlagFactory
import app.k9mail.provider.providerModule
import app.k9mail.widget.widgetModule
import com.fsck.k9.AppConfig
import com.fsck.k9.BuildConfig
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.provider.UnreadWidgetProvider
import com.fsck.k9.widget.list.MessageListWidgetProvider
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
    single<OAuthConfigurationFactory> { K9OAuthConfigurationFactory() }
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
