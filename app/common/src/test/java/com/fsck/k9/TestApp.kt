package com.fsck.k9

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import com.fsck.k9.backend.BackendFactory
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

class TestApp : CommonApp() {
    override fun provideAppModule(): Module = module {
        single(named("ClientIdAppName")) { "ClientIdAppName" }
        single(named("ClientIdAppVersion")) { "ClientIdAppVersion" }
        single {
            AppConfig(
                componentsToDisable = emptyList(),
            )
        }
        single<Map<String, BackendFactory>>(named("developmentBackends")) {
            emptyMap()
        }
        single<OAuthConfigurationFactory> {
            OAuthConfigurationFactory { emptyMap() }
        }
    }
}
