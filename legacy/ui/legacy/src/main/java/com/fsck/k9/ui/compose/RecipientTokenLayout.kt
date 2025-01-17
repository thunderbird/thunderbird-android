package com.fsck.k9.ui.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.fsck.k9.ui.R

/**
 * Custom layout for recipient tokens.
 *
 * Note: This layout is tightly coupled to recipient_token_item.xml
 */
class RecipientTokenLayout(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private lateinit var background: View
    private lateinit var contactPicture: View
    private lateinit var recipientName: View
    private lateinit var cryptoStatus: View

    override fun onFinishInflate() {
        super.onFinishInflate()
        background = findViewById(R.id.background)
        contactPicture = findViewById(R.id.contact_photo)
        recipientName = findViewById(android.R.id.text1)
        cryptoStatus = findViewById(R.id.crypto_status_container)
    }

    // Return an appropriate baseline so the view is properly aligned with user-entered text in RecipientSelectView
    override fun getBaseline(): Int {
        return recipientName.top + recipientName.baseline
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        recipientName.measure(widthMeasureSpec, heightMeasureSpec)
        cryptoStatus.measure(widthMeasureSpec, heightMeasureSpec)

        val height = recipientName.measuredHeight.coerceAtLeast(minimumHeight)

        val contactPictureWidth = height
        val fixedWidthComponent = contactPictureWidth + cryptoStatus.measuredWidth
        val desiredWidth = fixedWidthComponent + recipientName.measuredWidth

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(desiredWidth, height)
        } else {
            // Re-measure recipient name view with final width constraint
            val width = desiredWidth.coerceAtMost(MeasureSpec.getSize(widthMeasureSpec))
            val recipientNameWidth = width - fixedWidthComponent
            val recipientNameWidthMeasureSpec = MeasureSpec.makeMeasureSpec(recipientNameWidth, MeasureSpec.AT_MOST)
            recipientName.measure(recipientNameWidthMeasureSpec, heightMeasureSpec)

            setMeasuredDimension(width, height)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val contactPictureSize = height
        background.layout(contactPictureSize / 2, 0, width, height)
        contactPicture.layout(0, 0, contactPictureSize, contactPictureSize)

        val recipientNameHeight = recipientName.measuredHeight
        val recipientNameTop = (height - recipientNameHeight) / 2
        recipientName.layout(
            contactPictureSize,
            recipientNameTop,
            contactPictureSize + recipientName.measuredWidth,
            recipientNameTop + recipientNameHeight,
        )

        cryptoStatus.layout(width - cryptoStatus.measuredWidth, 0, width, cryptoStatus.measuredHeight)
    }
}
