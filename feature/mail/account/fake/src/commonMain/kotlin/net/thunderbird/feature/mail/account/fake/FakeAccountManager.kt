package net.thunderbird.feature.mail.account.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.account.api.AccountManager

/**
 * In-memory [AccountManager] for tests that only holds accounts.
 *
 * It doesn't imitate the real account manager's behaviour (locking, validation, failures). Tests that need such
 * behaviour can override the open functions.
 */
open class FakeAccountManager<TAccount : Account>(
    initialAccounts: List<TAccount> = emptyList(),
) : AccountManager<TAccount> {
    private val accounts = MutableStateFlow(initialAccounts)

    /** Every account saved through [create], [createSync], [update] or [updateSync], in order. */
    val savedAccounts = mutableListOf<TAccount>()

    override fun findAll(): List<TAccount> = accounts.value

    override fun observeAll(): Flow<List<TAccount>> = accounts

    override fun findById(accountId: AccountId): TAccount? = accounts.value.firstOrNull { it.id == accountId }

    override fun observeById(accountId: AccountId): Flow<TAccount?> =
        accounts.map { list -> list.firstOrNull { it.id == accountId } }

    override fun moveAccount(accountId: AccountId, newPosition: Int) {
        val account = findById(accountId) ?: return
        val otherAccounts = accounts.value - account
        accounts.value = otherAccounts.toMutableList().apply { add(newPosition.coerceIn(0, size), account) }
    }

    override suspend fun create(account: TAccount) = createSync(account)

    override fun createSync(account: TAccount) {
        replace(account)
        savedAccounts += account
    }

    override suspend fun update(accountId: AccountId, transform: (TAccount) -> TAccount): TAccount? =
        updateSync(accountId, transform)

    override fun updateSync(accountId: AccountId, transform: (TAccount) -> TAccount): TAccount? {
        val account = findById(accountId)?.let(transform) ?: return null
        replace(account)
        savedAccounts += account
        return account
    }

    /** Adds or replaces [account] without recording a save, e.g. to simulate a change made by another component. */
    fun replace(account: TAccount) {
        val list = accounts.value
        val index = list.indexOfFirst { it.id == account.id }
        accounts.value = if (index == -1) list + account else list.toMutableList().apply { set(index, account) }
    }

    /** Removes the account without recording a save. */
    fun remove(accountId: AccountId) {
        accounts.value = accounts.value.filterNot { it.id == accountId }
    }
}
