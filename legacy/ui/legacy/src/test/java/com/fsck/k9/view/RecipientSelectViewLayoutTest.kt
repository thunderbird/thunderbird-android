package com.fsck.k9.view

import androidx.appcompat.app.AppCompatActivity
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.ui.R
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric

@RunWith(ParameterizedRobolectricTestRunner::class)
class RecipientSelectViewLayoutTest(
    private val input: String,
    private val expectedOutPut: Boolean,
) : K9RobolectricTest() {
    private lateinit var activity: AppCompatActivity
    private lateinit var view: RecipientSelectView

    @Before
    fun setUp() {
        Log.logger = TestLogger()
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).get()
        activity.setTheme(R.style.Theme_Legacy_Test)
        view = RecipientSelectView(activity)
    }

    @Test
    fun `hasUncompletedText should return true for valid emails but false for invalid emails`() {
        view.setText(input)
        view.tryPerformCompletion()
        assertThat(view.hasUncompletedText()).isEqualTo(expectedOutPut)
    }

    companion object {
        @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = " Expected hasCompletedText()->{1} for Input-> {0} ")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // Space check
                arrayOf("test1@gmail.com ", false), // space after email
                arrayOf(" test1@gmail.com", false), // space before email
                arrayOf(" test1@gmail.com ", false), // space before and after

                // Non-Email Characters/invalid format (Error Trigger)
                arrayOf("test1@gmail@com", true), // Double @
                arrayOf("test:1@gmail.com", true), // : symbol
                arrayOf("test1gmail.com", true), // no @

                // Spaces/comma/semicolon Within Pasted Text of Multiple Recipients
                arrayOf("test1@gmail.com test2@gmail.com", true), // no separator between recipients
                arrayOf("test1@gmail.com ;  test2@gmail.com,test3@gmail.com", false), // separators among recipients having/not having space
            )
        }
    }
}
