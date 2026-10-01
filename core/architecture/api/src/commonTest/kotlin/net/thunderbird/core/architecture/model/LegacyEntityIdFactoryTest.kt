package net.thunderbird.core.architecture.model

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory.ByteRepresentation

@OptIn(ExperimentalUuidApi::class)
class LegacyEntityIdFactoryTest {

    @Test
    fun `of should generate UUID following the documented byte layout when given a legacy id`() {
        // Arrange
        val byteRepresentation = ByteRepresentation.MESSAGE
        val testSubject = TestLegacyEntityIdFactory(byteRepresentation)
        val legacyId = 12345L
        val expectedUuid = buildExpectedUuid(
            upper16BytesAsString = "0000",
            lower48BytesAsString = "000000003039",
            byteRepresentation = byteRepresentation,
        )

        // Act
        val id = testSubject.of(legacyId)

        // Assert
        assertThat(id.toString()).isEqualTo(expectedUuid)
    }

    @Test
    fun `of should generate UUID with only fixed marker bytes when given a zero legacy id`() {
        // Arrange
        val byteRepresentation = ByteRepresentation.THREADS
        val testSubject = TestLegacyEntityIdFactory(byteRepresentation)
        val legacyId = 0L
        val expectedUuid = buildExpectedUuid(
            upper16BytesAsString = "0000",
            lower48BytesAsString = "000000000000",
            byteRepresentation = byteRepresentation,
        )

        // Act
        val id = testSubject.of(legacyId)

        // Assert
        assertThat(id.toString()).isEqualTo(expectedUuid)
    }

    @Test
    fun `of should embed entity byte representation in the UUID when given different entity types`() {
        // Arrange
        val messageByteRepresentation = ByteRepresentation.MESSAGE
        val messageFactory = TestLegacyEntityIdFactory(messageByteRepresentation)
        val notificationsByteRepresentation = ByteRepresentation.NOTIFICATIONS
        val notificationFactory = TestLegacyEntityIdFactory(notificationsByteRepresentation)
        val legacyId = 1L
        val messageUuid = buildExpectedUuid(
            upper16BytesAsString = "0000",
            lower48BytesAsString = "000000000001",
            byteRepresentation = messageByteRepresentation,
        )
        val notificationsUuid = buildExpectedUuid(
            upper16BytesAsString = "0000",
            lower48BytesAsString = "000000000001",
            byteRepresentation = notificationsByteRepresentation,
        )

        // Act
        val messageId = messageFactory.of(legacyId)
        val notificationId = notificationFactory.of(legacyId)

        // Assert
        assertThat(messageId.toString()).isEqualTo(messageUuid)
        assertThat(notificationId.toString()).isEqualTo(notificationsUuid)
    }

    @Test
    fun `toLegacyId should preserve the full positive Long range`() {
        // Arrange
        val testSubject = TestLegacyEntityIdFactory()
        val legacyId = Long.MAX_VALUE
        val id = testSubject.of(legacyId)

        // Act
        val result = testSubject.toLegacyId(id)

        // Assert
        assertThat(result).isEqualTo(legacyId)
    }

    @Test
    fun `toLegacyId should return the original id when round tripping legacy ids of varying sizes`() {
        // Arrange
        val testSubject = TestLegacyEntityIdFactory()
        val legacyIds = listOf(
            0L,
            1L,
            42L,
            255L, // 2^8 - 1
            65_535L, // 2^16 - 1
            65_536L, // 2^16
            16_777_215L, // 2^24 - 1
            4_294_967_295L, // 2^32 - 1
            4_294_967_296L, // 2^32
            281_474_976_710_655L, // 2^48 - 1,
            281_474_976_710_656L, // 2^48
            Long.MAX_VALUE,
        )

        legacyIds.forEach { legacyId ->
            // Act
            val result = testSubject.toLegacyId(testSubject.of(legacyId))

            // Assert
            assertThat(result).isEqualTo(legacyId)
        }
    }

    @Test
    fun `isLegacy should return true when given an id created by of`() {
        // Arrange
        val testSubject = TestLegacyEntityIdFactory(byteRepresentation = ByteRepresentation.MESSAGE)
        val id = testSubject.of(legacyId = 1L)

        // Act
        val result = testSubject.isLegacy(id)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isLegacy should return true when given an id created by a factory with a different entity type`() {
        // Arrange
        val messageFactory = TestLegacyEntityIdFactory(byteRepresentation = ByteRepresentation.MESSAGE)
        val folderFactory = TestLegacyEntityIdFactory(byteRepresentation = ByteRepresentation.FOLDERS)
        val folderId = folderFactory.of(legacyId = 1L)

        // Act
        val result = messageFactory.isLegacy(folderId)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isLegacy should return false when given a random UUID not following the legacy layout`() {
        // Arrange
        val byteRepresentation = ByteRepresentation.MESSAGE
        val testSubject = TestLegacyEntityIdFactory(byteRepresentation)
        val id = TestEntityId(Uuid.random())

        // Act
        val result = testSubject.isLegacy(id)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `of should reject negative legacy ids`() {
        // Arrange
        val testSubject = TestLegacyEntityIdFactory()
        // Act & Assert
        assertFailure { testSubject.of(legacyId = -1L) }.isInstanceOf<IllegalArgumentException>()
    }

    private fun buildExpectedUuid(
        upper16BytesAsString: String,
        lower48BytesAsString: String,
        byteRepresentation: ByteRepresentation,
    ): String = buildString {
        append(LegacyEntityIdFactory.LegacyRange)
        append("-")
        append(upper16BytesAsString)
        append("-")
        append(byteRepresentation.toHexString())
        append("-")
        append(LegacyEntityIdFactory.FixedPayload)
        append("-")
        append(lower48BytesAsString)
    }

    private class TestEntityId(value: Uuid) : BaseUuidIdentifier(value)

    private class TestLegacyEntityIdFactory(
        byteRepresentation: ByteRepresentation = ByteRepresentation.MESSAGE,
    ) : LegacyEntityIdFactory<TestEntityId>(byteRepresentation, ::TestEntityId)
}
