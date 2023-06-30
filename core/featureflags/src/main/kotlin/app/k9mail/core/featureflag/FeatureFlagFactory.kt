package app.k9mail.core.featureflag

fun interface FeatureFlagFactory {
    fun createFeatureCatalog(): List<FeatureFlag>
}
