package com.fsck.k9.mailstore

import android.net.Uri
import com.fsck.k9.Account
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.provider.EmailProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.inject

class K9BackendStorageTest : K9RobolectricTest() {
    val preferences: Preferences by inject()
    val localStoreProvider: LocalStoreProvider by inject()

    val account: Account = createAccount()
    val database: LockableDatabase = localStoreProvider.getInstance(account).database
    val backendStorage = createBackendStorage()

    @Before
    fun setUp() {
        // Set EmailProvider.CONTENT_URI so LocalStore.notifyChange() won't crash
        EmailProvider.CONTENT_URI = Uri.parse("content://dummy")
    }

    @After
    fun tearDown() {
        preferences.deleteAccount(account)
    }

    @Test
    fun writeAndReadExtraString() {
        backendStorage.setExtraString("testString", "someValue")
        val value = backendStorage.getExtraString("testString")

        assertEquals("someValue", value)
    }

    @Test
    fun updateExtraString() {
        backendStorage.setExtraString("testString", "oldValue")
        backendStorage.setExtraString("testString", "newValue")

        val value = backendStorage.getExtraString("testString")
        assertEquals("newValue", value)
    }

    @Test
    fun writeAndReadExtraInteger() {
        backendStorage.setExtraNumber("testNumber", 42)
        val value = backendStorage.getExtraNumber("testNumber")

        assertEquals(42L, value)
    }

    @Test
    fun updateExtraInteger() {
        backendStorage.setExtraNumber("testNumber", 42)
        backendStorage.setExtraNumber("testNumber", 23)

        val value = backendStorage.getExtraNumber("testNumber")
        assertEquals(23L, value)
    }

    fun createAccount(): Account {
        // FIXME: This is a hack to get Preferences into a state where it's safe to call newAccount()
        preferences.clearAccounts()

        return preferences.newAccount()
    }

    private fun createBackendStorage(): BackendStorage {
        val localStore: LocalStore = localStoreProvider.getInstance(account)
        return K9BackendStorage(preferences, account, localStore, emptyList())
    }
}
