package net.thunderbird.core.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.eygraber.uri.Uri
import org.junit.Test

class AndroidMimeTypeResolverTest {

    @Test
    fun `should return correct mime type for content uri without extension`() {
        // Arrange
        val imageUri = Uri.parse("content://com.android.providers.media.documents/document/image:12345")
        val provider = FakeMimeTypeProvider(
            mapOf(
                imageUri to "image/jpeg",
            ),
        )
        val testSubject = AndroidMimeTypeResolver(provider)

        // Act
        val result = testSubject.getMimeType(imageUri)

        // Assert
        assertThat(result).isEqualTo(MimeType.JPEG)
    }

    @Test
    fun `should return correct mime type for file uri with extension`() {
        // Arrange
        val fileUri = Uri.parse("file:///storage/emulated/0/Download/document.pdf")
        val provider = FakeMimeTypeProvider(
            mapOf(
                fileUri to "application/pdf",
            ),
        )
        val testSubject = AndroidMimeTypeResolver(provider)

        // Act
        val result = testSubject.getMimeType(fileUri)

        // Assert
        assertThat(result).isEqualTo(MimeType.PDF)
    }

    @Test
    fun `should return correct mime type for known types`() {
        // Arrange
        val jpgUri = Uri.parse("content://com.example.images/photo.jpg")
        val jpegUri = Uri.parse("content://com.example.images/photo.jpeg")
        val pngUri = Uri.parse("content://com.example.images/image.png")
        val pdfUri = Uri.parse("content://com.example.documents/doc.pdf")

        val provider = FakeMimeTypeProvider(
            mapOf(
                jpgUri to "image/jpeg",
                jpegUri to "image/jpeg",
                pngUri to "image/png",
                pdfUri to "application/pdf",
            ),
        )
        val testSubject = AndroidMimeTypeResolver(provider)

        // Act & Assert
        assertThat(testSubject.getMimeType(jpgUri)).isEqualTo(MimeType.JPEG)
        assertThat(testSubject.getMimeType(jpegUri)).isEqualTo(MimeType.JPEG)
        assertThat(testSubject.getMimeType(pngUri)).isEqualTo(MimeType.PNG)
        assertThat(testSubject.getMimeType(pdfUri)).isEqualTo(MimeType.PDF)
    }

    @Test
    fun `should return null for unknown mime type`() {
        // Arrange
        val provider = FakeMimeTypeProvider()
        val testSubject = AndroidMimeTypeResolver(provider)
        val uri = Uri.parse("content://com.example.unknown/file.unknown")

        // Act
        val result = testSubject.getMimeType(uri)

        // Assert
        assertThat(result).isNull()
    }
}
