package com.fsck.k9.ui.messagedetails

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import com.fsck.k9.ui.R
import com.fsck.k9.ui.resolveColorAttribute
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class CryptoStatusItem(val cryptoDetails: CryptoDetails) : AbstractItem<CryptoStatusItem.ViewHolder>() {
    override val type = R.id.message_details_crypto_status
    override val layoutRes = R.layout.message_details_crypto_status_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<CryptoStatusItem>(view) {
        private val titleTextView = view.findViewById<MaterialTextView>(R.id.crypto_status_title)
        private val descriptionTextView = view.findViewById<MaterialTextView>(R.id.crypto_status_description)
        private val imageView = view.findViewById<ImageView>(R.id.crypto_status_icon)
        private val originalBackground = view.background

        override fun bindView(item: CryptoStatusItem, payloads: List<Any>) {
            val context = itemView.context
            val cryptoDetails = item.cryptoDetails
            val cryptoStatus = cryptoDetails.cryptoStatus

            imageView.setImageResource(cryptoStatus.statusIconRes)
            val tintColor = context.theme.resolveColorAttribute(cryptoStatus.colorAttr)
            imageView.imageTintList = ColorStateList.valueOf(tintColor)

            cryptoStatus.titleTextRes?.let { stringResId ->
                titleTextView.text = context.getString(stringResId)
            }
            cryptoStatus.descriptionTextRes?.let { stringResId ->
                descriptionTextView.text = context.getString(stringResId)
            }

            if (cryptoDetails.isClickable) {
                itemView.background = originalBackground
            } else {
                itemView.background = null
            }
        }

        override fun unbindView(item: CryptoStatusItem) {
            imageView.setImageDrawable(null)
            titleTextView.text = null
            descriptionTextView.text = null
        }
    }
}
