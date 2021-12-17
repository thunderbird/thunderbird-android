package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.ListPreference
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
    internal var vibrationPattern: Int = 0
        private set

    internal var vibrationTimes: Int = 1
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

    private fun updateSummary() {
        val index = entryValues.indexOf(vibrationPattern.toString())
        summary = entries[index]
    }

    companion object {
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
