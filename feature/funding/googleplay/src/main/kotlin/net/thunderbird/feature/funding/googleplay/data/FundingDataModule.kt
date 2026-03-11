package net.thunderbird.feature.funding.googleplay.data

import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.data.local.LocalContributionPurchaseDataSource
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigDefinition
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigMapper
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigMigration
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigStore
import net.thunderbird.feature.funding.googleplay.data.mapper.ContributionPurchaseMapper
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
    single<Local.ContributionConfigMapper> {
        ContributionConfigMapper(
            logger = get(),
        )
    }
    single<Local.ContributionConfigMigration> {
        ContributionConfigMigration()
    }

    single<Local.ContributionConfigDefinition> {
        ContributionConfigDefinition(
            mapper = get(),
            migration = get(),
        )
    }

    single<Local.ContributionConfigStore> {
        ContributionConfigStore(
            provider = get(),
            definition = get(),
        )
    }

    single<Local.ContributionPurchaseDataSource> {
        LocalContributionPurchaseDataSource(
            configStore = get(),
            contributionPurchaseMapper = get(),
        )
    }

    single<FundingDataContract.Mapper.ContributionPurchase> {
        ContributionPurchaseMapper()
    }

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
            logger = get(),
        )
    }

    single<FundingDomainContract.ContributionRepository> {
        ContributionRepository(
            remoteDataSource = get(),
            localDataSource = get(),
        )
    }
}
