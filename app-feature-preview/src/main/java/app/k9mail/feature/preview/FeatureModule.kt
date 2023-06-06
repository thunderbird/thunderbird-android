package app.k9mail.feature.preview

import app.k9mail.feature.account.setup.featureAccountSetupModule
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

val featureModule: Module = module {

    // TODO move to network module
    single<OkHttpClient> {
        OkHttpClient()
    }

    includes(featureAccountSetupModule)
}
