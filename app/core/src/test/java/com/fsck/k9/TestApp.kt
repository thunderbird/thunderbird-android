package com.fsck.k9

import android.app.Application
import org.koin.dsl.module.applicationContext

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit(this)

        super.onCreate()
        DI.start(this, Core.coreModules + testModule)

        K9.init(this)
        Core.init(this)
    }
}

val testModule = applicationContext {
    bean { AppConfig(emptyList()) }
}
