package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.res.TypedArrayUtils
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.preference.TwoStatePreference
import android.util.AttributeSet
import com.fsck.k9.R
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

@SuppressLint("RestrictedApi")
class AutocryptPreferEncryptPreference
@JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, android.support.v7.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle),
        defStyleRes: Int = 0
) : TwoStatePreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.AutocryptPreferEncryptPreference,
                defStyleAttr, defStyleRes)

        summaryOn = attributes.getString(R.styleable.AutocryptPreferEncryptPreference_summaryOn)
        summaryOff = attributes.getString(R.styleable.AutocryptPreferEncryptPreference_summaryOff)

        attributes.recycle()
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
                    AutocryptPreferEncryptPreference::class.java, AutocryptPreferEncryptDialogFragment::class.java)
        }
    }
}
