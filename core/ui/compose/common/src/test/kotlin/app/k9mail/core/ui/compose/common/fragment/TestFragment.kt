package app.k9mail.core.ui.compose.common.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import app.k9mail.core.ui.compose.common.test.R

class TestFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return Switch(context).apply {
            id = R.id.core_ui_compose_common_test_fragment_switch
            isChecked = false
        }
    }
}
