package net.thunderbird.android.dev

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.demo.DemoAutoDiscovery
import com.fsck.k9.backend.BackendFactory
import org.koin.core.module.Module
import org.koin.core.qualifier.named

fun Module.developmentModuleAdditions() {
    single { DemoBackendFactory(backendStorageFactory = get()) }
    single<Map<String, BackendFactory>>(named("developmentBackends")) {
        mapOf("demo" to get<DemoBackendFactory>())
    }
    single<List<AutoDiscovery>>(named("extraAutoDiscoveries")) {
        listOf(DemoAutoDiscovery())
    }
}
