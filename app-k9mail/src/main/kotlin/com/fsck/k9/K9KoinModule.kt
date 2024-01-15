package com.fsck.k9

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.dev.developmentModuleAdditions
import com.fsck.k9.activity.LauncherShortcuts
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.auth.AppOAuthConfigurationFactory
import com.fsck.k9.provider.UnreadWidgetProvider
import com.fsck.k9.widget.list.MessageListWidgetProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("ClientIdAppName")) { BuildConfig.CLIENT_ID_APP_NAME }
    single(named("ClientIdAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { AppOAuthConfigurationFactory() }

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
