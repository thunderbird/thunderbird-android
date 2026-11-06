package net.thunderbird.feature.account.settings.impl.domain.usecase

import java.text.BreakIterator
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateMonogramError

internal class ValidateAvatarMonogram : UseCase.ValidateAvatarMonogram {

    override fun invoke(monogram: String): Outcome<Unit, ValidateMonogramError> = when {
        monogram.isBlank() -> Outcome.failure(ValidateMonogramError.EmptyMonogram)
        monogram.graphemeClusterCount() > MAX_MONOGRAM_LENGTH -> Outcome.failure(ValidateMonogramError.TooLongMonogram)
        else -> Outcome.success(Unit)
    }

    /**
     * Counts the grapheme clusters in this string, so that a single multi-code-point character such
     * as an emoji (including ZWJ sequences like 🐦‍🔥) counts as one, not as several UTF-16 units.
     */
    private fun String.graphemeClusterCount(): Int {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(this)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) {
            count++
        }
        return count
    }

    private companion object {
        const val MAX_MONOGRAM_LENGTH = 3
    }
}
