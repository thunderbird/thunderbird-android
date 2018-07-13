package com.fsck.k9

import android.content.Context
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.power.TracingPowerManager
import org.koin.dsl.module.applicationContext

val mainModule = applicationContext {
    bean { Preferences.getPreferences(get()) }
    bean { MessagingController.getInstance(get()) }
    bean { get<Context>().resources }
    bean { StorageManager.getInstance(get()) }
    bean { TracingPowerManager.getPowerManager(get()) as PowerManager }
}
