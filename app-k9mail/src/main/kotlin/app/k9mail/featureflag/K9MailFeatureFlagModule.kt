package app.k9mail.featureflag

import net.thunderbird.core.featureflag.inject.featureFlagModule
import net.thunderbird.core.featureflag.inject.qualifier.InjectQualifier
import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.RuntimeDebugOverrideFeatureFlagProvider
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import org.koin.core.qualifier.named
import org.koin.dsl.module

val k9MailFeatureFlagModule = module {
    includes(featureFlagModule)
    factory<AppVariantOverrides.Factory> { K9MailOverrides.Factory }
    factory { FlagRegistryOverrideSerializer(k9Factory = get(), thunderbirdFactory = EmptyAppVariantOverride) }
    single {
        RuntimeDebugOverrideFeatureFlagProvider(
            configStore = get(),
            logger = get(),
        )
    }
    single<CatalogFeatureFlagProvider>(named(InjectQualifier.InMemory)) {
        get<RuntimeDebugOverrideFeatureFlagProvider>()
    }
}
