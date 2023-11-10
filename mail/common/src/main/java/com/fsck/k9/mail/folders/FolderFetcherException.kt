package com.fsck.k9.mail.folders

/**
 * Thrown by [FolderFetcher] in case of an error.
 */
class FolderFetcherException(
    cause: Throwable,
    val messageFromServer: String? = null,
) : RuntimeException(cause.message, cause)
