package net.thunderbird.android.featureflag

import net.thunderbird.core.featureflag.inject.featureFlagModule
import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import org.koin.dsl.module

val thunderbirdFeatureFlagModule = module {
    includes(featureFlagModule)
    factory<AppVariantOverrides.Factory> { ThunderbirdOverrides.Factory }
    factory { FlagRegistryOverrideSerializer(k9Factory = EmptyAppVariantOverride, thunderbirdFactory = get()) }
}
