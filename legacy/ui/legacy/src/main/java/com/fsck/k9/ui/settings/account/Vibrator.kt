package com.fsck.k9.ui.settings.account

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import androidx.core.content.getSystemService
import android.os.Vibrator as VibratorService

interface Vibrator {
    val hasVibrator: Boolean
    fun vibrate(vibrationPattern: LongArray)
}

internal class AndroidVibrator(private val vibrator: VibratorService) : Vibrator {
    override val hasVibrator: Boolean
        get() = vibrator.hasVibrator()

    override fun vibrate(vibrationPattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationPattern, -1)
        }
    }
}

internal fun getSystemVibrator(context: Context): Vibrator {
    val vibratorService = context.getSystemService<VibratorService>() ?: error("Vibrator service missing")
    return AndroidVibrator(vibratorService)
}
