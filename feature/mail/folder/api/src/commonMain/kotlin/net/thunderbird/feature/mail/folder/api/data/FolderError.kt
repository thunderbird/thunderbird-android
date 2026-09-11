package net.thunderbird.feature.mail.folder.api.data

sealed interface FolderError {
    val throwable: Throwable? get() = null

    data class AccountNotFound(override val throwable: Throwable) : FolderError
    data object NotFound : FolderError
    data object Unavailable : FolderError
    data class FailedPrecondition(val message: String, override val throwable: Throwable) : FolderError
    data class FailedToQueryDatabase(val message: String, override val throwable: Throwable) : FolderError
}
