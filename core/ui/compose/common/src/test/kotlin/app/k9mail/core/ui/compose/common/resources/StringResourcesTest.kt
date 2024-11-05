package app.k9mail.core.ui.compose.common.resources

import androidx.compose.ui.text.buildAnnotatedString
import app.k9mail.core.ui.compose.common.test.R
import app.k9mail.core.ui.compose.common.text.bold
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class StringResourcesTest : ComposeTest() {
    @Test
    fun `annotatedStringResource() with bold text`() = runComposeTest {
        val argument = "text".bold()

        setContent {
            val result = annotatedStringResource(id = R.string.StringResourcesTest, argument)

            assertThat(result).isEqualTo(
                buildAnnotatedString {
                    append("prefix ")
                    append(argument)
                    append(" suffix")
                },
            )
        }
    }
}
