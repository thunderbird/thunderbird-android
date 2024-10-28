package app.k9mail.feature.funding.googleplay.ui.reminder

import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.testing.FragmentScenario.FragmentAction
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.google.android.material.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FundingReminderDialogFragmentTest {

    @Test
    fun `should return 'show_funding = true' in result bundle when positive button is clicked`() {
        val resultBundle = startFundingReminderDialogFragmentForResult { fragment ->
            val dialog = checkNotNull(fragment.dialog)
            dialog.findViewById<Button>(android.R.id.button1).performClick()
        }

        assertThat(resultBundle).prop_showFunding().isTrue()
    }

    @Test
    fun `should not return result when negative button is clicked`() {
        val resultBundle = startFundingReminderDialogFragmentForResult { fragment ->
            val dialog = checkNotNull(fragment.dialog)
            dialog.findViewById<Button>(android.R.id.button2).performClick()
        }

        assertThat(resultBundle).isNull()
    }

    private fun startFundingReminderDialogFragmentForResult(
        action: FragmentAction<FundingReminderDialogFragment>,
    ): Bundle? {
        var resultBundle: Bundle? = null

        with(launchFragment<FundingReminderDialogFragment>(themeResId = R.style.Theme_Material3_Light)) {
            onFragment { fragment ->
                fragment.parentFragmentManager.setFragmentResultListener(
                    FundingReminderContract.Dialog.FRAGMENT_REQUEST_KEY,
                    fragment,
                ) { requestKey, result ->
                    assertThat(requestKey).isEqualTo(FundingReminderContract.Dialog.FRAGMENT_REQUEST_KEY)
                    resultBundle = result
                }
            }
            moveToState(Lifecycle.State.RESUMED)

            onFragment(action)

            moveToState(Lifecycle.State.DESTROYED)
        }

        return resultBundle
    }

    private fun Assert<Bundle?>.prop_showFunding(): Assert<Boolean> {
        return isNotNull()
            .transform { it.getBoolean(FundingReminderContract.Dialog.FRAGMENT_RESULT_SHOW_FUNDING, false) }
    }
}
