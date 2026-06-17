package app.k9mail.feature.account.setup.domain.usecase

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.folders.FolderFetcher
import com.fsck.k9.mail.folders.RemoteFolder
import com.fsck.k9.mail.oauth.AuthStateStorage

class FakeFolderFetcher(
    private val folders: List<RemoteFolder> = emptyList(),
) : FolderFetcher {
    var lastServerSettings: ServerSettings? = null
        private set

    override fun getFolders(
        serverSettings: ServerSettings,
        authStateStorage: AuthStateStorage?,
    ): List<RemoteFolder> {
        lastServerSettings = serverSettings
        return folders
    }
}
