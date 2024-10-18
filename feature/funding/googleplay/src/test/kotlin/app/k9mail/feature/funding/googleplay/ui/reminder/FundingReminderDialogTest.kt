package app.k9mail.feature.funding.googleplay.ui.reminder

import android.app.Application
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import app.k9mail.core.testing.TestClock
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.datetime.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import com.google.android.material.R

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class FundingReminderDialogTest {

    @Test
    fun `should show dialog and set reminder shown timestamp on dismiss`() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.Theme_Material3_Light)
        val settings = FakeFundingSettings()
        val clock = TestClock(CURRENT_TIME_INSTANT)
        val dialog = FundingReminderDialog(settings, clock)

        dialog.show(activity) {}
        val shadowDialog = ShadowDialog.getLatestDialog()
        ShadowLooper.idleMainLooper()

        assertThat(shadowDialog.isShowing).isTrue()

        shadowDialog.dismiss()
        ShadowLooper.idleMainLooper()

        assertThat(settings.getReminderShownTimestamp()).isEqualTo(CURRENT_TIME)
    }

    @Test
    fun `should call onOpenFunding when positive button is clicked`() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.Theme_Material3_Light)
        val settings = FakeFundingSettings()
        val clock = TestClock(CURRENT_TIME_INSTANT)
        val dialog = FundingReminderDialog(settings, clock)
        var fundingOpened = false
        val onOpenFunding = { fundingOpened = true }

        dialog.show(activity, onOpenFunding)
        val shadowDialog = ShadowDialog.getLatestDialog()
        ShadowLooper.idleMainLooper()

        assertThat(shadowDialog.isShowing).isTrue()

        shadowDialog.findViewById<Button>(android.R.id.button1).performClick()
        ShadowLooper.idleMainLooper()

        // Then
        assertThat(fundingOpened).isTrue()
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(CURRENT_TIME)
    }

    @Test
    fun `should not call onOpenFunding when negative button is clicked`() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.Theme_Material3_Light)
        val settings = FakeFundingSettings()
        val clock = TestClock(CURRENT_TIME_INSTANT)
        val dialog = FundingReminderDialog(settings, clock)
        var fundingOpened = false
        val onOpenFunding = { fundingOpened = true }

        dialog.show(activity, onOpenFunding)
        val shadowDialog = ShadowDialog.getLatestDialog()
        ShadowLooper.idleMainLooper()

        assertThat(shadowDialog.isShowing).isTrue()

        shadowDialog.findViewById<Button>(android.R.id.button2).performClick()
        ShadowLooper.idleMainLooper()

        // Then
        assertThat(fundingOpened).isFalse()
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(CURRENT_TIME)
    }

    private companion object {
        private const val CURRENT_TIME = 1000L
        private val CURRENT_TIME_INSTANT = Instant.fromEpochMilliseconds(CURRENT_TIME)
    }
}

private class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_Material3_Light)
    }
}
