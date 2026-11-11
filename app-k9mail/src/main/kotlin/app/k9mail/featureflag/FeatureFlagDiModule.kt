package app.k9mail.featureflag

import net.thunderbird.core.featureflag.coreFeatureFlagDiModule
import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import org.koin.dsl.module

internal val featureFlagDiModule = module {
    includes(coreFeatureFlagDiModule)
    factory<AppVariantOverrides.Factory> { K9MailOverrides.Factory }
    factory { FlagRegistryOverrideSerializer(k9Factory = get(), thunderbirdFactory = EmptyAppVariantOverride) }
}
