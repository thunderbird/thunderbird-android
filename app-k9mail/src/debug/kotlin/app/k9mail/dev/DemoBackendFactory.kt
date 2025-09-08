package app.k9mail.dev

import app.k9mail.backend.demo.DemoBackend
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mailstore.K9BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountDto

class DemoBackendFactory(private val backendStorageFactory: K9BackendStorageFactory) : BackendFactory {
    override fun createBackend(account: LegacyAccountDto): Backend {
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        return DemoBackend(backendStorage)
    }
}
