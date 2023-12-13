package app.k9mail.feature.account.common.domain.entity

import com.fsck.k9.mail.folders.RemoteFolder

sealed interface SpecialFolderOption {
    data class None(
        val isAutomatic: Boolean = false,
    ) : SpecialFolderOption

    data class Regular(
        val remoteFolder: RemoteFolder,
    ) : SpecialFolderOption

    data class Special(
        val isAutomatic: Boolean = false,
        val remoteFolder: RemoteFolder,
    ) : SpecialFolderOption
}
