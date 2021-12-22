package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.ListPreference
import com.fsck.k9.NotificationSetting
import com.takisoft.preferencex.PreferenceFragmentCompat

/**
 * Allows selecting a vibration pattern and specifying how often the vibration should repeat.
 */
@SuppressLint("RestrictedApi")
class VibrationPatternPreference
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
    internal var vibrationPattern: Int = DEFAULT_VIBRATION_PATTERN
        private set

    internal var vibrationTimes: Int = DEFAULT_VIBRATION_TIMES
        private set

    override fun onSetInitialValue(defaultValue: Any?) {
        val encoded = getPersistedString(defaultValue as String?)
        val (vibrationPattern, vibrationTimes) = decode(encoded)

        this.vibrationPattern = vibrationPattern
        this.vibrationTimes = vibrationTimes

        updateSummary()
    }

    override fun onClick() {
        preferenceManager.showDialog(this)
    }

    fun setVibrationPattern(vibrationPattern: Int, vibrationTimes: Int) {
        this.vibrationPattern = vibrationPattern
        this.vibrationTimes = vibrationTimes

        val encoded = encode(vibrationPattern, vibrationTimes)
        persistString(encoded)

        updateSummary()
    }

    fun setVibrationPatternFromSystem(combinedPattern: List<Long>?) {
        if (combinedPattern == null || combinedPattern.size < 2 || combinedPattern.size % 2 != 0) {
            setVibrationPattern(DEFAULT_VIBRATION_PATTERN, DEFAULT_VIBRATION_TIMES)
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

        setVibrationPattern(vibrationPattern, vibrationTimes)
    }

    private fun updateSummary() {
        val index = entryValues.indexOf(vibrationPattern.toString())
        summary = entries[index]
    }

    companion object {
        private const val DEFAULT_VIBRATION_PATTERN = 0
        private const val DEFAULT_VIBRATION_TIMES = 1

        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                VibrationPatternPreference::class.java, VibrationPatternDialogFragment::class.java
            )
        }

        fun encode(vibrationPattern: Int, vibrationTimes: Int): String {
            return "$vibrationPattern|$vibrationTimes"
        }

        fun decode(encoded: String): Pair<Int, Int> {
            val (vibrationPattern, vibrationTimes) = encoded.split('|').map { it.toInt() }
            return Pair(vibrationPattern, vibrationTimes)
        }
    }
}
