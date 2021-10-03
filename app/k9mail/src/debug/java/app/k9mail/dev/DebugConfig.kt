package app.k9mail.dev

import org.koin.core.module.Module
import org.koin.core.scope.Scope

fun Scope.developmentBackends() = mapOf(
    "demo" to get<DemoBackendFactory>()
)

fun Module.developmentModuleAdditions() {
    single { DemoBackendFactory(backendStorageFactory = get()) }
}
