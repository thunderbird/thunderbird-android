package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.withStyledAttributes
import androidx.preference.DialogPreference
import com.fsck.k9.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompat

@SuppressLint("RestrictedApi")
class AutocryptPreferEncryptPreference
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
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    internal var isPreferEncryptEnabled: Boolean = false
        private set

    private var summaryOn: String? = null
    private var summaryOff: String? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.AutocryptPreferEncryptPreference, defStyleAttr, defStyleRes) {
            summaryOn = getString(R.styleable.AutocryptPreferEncryptPreference_summaryOn)
            summaryOff = getString(R.styleable.AutocryptPreferEncryptPreference_summaryOff)
        }
    }

    override fun onClick() {
        preferenceManager.showDialog(this)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        isPreferEncryptEnabled = getPersistedBoolean(defaultValue as? Boolean ?: false)
        updateSummary()
    }

    fun setPreferEncryptEnabled(newValue: Boolean) {
        if (callChangeListener(newValue)) {
            isPreferEncryptEnabled = newValue
            persistBoolean(newValue)

            updateSummary()
        }
    }

    private fun updateSummary() {
        summary = if (isPreferEncryptEnabled) summaryOn else summaryOff
    }

    companion object {
        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                AutocryptPreferEncryptPreference::class.java,
                AutocryptPreferEncryptDialogFragment::class.java,
            )
        }
    }
}
