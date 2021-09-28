package app.k9mail.dev

import com.fsck.k9.backend.BackendFactory
import org.koin.core.module.Module
import org.koin.core.scope.Scope

fun Scope.developmentBackends() = emptyMap<String, BackendFactory>()

fun Module.developmentModuleAdditions() = Unit
