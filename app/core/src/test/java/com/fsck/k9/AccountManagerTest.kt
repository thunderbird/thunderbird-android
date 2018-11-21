package com.fsck.k9


import com.fsck.k9.helper.IdentityHelperTest
import org.junit.Assert
import org.junit.Test
import org.koin.standalone.inject


class AccountManagerTest : K9RobolectricTest() {
    val preferences: Preferences by inject();
    val accountManager: AccountManager by inject();

    @Test
    fun saveAndLoad() {
        val account = accountManager.createAccountWithDefaults()
        val identity = Identity().apply {
            name = "Default"
            email = IdentityHelperTest.DEFAULT_ADDRESS
        }
        account.identities = listOf(identity)
        accountManager.save(account)

        val loadedAccount = accountManager.loadAccount(account.uuid)
        Assert.assertEquals(account, loadedAccount)

        preferences.deleteAccount(account)
    }

    @Test
    fun moveUp() {
        val identity = Identity().apply {
            name = "Default"
            email = IdentityHelperTest.DEFAULT_ADDRESS
        }
        val account1 = accountManager.createAccountWithDefaults()
        account1.identities = listOf(identity)
        val account2 = accountManager.createAccountWithDefaults()
        account2.identities = listOf(identity)

        accountManager.save(account1)
        accountManager.save(account2)

        accountManager.move(account2, true)
        Assert.assertEquals(preferences.accounts[0], account2)

        preferences.deleteAccount(account1)
        preferences.deleteAccount(account2)
    }
}