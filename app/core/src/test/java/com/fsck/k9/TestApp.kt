package com.fsck.k9

import android.app.Application

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit(this)

        super.onCreate()
        DI.start(this, Core.coreModules)

        K9.init(this)
        Core.init(this)
    }
}
