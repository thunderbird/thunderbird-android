package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import android.util.AttributeSet
import com.takisoft.preferencex.PreferenceFragmentCompat

@SuppressLint("RestrictedApi")
class NotificationsPreference
@JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, androidx.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle),
        defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    override fun onClick() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.context.packageName)
        startActivity(this.context, intent, null)
    }

    companion object {
        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                    NotificationsPreference::class.java, DialogFragment::class.java)
        }
    }
}
