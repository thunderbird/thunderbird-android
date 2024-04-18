package app.k9mail.feature.settings.push.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi

/**
 * Start the system activity to request the permission to schedule exact alarms.
 *
 * Note: [Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM] is documented to return [Activity.RESULT_OK] when the
 * permission was granted, [Activity.RESULT_CANCELED] otherwise. But at least on Android 14 `RESULT_CANCELED` is always
 * returned.
 * So the result mechanism should only be used as a trigger to check the permission again.
 */
internal class RequestAlarmPermission : ActivityResultContract<Unit, Unit>() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
        // We can't rely on the system activity returning a useful result.
    }
}
