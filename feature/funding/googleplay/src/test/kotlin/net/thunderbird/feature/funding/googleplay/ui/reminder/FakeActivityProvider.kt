package net.thunderbird.feature.funding.googleplay.ui.reminder

import androidx.appcompat.app.AppCompatActivity
import net.thunderbird.core.android.common.activity.ActivityProvider

class FakeActivityProvider(
    private var activity: AppCompatActivity? = null,
) : ActivityProvider {
    override fun getCurrent(): AppCompatActivity? = activity

    fun setActivity(activity: AppCompatActivity?) {
        this.activity = activity
    }
}
