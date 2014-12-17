package com.fsck.k9.helper;


import android.graphics.Color;
import android.test.AndroidTestCase;
import android.text.SpannableString;

import com.fsck.k9.mail.Address;

public class MessageHelperTest extends AndroidTestCase {
    private Contacts contacts;
    private Contacts mockContacts;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        contacts = new Contacts(getContext());
        mockContacts = new Contacts(getContext()) {
            @Override public String getNameForAddress(String address) {
                if ("test@testor.com".equals(address)) {
                    return "Tim Testor";
                } else {
                    return null;
                }
            }
        };
    }

    public void testToFriendlyShowsPersonalPartIfItExists() throws Exception {
        Address address = new Address("test@testor.com", "Tim Testor");
        assertEquals("Tim Testor", MessageHelper.toFriendly(address, contacts));
    }

    public void testToFriendlyShowsEmailPartIfNoPersonalPartExists() throws Exception {
        Address address = new Address("test@testor.com");
        assertEquals("test@testor.com", MessageHelper.toFriendly(address, contacts));
    }

    public void testToFriendlyArray() throws Exception {
        Address address1 = new Address("test@testor.com", "Tim Testor");
        Address address2 = new Address("foo@bar.com", "Foo Bar");
        Address[] addresses = new Address[] { address1, address2 };
        assertEquals("Tim Testor,Foo Bar", MessageHelper.toFriendly(addresses, contacts).toString());
    }

    public void testToFriendlyWithContactLookup() throws Exception {
        Address address = new Address("test@testor.com");
        assertEquals("Tim Testor", MessageHelper.toFriendly(address, mockContacts).toString());
    }

    public void testToFriendlyWithChangeContactColor() throws Exception {
        Address address = new Address("test@testor.com");
        CharSequence friendly = MessageHelper.toFriendly(address, mockContacts, true, true, Color.RED);
        assertTrue(friendly instanceof SpannableString);
        assertEquals("Tim Testor", friendly.toString());
    }

    public void testToFriendlyWithoutCorrespondentNames() throws Exception {
        Address address = new Address("test@testor.com", "Tim Testor");
        CharSequence friendly = MessageHelper.toFriendly(address, mockContacts, false, false, 0);
        assertEquals("test@testor.com", friendly.toString());
    }
}
