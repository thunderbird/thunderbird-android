package net.thunderbird.app.common.feature.funding

import net.thunderbird.app.common.feature.funding.configstore.DefaultFundingConfigStore
import net.thunderbird.app.common.feature.funding.configstore.FundingConfigStore
import net.thunderbird.core.configstore.ConfigId
import org.koin.dsl.module

val fundingModule = module {
    single<FundingConfigStore> {
        DefaultFundingConfigStore(
            id = ConfigId(backend = "funding", feature = "storage"),
            provider = get()
        )
    }
}
