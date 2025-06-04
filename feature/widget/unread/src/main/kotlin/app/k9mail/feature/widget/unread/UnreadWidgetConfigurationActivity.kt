package app.k9mail.feature.widget.unread

import android.appwidget.AppWidgetManager
import android.os.Bundle
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.base.extensions.fragmentTransaction
import net.thunderbird.core.logging.legacy.Log

/**
 * Activity to select an account for the unread widget.
 */
class UnreadWidgetConfigurationActivity : K9Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_unread_widget_configuration)
        setTitle(R.string.unread_widget_select_account)

        var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e("Received an invalid widget ID")
            finish()
            return
        }

        if (savedInstanceState == null) {
            fragmentTransaction {
                add(R.id.fragment_container, UnreadWidgetConfigurationFragment.create(appWidgetId))
            }
        }
    }
}
