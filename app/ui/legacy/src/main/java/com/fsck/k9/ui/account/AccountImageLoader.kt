package com.fsck.k9.ui.account

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Load the account image into an [ImageView].
 */
class AccountImageLoader(private val accountFallbackImageProvider: AccountFallbackImageProvider) {
    fun setAccountImage(imageView: ImageView, email: String, color: Int) {
        Glide.with(imageView.context)
            .load(AccountImage(email, color))
            .placeholder(accountFallbackImageProvider.getDrawable(color))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .dontAnimate()
            .into(imageView)
    }

    fun cancel(imageView: ImageView) {
        Glide.with(imageView.context).clear(imageView)
    }
}
