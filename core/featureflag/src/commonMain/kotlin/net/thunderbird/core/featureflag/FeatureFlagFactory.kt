package net.thunderbird.core.featureflag

fun interface FeatureFlagFactory {
    fun createFeatureCatalog(): List<FeatureFlag>
}
