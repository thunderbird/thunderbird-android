package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import com.android.billingclient.api.ProductDetails
import net.thunderbird.core.common.cache.InMemoryCache
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Remote

class BillingProductCache : InMemoryCache<String, ProductDetails>(), Remote.BillingProductCache
