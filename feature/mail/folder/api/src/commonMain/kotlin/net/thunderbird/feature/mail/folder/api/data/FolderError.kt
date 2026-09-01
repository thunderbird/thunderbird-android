package net.thunderbird.feature.mail.folder.api.data

sealed interface FolderError {
    data object AccountNotFound : FolderError
    data object NotFound : FolderError
    data object Unavailable : FolderError
    data class FailedPrecondition(val message: String, val throwable: Throwable? = null) : FolderError
}
