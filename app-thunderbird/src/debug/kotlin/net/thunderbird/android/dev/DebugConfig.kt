package net.thunderbird.android.dev

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.demo.DemoAutoDiscovery
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.core.featureflag.DefaultFeatureFlagOverrides
import net.thunderbird.core.featureflag.FeatureFlagOverrides
import net.thunderbird.core.featureflag.inject.qualifier.InjectQualifier
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.RuntimeDebugOverrideFeatureFlagProvider
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.plugin.module.dsl.bind

fun Module.developmentModuleAdditions() {
    single {
        DemoBackendFactory(
            backendStorageFactory = get(),
        )
    }
    single<Map<String, BackendFactory>>(named("developmentBackends")) {
        mapOf("demo" to get<DemoBackendFactory>())
    }
    single<List<AutoDiscovery>>(named("extraAutoDiscoveries")) {
        listOf(DemoAutoDiscovery())
    }
    single<FeatureFlagOverrides> { DefaultFeatureFlagOverrides() }
    single<CatalogFeatureFlagProvider>(named(InjectQualifier.InMemory)) {
        RuntimeDebugOverrideFeatureFlagProvider(
            configStore = get(),
            logger = get(),
        )
    }.bind(RuntimeDebugOverrideFeatureFlagProvider::class)
}
