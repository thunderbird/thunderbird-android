package com.fsck.k9.helper

import android.content.Context
import com.fsck.k9.mail.ssl.KeyStoreDirectoryProvider
import java.io.File

internal class AndroidKeyStoreDirectoryProvider(private val context: Context) : KeyStoreDirectoryProvider {
    override fun getDirectory(): File {
        return context.getDir("KeyStore", Context.MODE_PRIVATE)
    }
}
