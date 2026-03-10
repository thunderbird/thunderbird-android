package app.k9mail.dev

import app.k9mail.backend.demo.DemoBackend
import com.fsck.k9.backend.api.Backend
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.feature.account.AccountId

class DemoBackendFactory(
    private val backendStorageFactory: BackendStorageFactory,
) : BackendFactory {
    override fun createBackend(accountId: AccountId): Backend {
        val backendStorage = backendStorageFactory.createBackendStorage(accountId)
        return DemoBackend(backendStorage)
    }
}
