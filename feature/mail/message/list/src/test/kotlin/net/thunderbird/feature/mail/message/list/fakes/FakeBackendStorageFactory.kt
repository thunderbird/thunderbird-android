package net.thunderbird.feature.mail.message.list.fakes

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import dev.mokkery.spy
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.feature.mail.account.api.BaseAccount

internal open class FakeBackendStorageFactory(
    backendFolderUpdater: FakeBackendFolderUpdater = FakeBackendFolderUpdater(),
) : BackendStorageFactory<BaseAccount> {
    val backendFolderUpdater = spy(backendFolderUpdater)

    override fun createBackendStorage(account: BaseAccount): BackendStorage = object : BackendStorage {
        override fun getFolder(folderServerId: String): BackendFolder = error("not implemented.")

        override fun getFolderServerIds(): List<String> = error("not implemented.")

        override fun createFolderUpdater(): BackendFolderUpdater = backendFolderUpdater

        override fun getExtraString(name: String): String? = error("not implemented.")

        override fun setExtraString(name: String, value: String) = error("not implemented.")

        override fun getExtraNumber(name: String): Long? = error("not implemented.")

        override fun setExtraNumber(name: String, value: Long) = error("not implemented.")
    }
}
