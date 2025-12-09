package net.thunderbird.android.dev

import app.k9mail.autodiscovery.api.AutoDiscovery
import com.fsck.k9.backend.BackendFactory
import net.thunderbird.core.featureflag.FeatureFlagOverrides
import net.thunderbird.core.featureflag.NoOpFeatureFlagOverrides
import org.koin.core.module.Module
import org.koin.core.qualifier.named

fun Module.developmentModuleAdditions() {
    single<Map<String, BackendFactory>>(named("developmentBackends")) {
        emptyMap()
    }
    single<List<AutoDiscovery>>(named("extraAutoDiscoveries")) {
        emptyList()
    }
    single<FeatureFlagOverrides> { NoOpFeatureFlagOverrides() }
}
