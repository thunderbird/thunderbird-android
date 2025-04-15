package net.thunderbird.core.ui.account

import com.bumptech.glide.load.Key
import java.security.MessageDigest

data class AccountImage(val email: String, val color: Int) : Key {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(toString().toByteArray(Key.CHARSET))
    }
}
