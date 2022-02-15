package com.fsck.k9

import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy

fun enableStrictMode() {
    StrictMode.setThreadPolicy(createThreadPolicy())
    StrictMode.setVmPolicy(createVmPolicy())
}

private fun createThreadPolicy(): ThreadPolicy {
    return ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build()
}

private fun createVmPolicy(): VmPolicy {
    return VmPolicy.Builder()
        .detectActivityLeaks()
        .detectLeakedClosableObjects()
        .detectLeakedRegistrationObjects()
        .detectFileUriExposure()
        .detectLeakedSqlLiteObjects()
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                detectContentUriWithoutPermission()

                // Disabled because we currently don't use tagged sockets; so this would generate a lot of noise
                // detectUntaggedSockets()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                detectCredentialProtectedWhileLocked()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                detectIncorrectContextUse()
                detectUnsafeIntentLaunch()
            }
        }
        .penaltyLog()
        .build()
}
