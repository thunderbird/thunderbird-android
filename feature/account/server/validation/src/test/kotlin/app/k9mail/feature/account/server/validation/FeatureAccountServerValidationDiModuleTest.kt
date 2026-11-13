package app.k9mail.feature.account.server.validation

import android.app.Application
import android.content.Context
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureAccountServerValidationDiModuleTest {

    private val externalDependencies = module {
        single<Application> { mock() }
        single<Context> { get<Application>() }
        single<Logger> { mock() }
        single<OAuthConfigurationFactory> { mock() }
        single<OAuth2TokenProviderFactory> { mock() }
        single<TrustedSocketFactory> { mock() }
        single<LocalKeyStore> { mock() }
        single<String>(named("ClientInfoAppName")) { "test" }
        single<String>(named("ClientInfoAppVersion")) { "1" }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureAccountServerValidationDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
