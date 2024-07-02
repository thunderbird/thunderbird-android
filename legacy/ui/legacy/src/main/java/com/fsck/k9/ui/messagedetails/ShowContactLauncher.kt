package com.fsck.k9.ui.messagedetails

import android.content.Context
import android.content.Intent
import android.net.Uri

internal class ShowContactLauncher {
    fun launch(context: Context, contactLookupUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = contactLookupUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }

        context.startActivity(intent)
    }
}
