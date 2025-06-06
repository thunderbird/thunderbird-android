package com.fsck.k9.helper

import android.app.AlarmManager
import android.content.Context
import com.fsck.k9.mail.ssl.KeyStoreDirectoryProvider
import org.koin.dsl.module

val helperModule = module {
    single { ClipboardManager(get()) }
    single { MessageHelper(resourceProvider = get(), contactRepository = get(), generalSettingsManager = get()) }
    factory<KeyStoreDirectoryProvider> { AndroidKeyStoreDirectoryProvider(context = get()) }
    factory { get<Context>().getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    factory<ContactNameProvider> { RealContactNameProvider(contactRepository = get()) }
}
