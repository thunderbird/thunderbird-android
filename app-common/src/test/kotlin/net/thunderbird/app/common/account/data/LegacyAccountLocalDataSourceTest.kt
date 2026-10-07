package net.thunderbird.app.common.account.data

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.legacy.AccountStorageHandler
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

class LegacyAccountLocalDataSourceTest {

    private val fakeStorageProvider = FakeStorageProvider()
    private val fakeAccountStorageHandler = FakeAccountStorageHandler()
    private val fakeAccountDefaultsProvider = FakeAccountDefaultsProvider()

    @Test
    fun `loadAll should return accounts in exact display order specified by accountUuids`() {
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()
        val accountId3 = AccountIdFactory.create()

        val account1 = createFakeAccount(accountId1, "Account 1")
        val account2 = createFakeAccount(accountId2, "Account 2")
        val account3 = createFakeAccount(accountId3, "Account 3")

        fakeAccountStorageHandler.setAccount(account1)
        fakeAccountStorageHandler.setAccount(account2)
        fakeAccountStorageHandler.setAccount(account3)

        // Storage with accounts reordered as 2, 1, 3
        val fakeStorage = FakeStorage(mapOf("accountUuids" to "$accountId2,$accountId1,$accountId3"))
        val testSubject = createTestSubject(fakeStorage)

        val result = testSubject.loadAll()

        assertThat(result).containsExactly(account2, account1, account3)
    }

