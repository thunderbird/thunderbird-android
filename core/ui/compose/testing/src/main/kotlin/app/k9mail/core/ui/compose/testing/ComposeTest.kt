package app.k9mail.core.ui.compose.testing

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
open class ComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    fun getString(@StringRes resourceId: Int): String = RuntimeEnvironment.getApplication().getString(resourceId)

    fun runComposeTest(testContent: ComposeContentTestRule.() -> Unit): Unit = with(composeTestRule) {
        testContent()
    }
}
