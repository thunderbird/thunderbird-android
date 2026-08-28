package com.fsck.k9.ui.compose

import android.os.Bundle
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import com.fsck.k9.message.QuotedTextMode
import com.fsck.k9.message.SimpleMessageFormat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuotedMessagePresenterTest {
    @Test
    fun `saved state should only contain quote restoration metadata`() {
        // Arrange
        val bundle = Bundle()

        // Act
        QuotedMessagePresenter.saveState(
            bundle,
            QuotedTextMode.SHOW,
            SimpleMessageFormat.HTML,
            true,
        )

        // Assert
        assertThat(bundle.keySet()).containsExactlyInAnyOrder(
            "state:quotedTextShown",
            "state:quotedTextFormat",
            "state:forcePlainText",
        )
    }
}
