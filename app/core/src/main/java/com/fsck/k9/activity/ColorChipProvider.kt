package com.fsck.k9.activity

import com.fsck.k9.Account
import com.fsck.k9.view.ColorChip

internal class ColorChipProvider {
    val cache = mutableMapOf<String, ColorChipHolder>()

    fun getColorChip(account: Account, messageRead: Boolean, messageFlagged: Boolean): ColorChip {
        val chipHolder = getChipHolder(account)

        return when {
            messageRead && messageFlagged -> chipHolder.flaggedReadColorChip
            messageRead && !messageFlagged -> chipHolder.readColorChip
            !messageRead && messageFlagged -> chipHolder.flaggedUnreadColorChip
            !messageRead && !messageFlagged -> chipHolder.unreadColorChip
            else -> throw AssertionError()
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
                readColorChip = ColorChip(chipColor, true, ColorChip.CIRCULAR),
                unreadColorChip = ColorChip(chipColor, false, ColorChip.CIRCULAR),
                flaggedReadColorChip = ColorChip(chipColor, true, ColorChip.STAR),
                flaggedUnreadColorChip = ColorChip(chipColor, false, ColorChip.STAR)
        )
    }


    data class ColorChipHolder(
            val chipColor: Int,
            val readColorChip: ColorChip,
            val unreadColorChip: ColorChip,
            val flaggedReadColorChip: ColorChip,
            val flaggedUnreadColorChip: ColorChip
    )
}
