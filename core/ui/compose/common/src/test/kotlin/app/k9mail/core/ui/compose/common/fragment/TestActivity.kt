package app.k9mail.core.ui.compose.common.fragment

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import app.k9mail.core.ui.compose.common.activity.LocalActivity

class TestActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalActivity provides this) {
                FragmentView(
                    fragmentFactory = { TestFragment() },
                )
            }
        }
    }
}
