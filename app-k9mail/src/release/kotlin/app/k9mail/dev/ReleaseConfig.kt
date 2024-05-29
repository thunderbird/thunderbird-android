package app.k9mail.dev

import app.k9mail.autodiscovery.api.AutoDiscovery
import com.fsck.k9.backend.BackendFactory
import org.koin.core.module.Module
import org.koin.core.qualifier.named

fun Module.developmentModuleAdditions() {
    single<Map<String, BackendFactory>>(named("developmentBackends")) {
        emptyMap()
    }
    single<List<AutoDiscovery>>(named("extraAutoDiscoveries")) {
        emptyList()
    }
}
