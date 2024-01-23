package app.k9mail.core.ui.compose.common.fragment

import android.widget.Switch
import app.k9mail.core.ui.compose.common.test.R
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isTrue
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FragmentViewTest {
    @Test
    fun `should inflate fragment and add to view hierarchy`() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()

            val switch = activity.findViewById<Switch>(R.id.core_ui_compose_common_test_fragment_switch)

            assertThat(switch.isChecked).isFalse()
        }
    }

    @Test
    fun `fragment state should be restored after restart`() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val firstActivity = controller.get()

            val firstSwitch = firstActivity.findViewById<Switch>(R.id.core_ui_compose_common_test_fragment_switch)
            assertThat(firstSwitch.isChecked).isFalse()
            firstSwitch.performClick()

            controller.recreate()
            val secondActivity = controller.get()
            assertThat(secondActivity).isNotSameInstanceAs(firstActivity)

            val secondSwitch = secondActivity.findViewById<Switch>(R.id.core_ui_compose_common_test_fragment_switch)
            assertThat(secondSwitch.isChecked).isTrue()
        }
    }
}
