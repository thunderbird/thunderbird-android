package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;

import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.provider.ContactsContract.CommonDataKinds.Email.TYPE_HOME;
import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SuppressWarnings("WeakerAccess")
@RunWith(K9RobolectricTestRunner.class)
public class RecipientLoaderTest {
    static final String CRYPTO_PROVIDER = "cryptoProvider";
    static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.TYPE,
            ContactsContract.CommonDataKinds.Email.LABEL,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
    };
    static final String[] PROJECTION_CRYPTO_ADDRESSES = { "address", "uid_address" };
    static final String[] PROJECTION_CRYPTO_STATUS = { "address", "uid_key_status", "autocrypt_key_status" };
    static final Address CONTACT_ADDRESS_1 = Address.parse("Contact Name <address@example.org>")[0];
    static final Address CONTACT_ADDRESS_2 = Address.parse("Other Contact Name <address_two@example.org>")[0];
    static final String[] CONTACT_1 = new String[] {"0", "Bob", "bob", "bob@host.com", ""+TYPE_HOME, null, "1", null};
    static final String[] CONTACT_NO_EMAIL = new String[] {"0", "Bob", "bob", null, ""+TYPE_HOME, null, "1", null};

    static final String CONTACT_ID_NOT_CONTACTED ="2";
    static final String CONTACT_ID_CONTACTED ="3";
    static final String[] CONTACT_WITH_NICKNAME_NOT_CONTACTED = new String[] {"0", "Eve_notContacted", "eve_notContacted", "eve_notContacted@host.com", ""+TYPE_HOME, null, CONTACT_ID_NOT_CONTACTED, null};
    static final String[] CONTACT_WITH_NICKNAME_CONTACTED = new String[] {"0", "Jule", "jule", "jule@host.com", ""+TYPE_HOME, null, CONTACT_ID_CONTACTED, null};


    static final String[] NICKNAME_NOT_CONTACTED = new String[]{CONTACT_ID_NOT_CONTACTED, "Eves_Nickname_Bob"};
    static final String[] NICKNAME_CONTACTED = new String[]{CONTACT_ID_CONTACTED, "Jules_Nickname_Bob"};

    static final String QUERYSTRING = "querystring";


    private static final String[] PROJECTION_NICKNAME = {
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.Nickname.NAME
    };



    Context context;
    ContentResolver contentResolver;


    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        contentResolver = mock(ContentResolver.class);

        when(context.getApplicationContext()).thenReturn(RuntimeEnvironment.application);

        when(context.getContentResolver()).thenReturn(contentResolver);
    }

    @Test
    public void queryCryptoProvider() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, QUERYSTRING);

        setupQueryCryptoProvider("%" + QUERYSTRING + "%", CONTACT_ADDRESS_1, CONTACT_ADDRESS_2);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(2, recipients.size());
        assertEquals(CONTACT_ADDRESS_1, recipients.get(0).address);
        assertEquals(CONTACT_ADDRESS_2, recipients.get(1).address);
        assertEquals(RecipientCryptoStatus.UNAVAILABLE, recipients.get(0).getCryptoStatus());
    }

    @Test
    public void queryCryptoStatus_unavailable() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, CONTACT_ADDRESS_1);

        setupCryptoProviderStatus(CONTACT_ADDRESS_1, "0", "0");

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(1, recipients.size());
        Recipient recipient = recipients.get(0);
        assertEquals(CONTACT_ADDRESS_1, recipient.address);
        assertEquals(RecipientCryptoStatus.UNAVAILABLE, recipient.getCryptoStatus());
    }

    @Test
    public void queryCryptoStatus_autocrypt_untrusted() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, CONTACT_ADDRESS_1);

        setupCryptoProviderStatus(CONTACT_ADDRESS_1, "0", "1");

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(1, recipients.size());
        Recipient recipient = recipients.get(0);
        assertEquals(CONTACT_ADDRESS_1, recipient.address);
        assertEquals(RecipientCryptoStatus.AVAILABLE_UNTRUSTED, recipient.getCryptoStatus());
    }

    @Test
    public void queryCryptoStatus_autocrypt_trusted() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, CONTACT_ADDRESS_1);

        setupCryptoProviderStatus(CONTACT_ADDRESS_1, "0", "2");

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(1, recipients.size());
        Recipient recipient = recipients.get(0);
        assertEquals(CONTACT_ADDRESS_1, recipient.address);
        assertEquals(RecipientCryptoStatus.AVAILABLE_TRUSTED, recipient.getCryptoStatus());
    }

    @Test
    public void queryCryptoStatus_withHigherUidStatus() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, CONTACT_ADDRESS_1);

        setupCryptoProviderStatus(CONTACT_ADDRESS_1, "2", "1");

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(1, recipients.size());
        Recipient recipient = recipients.get(0);
        assertEquals(CONTACT_ADDRESS_1, recipient.address);
        assertEquals(RecipientCryptoStatus.AVAILABLE_TRUSTED, recipient.getCryptoStatus());
    }

    private void setupQueryCryptoProvider(String queriedAddress, Address... contactAddresses) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION_CRYPTO_ADDRESSES);
        for (Address contactAddress : contactAddresses) {
            cursor.addRow(new String[] { queriedAddress, contactAddress.toString() });
        }

        when(contentResolver
                .query(eq(Uri.parse("content://" + CRYPTO_PROVIDER + ".provider.exported/autocrypt_status")),
                        aryEq(PROJECTION_CRYPTO_ADDRESSES), any(String.class),
                        aryEq(new String[] { queriedAddress }),
                        any(String.class))).thenReturn(cursor);
    }

    private void setupCryptoProviderStatus(Address address, String uidStatus, String autocryptStatus) {
        MatrixCursor cursorCryptoStatus = new MatrixCursor(PROJECTION_CRYPTO_STATUS);
        cursorCryptoStatus.addRow(new String[] { address.getAddress(), uidStatus, autocryptStatus });

        when(contentResolver
                .query(eq(Uri.parse("content://" + CRYPTO_PROVIDER + ".provider.exported/autocrypt_status")),
                        aryEq(PROJECTION_CRYPTO_STATUS), any(String.class),
                        aryEq(new String[] { address.getAddress() }),
                        any(String.class))).thenReturn(cursorCryptoStatus);
    }

    private void setupContactProvider(String queriedAddress, List<String> additionalIds, String[]... contacts) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION);
        for (String[] contact : contacts) {
            cursor.addRow(contact);
        }
        List<String> parameters = new ArrayList<>();
        parameters.add(queriedAddress);
        parameters.add(queriedAddress);
        parameters.addAll(additionalIds);
        when(contentResolver
                .query(eq(Email.CONTENT_URI),
                        aryEq(PROJECTION),
                        any(String.class),
                        aryEq(parameters.toArray(new String[parameters.size()])),
                        any(String.class))).thenReturn(cursor);
    }

    private void setupNicknameContactProvider(String queriedAddress, String[]... contactsWithNickname) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION_NICKNAME);
        for (String[] contact : contactsWithNickname) {
            cursor.addRow(contact);
        }
        when(contentResolver
                .query(eq(ContactsContract.Data.CONTENT_URI),
                        aryEq(PROJECTION_NICKNAME),
                        any(String.class),
                        any(String[].class),
                        any(String.class))).thenReturn(cursor);
    }

    @Test
    public void queryContactProvider() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, QUERYSTRING);
        setupContactProvider("%" + QUERYSTRING + "%", Collections.EMPTY_LIST, CONTACT_1);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(1, recipients.size());
        assertEquals("bob@host.com", recipients.get(0).address.getAddress());
        assertEquals(RecipientCryptoStatus.UNAVAILABLE, recipients.get(0).getCryptoStatus());
    }

    @Test
    public void queryContactProvider_ignoresRecipientWithNoEmail() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, QUERYSTRING);
        List<String> additionalIds = new ArrayList<>();
        additionalIds.add(CONTACT_ID_NOT_CONTACTED);
        setupContactProvider("%" + QUERYSTRING + "%", additionalIds, CONTACT_NO_EMAIL);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(0, recipients.size());
    }


    /**
     * Nickname should be sorted as querying others (more times contacted first)
     */
    @Test
    public void queryContactProvider_sortByContactedForNickname() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, null, QUERYSTRING);
        List<String> additionalIds = new ArrayList<>();
        additionalIds.add(CONTACT_ID_NOT_CONTACTED);
        setupContactProvider("%" + QUERYSTRING + "%", additionalIds, CONTACT_1, CONTACT_WITH_NICKNAME_NOT_CONTACTED);
        setupNicknameContactProvider("%" + QUERYSTRING + "%", NICKNAME_NOT_CONTACTED);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(2, recipients.size());
        assertEquals("bob@host.com", recipients.get(0).address.getAddress());
        assertEquals("eve_notContacted@host.com", recipients.get(1).address.getAddress());
    }

    @Test
    public void queryContactProvider_sortByContactedForNickname1() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, null, QUERYSTRING);
        List<String> additionalIds = new ArrayList<>();
        additionalIds.add(CONTACT_ID_NOT_CONTACTED);
        additionalIds.add(CONTACT_ID_CONTACTED);
        setupContactProvider("%" + QUERYSTRING + "%", additionalIds, CONTACT_1, CONTACT_WITH_NICKNAME_CONTACTED, CONTACT_WITH_NICKNAME_NOT_CONTACTED);
        setupNicknameContactProvider("%" + QUERYSTRING + "%", NICKNAME_NOT_CONTACTED, NICKNAME_CONTACTED);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(3, recipients.size());
        assertEquals("bob@host.com", recipients.get(0).address.getAddress());
        assertEquals("jule@host.com", recipients.get(1).address.getAddress());
        assertEquals("eve_notContacted@host.com", recipients.get(2).address.getAddress());
    }
}