    @Test
    fun `loadAll should return empty list when accountUuids is missing`() {
        val testSubject = createTestSubject(FakeStorage(emptyMap()))

        val result = testSubject.loadAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getById should return account when ID exists in accountUuids`() {
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()
        val account1 = createFakeAccount(accountId1, "Account 1")

        fakeAccountStorageHandler.setAccount(account1)
        val fakeStorage = FakeStorage(mapOf("accountUuids" to "$accountId1,$accountId2"))
        val testSubject = createTestSubject(fakeStorage)

        val result = testSubject.getById(accountId1)

        assertThat(result).isEqualTo(account1)
    }

    @Test
    fun `getById should return null when ID is not present in accountUuids`() {
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()

        val fakeStorage = FakeStorage(mapOf("accountUuids" to accountId1.toString()))
        val testSubject = createTestSubject(fakeStorage)

        val result = testSubject.getById(accountId2)

        assertThat(result).isNull()
    }

    @Test
    fun `save should delegate to accountStorageHandler and commit storageEditor`() {
        val account = createFakeAccount(AccountIdFactory.create(), "Account")
        val testSubject = createTestSubject(FakeStorage(emptyMap()))

        testSubject.save(account)

        assertThat(fakeAccountStorageHandler.savedAccounts).contains(account)
        assertThat(fakeStorageProvider.committedValues).isEqualTo(mapOf("accountId" to account.id.toString()))
    }

    @Test
    fun `save should fail when storage commit fails`() {
        val account = createFakeAccount(AccountIdFactory.create(), "Account")
        fakeStorageProvider.commitSucceeds = false
        val testSubject = createTestSubject(FakeStorage(emptyMap()))

        assertFailure { testSubject.save(account) }
            .isInstanceOf<IllegalStateException>()
            .hasMessage("Failed to save account settings")
    }

    @Test
    fun `delete should delegate to accountStorageHandler and commit storageEditor`() {
        val accountId = AccountIdFactory.create()
        val testSubject = createTestSubject(FakeStorage(emptyMap()))

        testSubject.delete(accountId)

        assertThat(fakeAccountStorageHandler.deletedAccountIds).contains(accountId)
        assertThat(fakeStorageProvider.committedRemovals).contains(accountId.toString())
    }

    private fun createTestSubject(storage: Storage): LegacyAccountLocalDataSource {
        fakeStorageProvider.storageInstance = storage
        return LegacyAccountLocalDataSource(
            storageProvider = fakeStorageProvider,
            accountStorageHandler = fakeAccountStorageHandler,
            accountDefaultsProvider = fakeAccountDefaultsProvider,
        )
    }

    private companion object Companion {
        fun createFakeAccount(
            id: AccountId,
            name: String,
        ): LegacyAccount {
            return LegacyAccount(
                id = id,
                name = name,
                email = "user@example.com",
                profile = ProfileDto(
                    id = id,
                    name = name,
                    color = -1,
                    avatar = AvatarDto(
                        id = id,
                        avatarType = AvatarTypeDto.MONOGRAM,
                        avatarMonogram = "NA",
                        avatarImageUri = null,
                        avatarIconName = null,
                    ),
                ),
                incomingServerSettings = ServerSettings(
                    type = "imap",
                    host = "host",
                    port = 993,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "user",
                    password = "pass",
                    clientCertificateAlias = null,
                ),
                outgoingServerSettings = ServerSettings(
                    type = "smtp",
                    host = "host",
                    port = 465,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "user",
                    password = "pass",
                    clientCertificateAlias = null,
                ),
                identities = emptyList(),
            )
        }
    }

    private class FakeStorage(private val values: Map<String, String> = emptyMap()) : Storage {
        override fun isEmpty(): Boolean = values.isEmpty()
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun getAll(): Map<String, String> = values
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key]?.toBoolean() ?: defValue
        override fun getInt(key: String, defValue: Int): Int = values[key]?.toIntOrNull() ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key]?.toLongOrNull() ?: defValue
        override fun getString(key: String): String = values[key] ?: error("No value for $key")
        override fun getStringOrDefault(key: String, defValue: String): String = values[key] ?: defValue
        override fun getStringOrNull(key: String): String? = values[key]
    }

    private class FakeStorageEditor(
        private val storageProvider: FakeStorageProvider,
    ) : StorageEditor {
        val values = mutableMapOf<String, String?>()
        val removedKeys = mutableListOf<String>()

        override fun putBoolean(key: String, value: Boolean): StorageEditor {
            values[key] = value.toString()
            return this
        }

        override fun putInt(key: String, value: Int): StorageEditor {
            values[key] = value.toString()
            return this
        }

        override fun putLong(key: String, value: Long): StorageEditor {
            values[key] = value.toString()
            return this
        }

        override fun putString(key: String, value: String?): StorageEditor {
            values[key] = value
            return this
        }

        override fun remove(key: String): StorageEditor {
            values.remove(key)
            removedKeys.add(key)
            return this
        }

        override fun commit(): Boolean {
            if (!storageProvider.commitSucceeds) return false
            storageProvider.committedValues.putAll(values)
            storageProvider.committedRemovals.addAll(removedKeys)
            return true
        }
    }

    private class FakeStorageProvider : StorageProvider {
        lateinit var storageInstance: Storage
        var commitSucceeds = true
        val committedValues = mutableMapOf<String, String?>()
        val committedRemovals = mutableListOf<String>()

        override val storage: Storage get() = storageInstance
        override fun loadLatestStorage(): Storage = storageInstance
        override fun createStorageEditor(): StorageEditor = FakeStorageEditor(this)
    }

    private class FakeAccountStorageHandler : AccountStorageHandler {
        private val accounts = mutableMapOf<AccountId, LegacyAccount>()
        val savedAccounts = mutableListOf<LegacyAccount>()
        val deletedAccountIds = mutableListOf<AccountId>()

        fun setAccount(account: LegacyAccount) {
            accounts[account.id] = account
        }

        override fun load(accountId: AccountId, storage: Storage): LegacyAccount {
            return accounts[accountId] ?: error("Account not found: $accountId")
        }

        override fun save(data: LegacyAccount, storage: Storage, editor: StorageEditor) {
            editor.putString("accountId", data.id.toString())
            savedAccounts.add(data)
            accounts[data.id] = data
        }

        override fun delete(accountId: AccountId, storage: Storage, editor: StorageEditor) {
            editor.remove(accountId.toString())
            deletedAccountIds.add(accountId)
            accounts.remove(accountId)
        }
    }

    private class FakeAccountDefaultsProvider : AccountDefaultsProvider {
        override fun applyDefaults(account: LegacyAccount): LegacyAccount = account
        override fun applyOverwrites(account: LegacyAccount, storage: Storage): LegacyAccount = account
    }
}
