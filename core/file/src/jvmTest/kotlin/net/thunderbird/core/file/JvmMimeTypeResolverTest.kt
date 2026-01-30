package net.thunderbird.core.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.eygraber.uri.Uri
import org.junit.Test

class JvmMimeTypeResolverTest {

    private val testSubject = JvmMimeTypeResolver()

    @Test
    fun `should resolve jpeg from content uri extension`() {
        val uri = Uri.parse("content://com.example/images/photo.jpg")
        assertThat(testSubject.getMimeType(uri)).isEqualTo(MimeType.JPEG)
    }

    @Test
    fun `should resolve png from content uri extension`() {
        val uri = Uri.parse("content://com.example/images/image.png")
        assertThat(testSubject.getMimeType(uri)).isEqualTo(MimeType.PNG)
    }

    @Test
    fun `should resolve pdf from content uri extension`() {
        val uri = Uri.parse("content://com.example/docs/file.pdf")
        assertThat(testSubject.getMimeType(uri)).isEqualTo(MimeType.PDF)
    }

    @Test
    fun `should return null for unknown extension`() {
        val uri = Uri.parse("content://com.example/files/file.unknownext")
        assertThat(testSubject.getMimeType(uri)).isNull()
    }
}
