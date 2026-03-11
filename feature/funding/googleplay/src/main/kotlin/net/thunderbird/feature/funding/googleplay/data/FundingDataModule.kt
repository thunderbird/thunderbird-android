package net.thunderbird.feature.funding.googleplay.data

import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.data.mapper.ProductDetailsMapper
import net.thunderbird.feature.funding.googleplay.data.remote.RemoteContributionDataSource
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingClient
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingClientProvider
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingConnector
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingProductCache
import net.thunderbird.feature.funding.googleplay.data.remote.bilingclient.BillingPurchaseHandler
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import org.koin.dsl.module

internal val fundingDataModule = module {
    single<FundingDataContract.Mapper.Product> {
        ProductDetailsMapper()
    }

    single<Remote.BillingClientProvider> {
        BillingClientProvider(
            context = get(),
        )
    }

    single<Remote.BillingProductCache> {
        BillingProductCache()
    }

    single<Remote.BillingPurchaseHandler> {
        BillingPurchaseHandler(
            productCache = get(),
            productMapper = get(),
            logger = get(),
        )
    }

    single<Remote.BillingConnector> {
        BillingConnector(
            clientProvider = get(),
            productCache = get(),
            logger = get(),
        )
    }

    single<Remote.BillingClient> {
        BillingClient(
            clientProvider = get(),
            productMapper = get(),
            productCache = get(),
            purchaseHandler = get(),
            activityProvider = get(),
            logger = get(),
        )
    }

    single<Remote.ContributionDataSource> {
        RemoteContributionDataSource(
            billingConnector = get(),
            billingClient = get(),
        )
    }

    single<FundingDomainContract.ContributionRepository> {
        ContributionRepository(
            remoteContributionDataSource = get(),
        )
    }
}
