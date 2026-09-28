package app.k9mail.core.ui.legacy.theme2.common

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.EditText
import androidx.core.content.res.use
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import com.google.android.material.R as MaterialR

@RunWith(RobolectricTestRunner::class)
class Theme2MainLightTest {
    @Test
    fun `EditText uses primary fixed dim for text highlight`() {
        val context = RuntimeEnvironment.getApplication()
        val themedContext = ContextThemeWrapper(context, R.style.Theme2_Main_Light)

        val testSubject = EditText(themedContext)

        val expectedHighlightColor = themedContext.resolveColor(MaterialR.attr.colorPrimaryFixedDim)

        assertThat(testSubject.highlightColor).isEqualTo(expectedHighlightColor)
    }

    private fun Context.resolveColor(attributeId: Int): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(attributeId))

        return typedArray.use {
            it.getColor(0, 0)
        }
    }
}
