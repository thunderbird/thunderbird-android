package app.k9mail.core.android.common.compat

import android.os.Bundle
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.Serializable
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundleCompatTest {

    @Test
    fun `getSerializable returns Serializable`() {
        val bundle = Bundle()
        val key = "keySerializable"
        val serializable = TestSerializable("value")
        val clazz = TestSerializable::class.java
        bundle.putSerializable(key, serializable)

        val result = BundleCompat.getSerializable(bundle, key, clazz)

        assertThat(result).isEqualTo(serializable)
    }

    @Test
    fun `getSerializable returns null when class mismatch`() {
        val bundle = Bundle()
        val key = "keySerializable"
        val serializable = TestSerializable("value")
        val clazz = OtherTestSerializable::class.java
        bundle.putSerializable(key, serializable)

        val result = BundleCompat.getSerializable(bundle, key, clazz)

        assertThat(result).isEqualTo(null)
    }

    internal class TestSerializable(
        val value: String,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    internal class OtherTestSerializable(
        val value: String,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 2L
        }
    }
}
