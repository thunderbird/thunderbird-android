package net.thunderbird.feature.funding.googleplay.data

import com.android.billingclient.api.ProductDetails
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.common.cache.InMemoryCache
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.data.mapper.BillingResultMapper
import net.thunderbird.feature.funding.googleplay.data.mapper.ProductDetailsMapper
import net.thunderbird.feature.funding.googleplay.data.remote.RemoteContributionDataSource
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingClient
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingClientProvider
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingPurchaseHandler
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import org.koin.dsl.module

internal val fundingDataModule = module {
    single<FundingDataContract.Mapper.Product> {
        ProductDetailsMapper()
    }

    single<FundingDataContract.Mapper.BillingResult> {
        BillingResultMapper()
    }

    single<Remote.BillingClientProvider> {
        BillingClientProvider(
            context = get(),
        )
    }

    single<Cache<String, ProductDetails>> {
        InMemoryCache()
    }

    single<Remote.BillingPurchaseHandler> {
        BillingPurchaseHandler(
            productCache = get(),
            productMapper = get(),
            logger = get(),
        )
    }

    single<Remote.BillingClient> {
        BillingClient(
            clientProvider = get(),
            productMapper = get(),
            resultMapper = get(),
            productCache = get(),
            purchaseHandler = get(),
            activityProvider = get(),
            logger = get(),
        )
    }

    single<Remote.ContributionDataSource> {
        RemoteContributionDataSource(
            billingClient = get(),
        )
    }

    single<FundingDomainContract.ContributionRepository> {
        ContributionRepository(
            remoteContributionDataSource = get(),
        )
    }
}
