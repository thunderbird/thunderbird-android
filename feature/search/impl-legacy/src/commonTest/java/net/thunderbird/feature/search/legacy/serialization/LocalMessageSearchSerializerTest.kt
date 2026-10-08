package net.thunderbird.feature.search.legacy.serialization

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.text.Charsets
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.UnifiedFolderSelection
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
        assertThat(result.id).isNull()
        assertThat(result.type).isEqualTo(LocalMessageSearchType.Account)
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
        assertThat(result.accountIds).isEqualTo(search.accountIds)
    }

    @Test
    fun `should round-trip serialize and deserialize search with account id`() {
        // Arrange
        val search = LocalMessageSearch()
        search.addAccountId(AccountIdFactory.create())

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.accountIds).isEqualTo(search.accountIds)
    }

    @Test
    fun `should round-trip serialize and deserialize new messages search type`() {
        // Arrange
        val accountId = AccountIdFactory.create()
        val search = LocalMessageSearch().apply {
            id = accountId
            type = LocalMessageSearchType.NewMessages
        }

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.id).isEqualTo(accountId)
        assertThat(result.type).isEqualTo(LocalMessageSearchType.NewMessages)
    }

    @Test
    fun `should round-trip serialize and deserialize unified search type`() {
        // Arrange
        val search = LocalMessageSearch().apply {
            type = LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(FolderType.INBOX))
        }

        // Act
        val bytes = LocalMessageSearchSerializer.serialize(search)
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.type).isEqualTo(
            LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(FolderType.INBOX)),
        )
    }

    @Test
    fun `should deserialize search without type as account type`() {
        // Arrange
        val bytes = "{}".toByteArray(Charsets.UTF_8)

        // Act
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.type).isEqualTo(LocalMessageSearchType.Account)
    }

    @Test
    fun `should deserialize search with unknown type as account type`() {
        // Arrange
        val bytes = """{"type":{"type":"default"}}""".toByteArray(Charsets.UTF_8)

        // Act
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.type).isEqualTo(LocalMessageSearchType.Account)
    }

    @Test
    fun `should deserialize search with unknown unified folder selection as account type`() {
        // Arrange
        val bytes = """{"type":{"type":"unified","folder":{"type":"custom","id":"1"}}}"""
            .toByteArray(Charsets.UTF_8)

        // Act
        val result = LocalMessageSearchSerializer.deserialize(bytes)

        // Assert
        assertThat(result.type).isEqualTo(LocalMessageSearchType.Account)
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
