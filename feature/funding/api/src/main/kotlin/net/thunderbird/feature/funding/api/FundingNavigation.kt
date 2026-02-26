package net.thunderbird.feature.funding.api

import kotlinx.serialization.Serializable
import net.thunderbird.core.ui.navigation.Navigation
import net.thunderbird.core.ui.navigation.Route

const val FUNDING_BASE_DEEP_LINK = "app://feature/funding"

sealed interface FundingRoute : Route {
    @Serializable
    data object Contribution : FundingRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = basePath

        const val BASE_PATH = "$FUNDING_BASE_DEEP_LINK/contribution"
    }
}

interface FundingNavigation : Navigation<FundingRoute>
