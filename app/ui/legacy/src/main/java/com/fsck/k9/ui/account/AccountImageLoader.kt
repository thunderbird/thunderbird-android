package com.fsck.k9.ui.account

import android.app.Activity
import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Load the account image into an [ImageView].
 */
class AccountImageLoader(private val accountFallbackImageProvider: AccountFallbackImageProvider) {
    fun setAccountImage(imageView: ImageView, email: String, color: Int) {
        imageView.context.ifNotDestroyed { context ->
            Glide.with(context)
                .load(AccountImage(email, color))
                .placeholder(accountFallbackImageProvider.getDrawable(color))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontAnimate()
                .into(imageView)
        }
    }

    fun cancel(imageView: ImageView) {
        imageView.context.ifNotDestroyed { context ->
            Glide.with(context).clear(imageView)
        }
    }

    private inline fun Context.ifNotDestroyed(block: (Context) -> Unit) {
        if ((this as? Activity)?.isDestroyed == true) {
            // Do nothing because Glide would throw an exception
        } else {
            block(this)
        }
    }
}
