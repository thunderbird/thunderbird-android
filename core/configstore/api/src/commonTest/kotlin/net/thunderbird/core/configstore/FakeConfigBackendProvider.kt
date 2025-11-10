package net.thunderbird.core.configstore

import net.thunderbird.core.configstore.backend.ConfigBackend
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

internal class FakeConfigBackendProvider(private val backend: ConfigBackend) : ConfigBackendProvider {
    override fun provide(id: ConfigId): ConfigBackend = backend
}
