package app.k9mail.dev

import app.k9mail.backend.demo.DemoBackend
import app.k9mail.legacy.account.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mailstore.K9BackendStorageFactory

class DemoBackendFactory(private val backendStorageFactory: K9BackendStorageFactory) : BackendFactory {
    override fun createBackend(account: Account): Backend {
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        return DemoBackend(backendStorage)
    }
}
