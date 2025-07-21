package app.k9mail.feature.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import app.k9mail.feature.launcher.ui.FeatureLauncherApp
import com.fsck.k9.ui.base.K9Activity

class FeatureLauncherActivity : K9Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatureLauncherApp()
        }
    }

    companion object {
        @JvmStatic
        fun launch(context: Context, target: FeatureLauncherTarget) {
            val intent = getIntent(context, target)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(context: Context, target: FeatureLauncherTarget): Intent {
            return Intent(context, FeatureLauncherActivity::class.java).apply {
                data = target.deepLinkUri
                if (target.flags != null) {
                    flags = target.flags
                }
            }
        }
    }
}
