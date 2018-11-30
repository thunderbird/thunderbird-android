package com.fsck.k9

import android.content.Context
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.power.TracingPowerManager
import org.koin.dsl.module.applicationContext

val mainModule = applicationContext {
    bean { Preferences.getPreferences(get()) }
    bean { get<Context>().resources }
    bean { StorageManager.getInstance(get()) }
    bean { LocalStoreProvider() }
    bean { TracingPowerManager.getPowerManager(get()) as PowerManager }
    bean { Contacts.getInstance(get()) }
}
