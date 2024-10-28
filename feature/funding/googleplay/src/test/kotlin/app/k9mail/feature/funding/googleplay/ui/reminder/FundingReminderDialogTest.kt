package app.k9mail.feature.funding.googleplay.ui.reminder

import android.app.Application
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.google.android.material.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class FundingReminderDialogTest {

    @Test
    fun `should call onOpenFunding when positive button is clicked`() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.Theme_Material3_Light)
        val dialog = FundingReminderDialog()
        var fundingOpened = false
        val onOpenFunding = { fundingOpened = true }

        dialog.show(activity, onOpenFunding)
        val shadowDialog = ShadowDialog.getLatestDialog()
        ShadowLooper.idleMainLooper()

        assertThat(shadowDialog.isShowing).isTrue()

        shadowDialog.findViewById<Button>(android.R.id.button1).performClick()
        ShadowLooper.idleMainLooper()

        assertThat(fundingOpened).isTrue()
    }

    @Test
    fun `should not call onOpenFunding when negative button is clicked`() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.Theme_Material3_Light)
        val dialog = FundingReminderDialog()
        var fundingOpened = false
        val onOpenFunding = { fundingOpened = true }

        dialog.show(activity, onOpenFunding)
        val shadowDialog = ShadowDialog.getLatestDialog()
        ShadowLooper.idleMainLooper()

        assertThat(shadowDialog.isShowing).isTrue()

        shadowDialog.findViewById<Button>(android.R.id.button2).performClick()
        ShadowLooper.idleMainLooper()

        assertThat(fundingOpened).isFalse()
    }
}

private class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_Material3_Light)
    }
}
