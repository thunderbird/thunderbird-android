package com.fsck.k9.activity.compose;


import java.util.List;

import android.Manifest;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;

import com.fsck.k9.RobolectricTest;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import static android.provider.ContactsContract.CommonDataKinds.Email.TYPE_HOME;
import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SuppressWarnings("WeakerAccess")
public class RecipientLoaderTest extends RobolectricTest {
    static final String CRYPTO_PROVIDER = "cryptoProvider";
    static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.TYPE,
            ContactsContract.CommonDataKinds.Email.LABEL,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED,
            ContactsContract.Contacts.SORT_KEY_PRIMARY,
            ContactsContract.Contacts.STARRED
    };
    static final String[] PROJECTION_NICKNAME = {
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.Nickname.NAME
    };
    static final String[] PROJECTION_CRYPTO_ADDRESSES = { "address", "uid_address" };
    static final String[] PROJECTION_CRYPTO_STATUS = { "address", "uid_key_status", "autocrypt_key_status" };
    static final Address CONTACT_ADDRESS_1 = Address.parse("Contact Name <address@example.org>")[0];
    static final Address CONTACT_ADDRESS_2 = Address.parse("Other Contact Name <address_two@example.org>")[0];
    static final String TYPE = "" + TYPE_HOME;
    static final String[] CONTACT_1 =
            new String[] { "0", "Bob", "bob", "bob@host.com", TYPE, null, "1", null, "100", "Bob", "0" };
    static final String[] CONTACT_2 =
            new String[] { "2", "Bob2", "bob2", "bob2@host.com", TYPE, null, "2", null, "99", "Bob2", "0" };
    static final String[] CONTACT_NO_EMAIL =
            new String[] { "0", "Bob", "bob", null, TYPE, null, "1", null, "10", "Bob_noMail", "0" };
    static final String[] CONTACT_WITH_NICKNAME_NOT_CONTACTED =
            new String[] { "0", "Eve_notContacted", "eve_notContacted", "eve_notContacted@host.com", TYPE, null, "2",
                    null, "0", "Eve", "0" };
    static final String[] NICKNAME_NOT_CONTACTED = new String[] { "2", "Eves_Nickname_Bob" };

    static final String QUERYSTRING = "querystring";

    ShadowApplication shadowApp;
    Context context;
    ContentResolver contentResolver;


    @Before
    public void setUp() throws Exception {
        Application application = RuntimeEnvironment.application;
        shadowApp = Shadows.shadowOf(application);
        shadowApp.grantPermissions(Manifest.permission.READ_CONTACTS);
        shadowApp.grantPermissions(Manifest.permission.WRITE_CONTACTS);

        context = mock(Context.class);
        contentResolver = mock(ContentResolver.class);

        when(context.getApplicationContext()).thenReturn(application);
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
                        aryEq(PROJECTION_CRYPTO_ADDRESSES), nullable(String.class),
                        aryEq(new String[] { queriedAddress }),
                        nullable(String.class))).thenReturn(cursor);
    }

    private void setupCryptoProviderStatus(Address address, String uidStatus, String autocryptStatus) {
        MatrixCursor cursorCryptoStatus = new MatrixCursor(PROJECTION_CRYPTO_STATUS);
        cursorCryptoStatus.addRow(new String[] { address.getAddress(), uidStatus, autocryptStatus });

        when(contentResolver
                .query(eq(Uri.parse("content://" + CRYPTO_PROVIDER + ".provider.exported/autocrypt_status")),
                        aryEq(PROJECTION_CRYPTO_STATUS), nullable(String.class),
                        aryEq(new String[] { address.getAddress() }),
                        nullable(String.class))).thenReturn(cursorCryptoStatus);
    }

    private void setupContactProvider(String queriedAddress, String[]... contacts) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION);
        for (String[] contact : contacts) {
            cursor.addRow(contact);
        }
        when(contentResolver
                .query(eq(Email.CONTENT_URI),
                        aryEq(PROJECTION),
                        nullable(String.class),
                        aryEq(new String[] { queriedAddress, queriedAddress }),
                        nullable(String.class))).thenReturn(cursor);
    }

    private void setupNicknameContactProvider(String[]... contactsWithNickname) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION_NICKNAME);
        for (String[] contact : contactsWithNickname) {
            cursor.addRow(contact);
        }
        when(contentResolver
                .query(eq(ContactsContract.Data.CONTENT_URI),
                        aryEq(PROJECTION_NICKNAME),
                        nullable(String.class),
                        nullable(String[].class),
                        nullable(String.class))).thenReturn(cursor);
    }

    private void setupContactProviderForId(String id, String[]... contacts) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION);
        for (String[] contact : contacts) {
            cursor.addRow(contact);
        }
        when(contentResolver
                .query(eq(Email.CONTENT_URI),
                        aryEq(PROJECTION),
                        nullable(String.class),
                        aryEq(new String[] { id }),
                        nullable(String.class))).thenReturn(cursor);
    }


    @Test
    public void queryContactProvider() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, QUERYSTRING);
        setupContactProvider("%" + QUERYSTRING + "%", CONTACT_1);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(1, recipients.size());
        assertEquals("bob@host.com", recipients.get(0).address.getAddress());
        assertEquals(RecipientCryptoStatus.UNAVAILABLE, recipients.get(0).getCryptoStatus());
    }


    @Test
    public void queryContactProviderWithoutPermission() throws Exception {
        shadowApp.denyPermissions(Manifest.permission.READ_CONTACTS);
        shadowApp.denyPermissions(Manifest.permission.WRITE_CONTACTS);

        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, QUERYSTRING);
        setupContactProvider("%" + QUERYSTRING + "%", CONTACT_1);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(0, recipients.size());
    }

    @Test
    public void queryContactProvider_ignoresRecipientWithNoEmail() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, CRYPTO_PROVIDER, QUERYSTRING);
        setupContactProvider("%" + QUERYSTRING + "%", CONTACT_NO_EMAIL);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(0, recipients.size());
    }

    @Test
    public void queryContactProvider_sortByTimesContactedForNickname() throws Exception {
        RecipientLoader recipientLoader = new RecipientLoader(context, null, QUERYSTRING);
        setupContactProvider("%" + QUERYSTRING + "%", CONTACT_1);
        setupNicknameContactProvider(NICKNAME_NOT_CONTACTED);
        setupContactProviderForId(NICKNAME_NOT_CONTACTED[0], CONTACT_WITH_NICKNAME_NOT_CONTACTED);

        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(2, recipients.size());
        assertEquals("bob@host.com", recipients.get(0).address.getAddress());
        assertEquals("eve_notContacted@host.com", recipients.get(1).address.getAddress());
    }

    @Test
    public void getMostContactedFoundMore() throws Exception {
        int maxTargets = 1;
        setupContactProvider(CONTACT_1, CONTACT_2);

        RecipientLoader recipientLoader = RecipientLoader.getMostContactedRecipientLoader(context, maxTargets);
        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(maxTargets, recipients.size());
        assertEquals("bob@host.com", recipients.get(0).address.getAddress());
        assertEquals(RecipientCryptoStatus.UNDEFINED, recipients.get(0).getCryptoStatus());
    }

    @Test
    public void getMostContactedFoundLess() throws Exception {
        int maxTargets = 5;
        setupContactProvider(CONTACT_1, CONTACT_2);

        RecipientLoader recipientLoader = RecipientLoader.getMostContactedRecipientLoader(context, maxTargets);
        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(2, recipients.size());
        assertEquals("bob@host.com", recipients.get(0).address.getAddress());
        assertEquals(RecipientCryptoStatus.UNDEFINED, recipients.get(0).getCryptoStatus());
        assertEquals("bob2@host.com", recipients.get(1).address.getAddress());
        assertEquals(RecipientCryptoStatus.UNDEFINED, recipients.get(1).getCryptoStatus());
    }

    @Test
    public void getMostContactedFoundNothing() throws Exception {
        int maxTargets = 5;
        setupContactProvider();


        RecipientLoader recipientLoader = RecipientLoader.getMostContactedRecipientLoader(context, maxTargets);
        List<Recipient> recipients = recipientLoader.loadInBackground();

        assertEquals(0, recipients.size());
    }


    private void setupContactProvider(String[]... contacts) {
        MatrixCursor cursor = new MatrixCursor(PROJECTION);
        for (String[] contact : contacts) {
            cursor.addRow(contact);
        }
        when(contentResolver
                .query(eq(Email.CONTENT_URI),
                        aryEq(PROJECTION),
                        isNull(),
                        isNull(),
                        nullable(String.class))).thenReturn(cursor);

    }
}
