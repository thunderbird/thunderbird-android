package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.ProductDetails
import net.thunderbird.core.common.cache.InMemoryCache
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId

internal class BillingProductCache : InMemoryCache<ContributionId, ProductDetails>(), Remote.BillingProductCache
