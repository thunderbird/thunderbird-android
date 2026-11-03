package net.thunderbird.core.android.common.keypad

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

/** Requires permission and system-reported idle telephone and communication audio before enabling shortcuts. */
class PhoneIdleProvider(private val context: Context) {
    fun isIdle(): Boolean {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        return try {
            val telecom = context.getSystemService(TelecomManager::class.java)
            val telephony = context.getSystemService(TelephonyManager::class.java)
            val audio = context.getSystemService(AudioManager::class.java)

            // The cellular state can report ringing before Telecom has finished creating its call object.
            @Suppress("DEPRECATION")
            val isCellularIdle = telephony?.callState == TelephonyManager.CALL_STATE_IDLE
            telecom?.isInCall == false && isCellularIdle && audio?.mode == AudioManager.MODE_NORMAL
        } catch (_: RuntimeException) {
            // A denied permission or a failed service query must not enable a phone-key shortcut.
            false
        }
    }
}
