package com.fsck.k9.mail.ssl

import java.io.File

fun interface KeyStoreDirectoryProvider {
    fun getDirectory(): File
}
