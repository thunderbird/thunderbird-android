package com.fsck.k9.mail.folders

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage

/**
 * Fetches the list of folders from a server.
 *
 * @throws FolderFetcherException in case of an error
 */
fun interface FolderFetcher {
    fun getFolders(
        serverSettings: ServerSettings,
        authStateStorage: AuthStateStorage?,
    ): List<RemoteFolder>
}
