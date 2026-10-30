package net.thunderbird.core.featureflag.navigation

import kotlinx.serialization.Serializable
import net.thunderbird.core.ui.navigation.Route

@Serializable
data object RemoteFeatureFlagRoute : Route {
    override val basePath: String = "app://remote_feature_flag"

    override fun route(): String = basePath
}
