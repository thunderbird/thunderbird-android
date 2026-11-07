package net.thunderbird.app.common.account.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.preferences.K9StoragePersister
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.storage.DefaultStorageProvider
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyAvatarDtoStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyProfileDtoStorageHandler
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test
import org.robolectric.RuntimeEnvironment

/**
 * Runs [LegacyAccountLocalDataSource] against the real preferences database to verify that reading from the cached
 * storage snapshot returns the latest values and that nothing is lost on disk.
 */
class LegacyAccountLocalDataSourceStorageTest : RobolectricTest() {

    private val logger = TestLogger()

    private val storageProvider = createStorageProvider()
    private val testSubject = createTestSubject(storageProvider)

    @Test
    fun `getById should return the latest saved values`() {
        val account = createAccount()

        testSubject.save(account.copy(lastSyncTime = 1L))
        testSubject.save(account.copy(lastSyncTime = 2L))

        assertThat(testSubject.getById(account.id)).isNotNull()
            .prop(LegacyAccount::lastSyncTime).isEqualTo(2L)
    }

    @Test
    fun `getById should return account values written by another editor`() {
        val account = createAccount()
        testSubject.save(account)

        storageProvider.createStorageEditor()
            .putLong("${account.id}.lastSyncTime", 42L)
            .commit()

        assertThat(testSubject.getById(account.id)).isNotNull()
            .prop(LegacyAccount::lastSyncTime).isEqualTo(42L)
    }

    @Test
    fun `saved values should be loaded after a restart`() {
        val account = createAccount()
        testSubject.save(account.copy(lastSyncTime = 1L))
        testSubject.save(account.copy(lastSyncTime = 2L))

        val restartedTestSubject = createTestSubject(createStorageProvider())

        assertThat(restartedTestSubject.loadAll().map { it.id }).containsExactly(account.id)
        assertThat(restartedTestSubject.getById(account.id)).isNotNull()
            .prop(LegacyAccount::lastSyncTime).isEqualTo(2L)
    }

    @Test
    fun `saving an account should keep values written by another editor`() {
        val account = createAccount()
        testSubject.save(account)

        storageProvider.createStorageEditor()
            .putString("otherKey", "otherValue")
            .commit()
        testSubject.save(account.copy(lastSyncTime = 1L))

        val restartedStorageProvider = createStorageProvider()
        assertThat(restartedStorageProvider.storage.getStringOrNull("otherKey")).isEqualTo("otherValue")
        assertThat(createTestSubject(restartedStorageProvider).getById(account.id)).isNotNull()
            .prop(LegacyAccount::lastSyncTime).isEqualTo(1L)
    }

    @Test
    fun `deleted account should be gone after a restart`() {
        val account = createAccount()
        val otherAccount = createAccount()
        testSubject.save(account)
        testSubject.save(otherAccount)

        testSubject.delete(account.id)

        val restartedTestSubject = createTestSubject(createStorageProvider())
        assertThat(restartedTestSubject.getById(account.id)).isNull()
        assertThat(restartedTestSubject.loadAll().map { it.id }).containsExactly(otherAccount.id)
    }

    private fun createStorageProvider(): StorageProvider {
        return DefaultStorageProvider(K9StoragePersister(RuntimeEnvironment.getApplication(), logger))
    }

    private fun createTestSubject(storageProvider: StorageProvider): LegacyAccountLocalDataSource {
        return LegacyAccountLocalDataSource(
            storageProvider = storageProvider,
            accountStorageHandler = LegacyAccountStorageHandler(
                serverSettingsDtoSerializer = ServerSettingsDtoSerializer(),
                profileDtoStorageHandler = LegacyProfileDtoStorageHandler(LegacyAvatarDtoStorageHandler()),
                logger = logger,
            ),
            accountDefaultsProvider = PassThroughAccountDefaultsProvider(),
        )
    }

    private class PassThroughAccountDefaultsProvider : AccountDefaultsProvider {
        override fun applyDefaults(account: LegacyAccount): LegacyAccount = account
        override fun applyOverwrites(account: LegacyAccount, storage: Storage): LegacyAccount = account
    }

    private companion object {
        fun createAccount(id: AccountId = AccountIdFactory.create()): LegacyAccount {
            return LegacyAccount(
                id = id,
                name = "Account",
                email = "user@example.com",
                profile = ProfileDto(
                    id = id,
                    name = "Account",
                    color = -1,
                    avatar = AvatarDto(
                        accountId = id,
                        avatarType = AvatarTypeDto.MONOGRAM,
                        avatarMonogram = "AC",
                        avatarImageUri = null,
                        avatarIconName = null,
                    ),
                ),
                incomingServerSettings = createServerSettings("imap", 993),
                outgoingServerSettings = createServerSettings("smtp", 465),
                identities = emptyList(),
                accountNumber = 1,
            )
        }

        fun createServerSettings(type: String, port: Int) = ServerSettings(
            type = type,
            host = "$type.example.com",
            port = port,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
