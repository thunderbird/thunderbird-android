package com.fsck.k9.activity

import com.fsck.k9.Account
import com.fsck.k9.view.ColorChip

internal class ColorChipProvider {
    val cache = mutableMapOf<String, ColorChipHolder>()

    fun getColorChip(account: Account, flagged: Boolean): ColorChip {
        val chipHolder = getChipHolder(account)

        return with(chipHolder) {
            if (flagged) flaggedColorChip else unreadColorChip
        }
    }

    private fun getChipHolder(account: Account): ColorChipHolder {
        val colorChipHolder = cache[account.uuid]
        if (colorChipHolder?.chipColor == account.chipColor) {
            return colorChipHolder
        }

        val newColorChipHolder = createColorChipHolder(account)
        cache[account.uuid] = newColorChipHolder
        return newColorChipHolder
    }

    private fun createColorChipHolder(account: Account): ColorChipHolder {
        val chipColor = account.chipColor
        return ColorChipHolder(
                chipColor = chipColor,
                unreadColorChip = ColorChip(chipColor, ColorChip.CIRCULAR),
                flaggedColorChip = ColorChip(chipColor, ColorChip.STAR)
        )
    }


    data class ColorChipHolder(
            val chipColor: Int,
            val unreadColorChip: ColorChip,
            val flaggedColorChip: ColorChip
    )
}
