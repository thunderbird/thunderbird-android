package com.fsck.k9.ui.compose

import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric

class RecipientTokenLayoutTest : RobolectricTest() {
    private lateinit var activity: AppCompatActivity

    private lateinit var recipientTokenLayout: RecipientTokenLayout

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).get()
        activity.setTheme(R.style.Theme_Legacy_Test)

        recipientTokenLayout =
            activity.layoutInflater.inflate(R.layout.recipient_token_item, null, false) as RecipientTokenLayout
    }

    @Test
    fun `measure with width constraint`() {
        val maxWidth = 100
        recipientTokenLayout.recipientNameView.text = "recipient@domain.example"

        recipientTokenLayout.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )

        assertThat(recipientTokenLayout.measuredWidth).isEqualTo(81)
        assertThat(recipientTokenLayout.measuredHeight).isEqualTo(49)
    }

    @Test
    fun `respect max width when measuring`() {
        val maxWidth = 70
        recipientTokenLayout.recipientNameView.text = "recipient@domain.example"

        recipientTokenLayout.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )

        assertThat(recipientTokenLayout.measuredWidth).isEqualTo(maxWidth)
    }

    @Test
    fun `layout without reaching the maximum width`() {
        val maxWidth = 100
        recipientTokenLayout.recipientNameView.text = "recipient@domain.example"
        recipientTokenLayout.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )

        recipientTokenLayout.layout(0, 0, recipientTokenLayout.measuredWidth, recipientTokenLayout.measuredHeight)

        assertThat(recipientTokenLayout.width).isEqualTo(81)
        assertThat(recipientTokenLayout.height).isEqualTo(49)

        assertThat(recipientTokenLayout.contactPictureView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.contactPictureView.bottom).isEqualTo(49)
        assertThat(recipientTokenLayout.contactPictureView.left).isEqualTo(0)
        assertThat(recipientTokenLayout.contactPictureView.right).isEqualTo(49)

        assertThat(recipientTokenLayout.recipientNameView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.recipientNameView.bottom).isEqualTo(49)
        assertThat(recipientTokenLayout.recipientNameView.left).isEqualTo(49)
        assertThat(recipientTokenLayout.recipientNameView.right).isEqualTo(81)

        assertThat(recipientTokenLayout.cryptoStatusView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.cryptoStatusView.bottom).isEqualTo(0)
        assertThat(recipientTokenLayout.cryptoStatusView.left).isEqualTo(81)
        assertThat(recipientTokenLayout.cryptoStatusView.right).isEqualTo(81)

        assertThat(recipientTokenLayout.backgroundView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.backgroundView.bottom).isEqualTo(49)
        assertThat(recipientTokenLayout.backgroundView.left).isEqualTo(24)
        assertThat(recipientTokenLayout.backgroundView.right).isEqualTo(81)
    }

    @Test
    fun `layout with ellipsized text and crypto status indicator`() {
        val maxWidth = 70
        recipientTokenLayout.recipientNameView.text = "recipient@domain.example"
        recipientTokenLayout.cryptoStatusView.findViewById<View>(R.id.contact_crypto_status_icon).isVisible = true
        recipientTokenLayout.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )

        recipientTokenLayout.layout(0, 0, recipientTokenLayout.measuredWidth, recipientTokenLayout.measuredHeight)

        assertThat(recipientTokenLayout.width).isEqualTo(70)
        assertThat(recipientTokenLayout.height).isEqualTo(49)

        assertThat(recipientTokenLayout.contactPictureView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.contactPictureView.bottom).isEqualTo(49)
        assertThat(recipientTokenLayout.contactPictureView.left).isEqualTo(0)
        assertThat(recipientTokenLayout.contactPictureView.right).isEqualTo(49)

        assertThat(recipientTokenLayout.recipientNameView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.recipientNameView.bottom).isEqualTo(49)
        assertThat(recipientTokenLayout.recipientNameView.left).isEqualTo(49)
        assertThat(recipientTokenLayout.recipientNameView.right).isEqualTo(58)

        assertThat(recipientTokenLayout.cryptoStatusView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.cryptoStatusView.bottom).isEqualTo(12)
        assertThat(recipientTokenLayout.cryptoStatusView.left).isEqualTo(58)
        assertThat(recipientTokenLayout.cryptoStatusView.right).isEqualTo(70)

        assertThat(recipientTokenLayout.backgroundView.top).isEqualTo(0)
        assertThat(recipientTokenLayout.backgroundView.bottom).isEqualTo(49)
        assertThat(recipientTokenLayout.backgroundView.left).isEqualTo(24)
        assertThat(recipientTokenLayout.backgroundView.right).isEqualTo(70)
    }
}

private val RecipientTokenLayout.backgroundView: View
    get() = findViewById(R.id.background)

private val RecipientTokenLayout.contactPictureView: View
    get() = findViewById(R.id.contact_photo)

private val RecipientTokenLayout.recipientNameView: MaterialTextView
    get() = findViewById(android.R.id.text1)

private val RecipientTokenLayout.cryptoStatusView: ViewGroup
    get() = findViewById(R.id.crypto_status_container)
