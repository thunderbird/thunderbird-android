package app.k9mail.feature.settings.push.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import app.k9mail.feature.settings.push.R
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.controller.push.AlarmPermissionManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@SuppressLint("RestrictedApi")
class PushFoldersPreference
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
) : Preference(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
    private val alarmPermissionManager: AlarmPermissionManager by inject()

    private var folderPushMode: FolderMode = FolderMode.NONE

    override fun onSetInitialValue(defaultValue: Any?) {
        folderPushMode = getPersistedString(defaultValue as? String)?.let { FolderMode.valueOf(it) } ?: folderPushMode
        updateSummary()
    }

    fun onValueSelected(folderPushMode: FolderMode?) {
        if (folderPushMode != null) {
            this.folderPushMode = folderPushMode
            persistString(folderPushMode.name)
        }

        updateSummary()
    }

    private fun updateSummary() {
        val needAlarmPermission = !alarmPermissionManager.canScheduleExactAlarms()
        val displayName = folderPushMode.toResourceString(context.resources)

        summary = if (needAlarmPermission && folderPushMode != FolderMode.NONE) {
            context.getString(R.string.settings_push_alarm_permission_required, displayName)
        } else {
            displayName
        }
    }
}
