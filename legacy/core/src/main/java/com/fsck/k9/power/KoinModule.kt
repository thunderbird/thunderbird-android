package com.fsck.k9.power

import android.content.Context
import com.fsck.k9.mail.power.PowerManager
import org.koin.dsl.module

val powerModule = module {
    factory { get<Context>().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
    single<PowerManager> { AndroidPowerManager(systemPowerManager = get()) }
}
