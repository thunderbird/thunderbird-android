package com.fsck.k9.helper;


import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;

import com.fsck.k9.mail.Address;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


@RunWith(RobolectricTestRunner.class)
public class MessageHelperTest {
    private Contacts contacts;
    private Contacts contactsWithFakeContact;
    private Contacts contactsWithFakeSpoofContact;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application;
        contacts = new Contacts(context);
        contactsWithFakeContact = new Contacts(context) {
            @Override public String getNameForAddress(String address) {
                if ("test@testor.com".equals(address)) {
                    return "Tim Testor";
                } else {
                    return null;
                }
            }
        };
        contactsWithFakeSpoofContact = new Contacts(context) {
            @Override public String getNameForAddress(String address) {
                if ("test@testor.com".equals(address)) {
                    return "Tim@Testor";
                } else {
                    return null;
                }
            }
        };
    }

    @Test
    public void testToFriendlyShowsPersonalPartIfItExists() throws Exception {
        Address address = new Address("test@testor.com", "Tim Testor");
        assertEquals("Tim Testor", MessageHelper.toFriendly(address, contacts));
    }

    @Test
    public void testToFriendlyShowsEmailPartIfNoPersonalPartExists() throws Exception {
        Address address = new Address("test@testor.com");
        assertEquals("test@testor.com", MessageHelper.toFriendly(address, contacts));
    }

    @Test
    public void testToFriendlyArray() throws Exception {
        Address address1 = new Address("test@testor.com", "Tim Testor");
        Address address2 = new Address("foo@bar.com", "Foo Bar");
        Address[] addresses = new Address[] { address1, address2 };
        assertEquals("Tim Testor,Foo Bar", MessageHelper.toFriendly(addresses, contacts).toString());
    }

    @Test
    public void testToFriendlyWithContactLookup() throws Exception {
        Address address = new Address("test@testor.com");
        assertEquals("Tim Testor", MessageHelper.toFriendly(address, contactsWithFakeContact).toString());
    }

    @Test
    public void testToFriendlyWithChangeContactColor() throws Exception {
        Address address = new Address("test@testor.com");
        CharSequence friendly = MessageHelper.toFriendly(address, contactsWithFakeContact,
                true, true, Color.RED);
        assertTrue(friendly instanceof SpannableString);
        assertEquals("Tim Testor", friendly.toString());
    }

    @Test
    public void testToFriendlyWithoutCorrespondentNames() throws Exception {
        Address address = new Address("test@testor.com", "Tim Testor");
        CharSequence friendly = MessageHelper.toFriendly(address, contactsWithFakeContact,
                false, false, 0);
        assertEquals("test@testor.com", friendly.toString());
    }

    @Test
    public void toFriendly_spoofPreventionOverridesPersonal() {
        Address address = new Address("test@testor.com", "potus@whitehouse.gov");
        CharSequence friendly = MessageHelper.toFriendly(address, contacts);
        assertEquals("test@testor.com", friendly.toString());
    }

    @Test
    public void toFriendly_spoofPreventionDoesntOverrideContact() {
        Address address = new Address("test@testor.com", "Tim Testor");
        CharSequence friendly = MessageHelper.toFriendly(address, contactsWithFakeSpoofContact,
                true, false, 0);
        assertEquals("Tim@Testor", friendly.toString());
    }
}
