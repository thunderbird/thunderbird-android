package net.thunderbird.feature.search.legacy.serialization

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlin.text.Charsets
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import org.junit.Test

class LocalMessageSearchSerializerTest {

    @Test
    fun `should serialize empty search`() {
        // Arrange
        val search = LocalMessageSearch()

        // Act
        val result = LocalMessageSearchSerializer.serialize(search)

        // Assert
        assertThat(result).isNotNull()
    }

    @Test
    fun `should deserialize empty search`() {
        // Arrange
        val search = LocalMessageSearch()
        val bytes = LocalMessageSearchSerializer.serialize(search)

        // Act
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result.id).isEqualTo("")
        assertThat(result.isManualSearch).isEqualTo(false)
    }

    @Test
    fun `should round-trip serialize and deserialize empty search`() {
        // Arrange
        val search = LocalMessageSearch()

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.id).isEqualTo(search.id)
        assertThat(result.isManualSearch).isEqualTo(search.isManualSearch)
        assertThat(result.accountUuids).isEqualTo(search.accountUuids)
    }

    @Test
    fun `should round-trip serialize and deserialize search with account uuid`() {
        // Arrange
        val search = LocalMessageSearch()
        search.addAccountUuid("test-account-uuid")

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.accountUuids).isEqualTo(search.accountUuids)
    }

    @Test
    fun `should round-trip serialize and deserialize search with condition`() {
        // Arrange
        val search = LocalMessageSearch()
        search.and(MessageSearchField.SUBJECT, "test subject", SearchAttribute.CONTAINS)

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        val originalCondition = search.conditions.condition
        val resultCondition = result.conditions.condition

        assertThat(resultCondition).isNotNull()
        assertThat(originalCondition).isNotNull()
        assertThat(resultCondition!!.field).isEqualTo(originalCondition!!.field)
        assertThat(resultCondition.attribute).isEqualTo(originalCondition.attribute)
        assertThat(resultCondition.value).isEqualTo(originalCondition.value)
    }

    @Test
    fun `should round-trip serialize and deserialize search with multiple conditions`() {
        // Arrange
        val search = LocalMessageSearch()
        search.and(MessageSearchField.SUBJECT, "test subject", SearchAttribute.CONTAINS)
        search.and(MessageSearchField.SENDER, "test sender", SearchAttribute.CONTAINS)

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        // Since the conditions are in a tree structure, we'll just verify the leaf set size
        assertThat(result.leafSet.size).isEqualTo(search.leafSet.size)
    }

    @Test
    fun `should handle special characters correctly`() {
        // Arrange
        val search = LocalMessageSearch()
        val specialChars = "Special characters: äöüß@€$%&*()[]{}|<>?/\\=+"
        search.and(MessageSearchField.SUBJECT, specialChars, SearchAttribute.CONTAINS)

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        val originalCondition = search.conditions.condition
        val resultCondition = result.conditions.condition

        assertThat(resultCondition).isNotNull()
        assertThat(originalCondition).isNotNull()
        assertThat(resultCondition!!.value).isEqualTo(originalCondition!!.value)
        assertThat(resultCondition.value).isEqualTo(specialChars)
    }

    @Test
    fun `should use UTF-8 encoding for serialization`() {
        // Arrange
        val search = LocalMessageSearch()
        val utf8String = "UTF-8 characters: 你好, こんにちは, 안녕하세요"
        search.and(MessageSearchField.SUBJECT, utf8String, SearchAttribute.CONTAINS)

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)

        // Assert
        // Convert bytes back to string using UTF-8 and verify it contains the original string
        val jsonString = String(bytes, Charsets.UTF_8)
        assertThat(jsonString.contains(utf8String)).isEqualTo(true)
    }
}
