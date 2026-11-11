package app.k9mail

import app.k9mail.auth.K9OAuthConfigurationFactory
import app.k9mail.dev.developmentModuleAdditions
import app.k9mail.feature.featureDiModule
import app.k9mail.feature.widget.shortcut.LauncherShortcutActivity
import app.k9mail.featureflag.featureFlagDiModule
import app.k9mail.provider.providerDiModule
import app.k9mail.widget.widgetDiModule
import com.fsck.k9.AppConfig
import com.fsck.k9.BuildConfig
import com.fsck.k9.DefaultAppConfig
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.provider.UnreadWidgetProvider
import com.fsck.k9.widget.list.MessageListWidgetProvider
import net.thunderbird.app.common.appCommonDiModule
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module

val k9mailDiModule = module {
    includes(featureFlagDiModule)
    includes(appCommonDiModule)

    includes(widgetDiModule)
    includes(featureDiModule)
    includes(providerDiModule)

    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { K9OAuthConfigurationFactory() }

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
