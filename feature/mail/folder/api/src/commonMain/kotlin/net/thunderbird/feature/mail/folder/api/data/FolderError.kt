package net.thunderbird.feature.mail.folder.api.data

public sealed interface FolderError {
    public val throwable: Throwable? get() = null

    public data class AccountNotFound(override val throwable: Throwable) : FolderError
    public data object NotFound : FolderError
    public data object Unavailable : FolderError
    public data class FailedPrecondition(val message: String, override val throwable: Throwable) : FolderError
    public data class FailedToQueryDatabase(val message: String, override val throwable: Throwable) : FolderError
}
