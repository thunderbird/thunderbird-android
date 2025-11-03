package net.thunderbird.feature.account.core.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.collections.emptyList
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.profile.AccountProfile

class DefaultAccountProfileRepositoryTest {

    @Test
    fun `getAll should return distinct values`() = runTest {
        // Arrange
        val profiles: MutableStateFlow<List<AccountProfile>> = MutableStateFlow(emptyList())
        val list1 = listOf(PROFILE_1)
        val list2 = listOf(PROFILE_1, PROFILE_2)
        val testSubject = DefaultAccountProfileRepository(
            localDataSource = FakeAccountProfileDataSource(
                profiles = profiles,
            ),
        )

        // Act / Assert
        testSubject.getAll().test {
            val emptyResult = awaitItem()
            assertThat(emptyResult).isEqualTo(emptyList())

            profiles.value = list1
            val firstResult = awaitItem()
            assertThat(firstResult).isEqualTo(list1)

            profiles.value = list1
            expectNoEvents()

            profiles.value = list2
            val secondResult = awaitItem()
            assertThat(secondResult).isEqualTo(list2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById should return distinct values`() = runTest {
        // Arrange
        val profiles: MutableStateFlow<List<AccountProfile>> = MutableStateFlow(emptyList())
        val list1 = listOf(PROFILE_1, PROFILE_2)
        val testSubject = DefaultAccountProfileRepository(
            localDataSource = FakeAccountProfileDataSource(
                profiles = profiles,
            ),
        )

        // Act / Assert
        testSubject.getById(PROFILE_ID_2).test {
            val nullResult = awaitItem()
            assertThat(nullResult).isEqualTo(null)

            profiles.value = list1
            val firstResult = awaitItem()
            assertThat(firstResult).isEqualTo(PROFILE_2)

            profiles.value = list1
            expectNoEvents()

            profiles.value = emptyList()
            val secondResult = awaitItem()
            assertThat(secondResult).isEqualTo(null)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `update should call local data source update`() = runTest {
        // Arrange
        var updatedProfile: AccountProfile? = null
        val testSubject = DefaultAccountProfileRepository(
            localDataSource = object : FakeAccountProfileDataSource() {
                override suspend fun update(accountProfile: AccountProfile) {
                    updatedProfile = accountProfile
                }
            },
        )
        val profile = PROFILE_1.copy(name = "Updated Name")

        // Act
        testSubject.update(profile)

        // Assert
        assertThat(updatedProfile).isEqualTo(profile)
    }

    private companion object {
        val PROFILE_ID_1 = AccountIdFactory.create()
        val PROFILE_ID_2 = AccountIdFactory.create()

        val PROFILE_1 = AccountProfile(
            id = PROFILE_ID_1,
            name = "Profile 1",
            color = 0xFF0000,
            avatar = AccountAvatar.Icon(name = "icon-1"),
        )
        val PROFILE_2 = AccountProfile(
            id = PROFILE_ID_2,
            name = "Profile 2",
            color = 0x00FF00,
            avatar = AccountAvatar.Monogram(value = "AB"),
        )
    }
}
