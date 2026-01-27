package net.thunderbird.app.common.account

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.first
import net.thunderbird.feature.account.profile.AccountProfileRepository

internal class AccountColorPicker(
    private val repository: AccountProfileRepository,
    private val accountColors: ImmutableList<Int>,
) {
    suspend fun pickColor(): Int {
        val profiles = repository.getAll().first()
        val usedCounts = profiles.groupingBy { it.color }.eachCount()

        val minCount = accountColors.minOf { usedCounts[it] ?: 0 }
        val candidates = accountColors.filter {
            (usedCounts[it] ?: 0) == minCount
        }

        return if (candidates.isNotEmpty()) {
            candidates.shuffled().first()
        } else {
            accountColors.shuffled().first()
        }
    }
}
