package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.ListPreference
import com.fsck.k9.NotificationSetting
import com.fsck.k9.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompat

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
        android.R.attr.preferenceStyle
    ),
    defStyleRes: Int = 0
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {
    internal var isVibrationEnabled: Boolean = false
        private set

    internal var vibrationPattern: Int = DEFAULT_VIBRATION_PATTERN
        private set

    internal var vibrationTimes: Int = DEFAULT_VIBRATION_TIMES
        private set

    override fun onSetInitialValue(defaultValue: Any?) {
        val encoded = getPersistedString(defaultValue as String?)
        val (isVibrationEnabled, vibrationPattern, vibrationTimes) = decode(encoded)

        this.isVibrationEnabled = isVibrationEnabled
        this.vibrationPattern = vibrationPattern
        this.vibrationTimes = vibrationTimes

        updateSummary()
    }

    override fun onClick() {
        preferenceManager.showDialog(this)
    }

    fun setVibration(isVibrationEnabled: Boolean, vibrationPattern: Int, vibrationTimes: Int) {
        this.isVibrationEnabled = isVibrationEnabled
        this.vibrationPattern = vibrationPattern
        this.vibrationTimes = vibrationTimes

        val encoded = encode(isVibrationEnabled, vibrationPattern, vibrationTimes)
        persistString(encoded)

        updateSummary()
    }

    fun setVibrationFromSystem(isVibrationEnabled: Boolean, combinedPattern: List<Long>?) {
        if (combinedPattern == null || combinedPattern.size < 2 || combinedPattern.size % 2 != 0) {
            setVibration(isVibrationEnabled, DEFAULT_VIBRATION_PATTERN, DEFAULT_VIBRATION_TIMES)
            return
        }

        val combinedPatternArray = combinedPattern.toLongArray()
        val vibrationTimes = combinedPattern.size / 2
        val vibrationPattern = entryValues.asSequence()
            .map { entryValue -> entryValue.toString().toInt() }
            .firstOrNull { vibrationPattern ->
                val testPattern = NotificationSetting.getVibration(vibrationPattern, vibrationTimes)

                testPattern.contentEquals(combinedPatternArray)
            } ?: DEFAULT_VIBRATION_PATTERN

        setVibration(isVibrationEnabled, vibrationPattern, vibrationTimes)
    }

    private fun updateSummary() {
        summary = if (isVibrationEnabled) {
            val index = entryValues.indexOf(vibrationPattern.toString())
            entries[index]
        } else {
            context.getString(R.string.account_settings_vibrate_summary_disabled)
        }
    }

    companion object {
        private const val DEFAULT_VIBRATION_PATTERN = 0
        private const val DEFAULT_VIBRATION_TIMES = 1

        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                VibrationPreference::class.java, VibrationDialogFragment::class.java
            )
        }

        fun encode(isVibrationEnabled: Boolean, vibrationPattern: Int, vibrationTimes: Int): String {
            return "$isVibrationEnabled|$vibrationPattern|$vibrationTimes"
        }

        fun decode(encoded: String): Triple<Boolean, Int, Int> {
            val parts = encoded.split('|')
            val isVibrationEnabled = parts[0].toBoolean()
            val vibrationPattern = parts[1].toInt()
            val vibrationTimes = parts[2].toInt()
            return Triple(isVibrationEnabled, vibrationPattern, vibrationTimes)
        }
    }
}
