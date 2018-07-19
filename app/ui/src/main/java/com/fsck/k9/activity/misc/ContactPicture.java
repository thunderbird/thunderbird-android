package com.fsck.k9.activity.misc;


import android.content.Context;

import com.fsck.k9.DI;
import com.fsck.k9.contacts.ContactLetterBitmapCreator;
import com.fsck.k9.contacts.ContactPictureLoader;


public class ContactPicture {

    public static ContactPictureLoader getContactPictureLoader(Context context) {
        ContactLetterBitmapCreator contactLetterBitmapCreator = DI.get(ContactLetterBitmapCreator.class);
        return new ContactPictureLoader(context.getApplicationContext(), contactLetterBitmapCreator);
    }
}
