package net.thunderbird.app.common.feature.mail

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import net.thunderbird.feature.mail.account.api.BaseAccount
import org.junit.Test

class BaseAccountBackendStorageFactoryTest {

    @Test
    fun `delegates to legacy factory for LegacyAccountDto`() {
        // Arrange
        val dto = FakeData.legacyAccountDto
        val legacyFactory = FakeLegacyAccountDtoBackendStorageFactory()
        val mapper = FakeLegacyAccountDataMapper()
        val factory = BaseAccountBackendStorageFactory(legacyFactory, mapper)

        // Act
        factory.createBackendStorage(dto)

        // Assert
        assertThat(legacyFactory.lastAccount).isSameInstanceAs(dto)
    }

    @Test
    fun `maps LegacyAccount to dto and delegates`() {
        // Arrange
        val dto = FakeData.legacyAccountDto
        val domain = FakeData.legacyAccount
        val mapper = FakeLegacyAccountDataMapper().apply { toDtoResult = dto }
        val legacyFactory = FakeLegacyAccountDtoBackendStorageFactory()
        val factory = BaseAccountBackendStorageFactory(legacyFactory, mapper)

        // Act
        factory.createBackendStorage(domain)

        // Assert
        assertThat(mapper.lastMapped).isSameInstanceAs(domain)
        assertThat(legacyFactory.lastAccount).isSameInstanceAs(dto)
    }

    @Test
    fun `unsupported BaseAccount throws`() {
        // Arrange
        val legacyFactory = FakeLegacyAccountDtoBackendStorageFactory()
        val mapper = FakeLegacyAccountDataMapper()
        val factory = BaseAccountBackendStorageFactory(legacyFactory, mapper)
        val unsupported: BaseAccount = FakeData.unsupportedAccount

        // Act & Assert
        assertFailure { factory.createBackendStorage(unsupported) }
            .isInstanceOf(IllegalArgumentException::class)
    }
}
