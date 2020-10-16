package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.withStyledAttributes
import androidx.preference.PreferenceViewHolder
import androidx.preference.TwoStatePreference
import com.fsck.k9.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompat

@SuppressLint("RestrictedApi")
class AutocryptPreferEncryptPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context, androidx.preference.R.attr.preferenceStyle,
        android.R.attr.preferenceStyle
    ),
    defStyleRes: Int = 0
) : TwoStatePreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        context.withStyledAttributes(attrs, R.styleable.AutocryptPreferEncryptPreference, defStyleAttr, defStyleRes) {
            summaryOn = getString(R.styleable.AutocryptPreferEncryptPreference_summaryOn)
            summaryOff = getString(R.styleable.AutocryptPreferEncryptPreference_summaryOff)
        }
    }

    override fun onClick() {
        preferenceManager.showDialog(this)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        syncSummaryView(holder)
    }

    fun userChangedValue(newValue: Boolean) {
        if (callChangeListener(newValue)) {
            isChecked = newValue
        }
    }

    companion object {
        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                AutocryptPreferEncryptPreference::class.java, AutocryptPreferEncryptDialogFragment::class.java
            )
        }
    }
}
