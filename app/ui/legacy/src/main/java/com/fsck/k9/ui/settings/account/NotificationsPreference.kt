package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.TypedArrayUtils
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import com.takisoft.preferencex.PreferenceFragmentCompat

typealias NotificationChannelIdProvider = () -> String

@SuppressLint("RestrictedApi")
@RequiresApi(Build.VERSION_CODES.O)
class NotificationsPreference
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
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    var notificationChannelIdProvider: NotificationChannelIdProvider? = null

    override fun onClick() {
        notificationChannelIdProvider.let { provider ->
            val intent = if (provider == null) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            } else {
                val notificationChannelId = provider.invoke()
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannelId)
                }
            }
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.context.packageName)
            startActivity(this.context, intent, null)
        }
    }

    companion object {
        init {
            PreferenceFragmentCompat.registerPreferenceFragment(
                NotificationsPreference::class.java,
                DialogFragment::class.java,
            )
        }
    }
}
