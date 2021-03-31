package com.fsck.k9.helper

import com.fsck.k9.mail.ssl.KeyStoreDirectoryProvider
import org.koin.dsl.module

val helperModule = module {
    single { ClipboardManager(get()) }
    single { MessageHelper.getInstance(get()) }
    factory<KeyStoreDirectoryProvider> { AndroidKeyStoreDirectoryProvider(context = get()) }
}
