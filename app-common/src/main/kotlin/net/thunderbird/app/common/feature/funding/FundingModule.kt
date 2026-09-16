package net.thunderbird.app.common.feature.funding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.thunderbird.app.common.feature.funding.configstore.DefaultFundingConfigStore
import net.thunderbird.app.common.feature.funding.configstore.FundingConfigStore
import net.thunderbird.core.configstore.ConfigId
import org.koin.core.qualifier.named
import org.koin.dsl.module

val fundingModule = module {

    single<CoroutineScope>(named("ConfigStoreScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    single<FundingConfigStore> {
        DefaultFundingConfigStore(
            id = ConfigId(backend = "funding", feature = "storage"),
            provider = get(),
            scope = get(named("ConfigStoreScope")),
        )
    }
}
