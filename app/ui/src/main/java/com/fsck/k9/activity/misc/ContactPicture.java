package com.fsck.k9.activity.misc;


import com.fsck.k9.DI;
import com.fsck.k9.contacts.ContactPictureLoader;


public class ContactPicture {

    public static ContactPictureLoader getContactPictureLoader() {
        return DI.get(ContactPictureLoader.class);
    }
}
