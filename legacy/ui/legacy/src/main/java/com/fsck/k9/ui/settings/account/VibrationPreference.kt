package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.ListPreference
import com.fsck.k9.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompat
import net.thunderbird.feature.notification.VibratePattern

/**
 * Preference to configure the vibration pattern used for a notification (enable/disable, pattern, repeat count).
 */
@SuppressLint("RestrictedApi")
class VibrationPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context,
        androidx.preference.R.attr.preferenceStyle,
        android.R.attr.preferenceStyle,
    ),
    defStyleRes: Int = 0,
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {
    internal var isVibrationEnabled: Boolean = false
        private set

    internal var vibratePattern = DEFAULT_VIBRATE_PATTERN
        private set

    internal var vibrationTimes: Int = DEFAULT_VIBRATION_TIMES
        private set

    override fun onSetInitialValue(defaultValue: Any?) {
        val encoded = getPersistedString(defaultValue as String?)
        val (isVibrationEnabled, vibrationPattern, vibrationTimes) = decode(encoded)

        this.isVibrationEnabled = isVibrationEnabled
        this.vibratePattern = vibrationPattern
        this.vibrationTimes = vibrationTimes

        updateSummary()
    }

    override fun onClick() {
        preferenceManager.showDialog(this)
    }

    fun setVibration(isVibrationEnabled: Boolean, vibratePattern: VibratePattern, vibrationTimes: Int) {
        this.isVibrationEnabled = isVibrationEnabled
        this.vibratePattern = vibratePattern
        this.vibrationTimes = vibrationTimes

        val encoded = encode(isVibrationEnabled, vibratePattern, vibrationTimes)
        persistString(encoded)

        updateSummary()
    }

    private fun updateSummary() {
        summary = if (isVibrationEnabled) {
            val index = entryValues.indexOf(vibratePattern.serialize().toString())
            entries[index]
        } else {
            context.getString(R.string.account_settings_vibrate_summary_disabled)
        }
    }

    companion object {
        private val DEFAULT_VIBRATE_PATTERN = VibratePattern.Default
        private const val DEFAULT_VIBRATION_TIMES = 1

        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                VibrationPreference::class.java,
                VibrationDialogFragment::class.java,
            )
        }

        fun encode(isVibrationEnabled: Boolean, vibratePattern: VibratePattern, vibrationTimes: Int): String {
            return "$isVibrationEnabled|${vibratePattern.name}|$vibrationTimes"
        }

        fun decode(encoded: String): Triple<Boolean, VibratePattern, Int> {
            val parts = encoded.split('|')
            val isVibrationEnabled = parts[0].toBoolean()
            val vibrationPattern = VibratePattern.valueOf(parts[1])
            val vibrationTimes = parts[2].toInt()
            return Triple(isVibrationEnabled, vibrationPattern, vibrationTimes)
        }
    }
}
