package com.fsck.k9.contacts

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.k9mail.core.common.mail.EmailAddress
import com.fsck.k9.helper.Contacts
import timber.log.Timber

internal class ContactPhotoLoader(private val contentResolver: ContentResolver, private val contacts: Contacts) {
    fun loadContactPhoto(emailAddress: String): Bitmap? {
        val photoUri = contacts.getPhotoUri(EmailAddress(emailAddress)) ?: return null
        return try {
            contentResolver.openInputStream(photoUri).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Timber.e(e, "Couldn't load contact photo: $photoUri")
            null
        }
    }
}
