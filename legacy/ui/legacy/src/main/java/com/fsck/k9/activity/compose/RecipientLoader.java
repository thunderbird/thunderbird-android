package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Data;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;
import androidx.core.content.ContextCompat;

import app.k9mail.core.android.common.database.EmptyCursor;
import com.fsck.k9.ui.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;
import org.apache.james.mime4j.util.CharsetUtil;
import timber.log.Timber;

import static java.lang.String.CASE_INSENSITIVE_ORDER;


public class RecipientLoader extends AsyncTaskLoader<List<Recipient>> {
    /*
     * Indexes of the fields in the projection. This must match the order in {@link #PROJECTION}.
     */
    private static final int INDEX_NAME = 1;
    private static final int INDEX_LOOKUP_KEY = 2;
    private static final int INDEX_EMAIL = 3;
    private static final int INDEX_EMAIL_TYPE = 4;
    private static final int INDEX_EMAIL_CUSTOM_LABEL = 5;
    private static final int INDEX_CONTACT_ID = 6;
    private static final int INDEX_PHOTO_URI = 7;
    private static final int INDEX_TIMES_CONTACTED = 8;
    private static final int INDEX_KEY_PRIMARY = 9;
    private static final int INDEX_STARRED = 10;

    private static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Email.DATA,
            ContactsContract.CommonDataKinds.Email.TYPE,
            ContactsContract.CommonDataKinds.Email.LABEL,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED,
            ContactsContract.Contacts.SORT_KEY_PRIMARY,
            ContactsContract.Contacts.STARRED,
    };

    private static final String SORT_ORDER = "" +
            ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED + " DESC, " +
            ContactsContract.Contacts.SORT_KEY_PRIMARY + "," +
            ContactsContract.CommonDataKinds.Email.IS_SUPER_PRIMARY + " DESC, " +
            ContactsContract.CommonDataKinds.Email.IS_PRIMARY + " DESC, " +
            ContactsContract.Contacts._ID;

    private static final String[] PROJECTION_NICKNAME = {
            ContactsContract.Data.CONTACT_ID,
    };

    private static final int INDEX_CONTACT_ID_FOR_NICKNAME = 0;

    private static final String[] PROJECTION_CRYPTO_ADDRESSES = {
            "address",
            "uid_address"
    };

    private static final int INDEX_USER_ID = 1;

    private static final String[] PROJECTION_CRYPTO_STATUS = {
            "address",
            "uid_key_status",
            "autocrypt_key_status"
    };

    private static final int INDEX_EMAIL_ADDRESS = 0;
    private static final int INDEX_EMAIL_STATUS = 1;
    private static final int INDEX_AUTOCRYPT_STATUS = 2;

    private static final int CRYPTO_PROVIDER_STATUS_UNTRUSTED = 1;
    private static final int CRYPTO_PROVIDER_STATUS_TRUSTED = 2;

    private static final Comparator<Recipient> RECIPIENT_COMPARATOR = new Comparator<Recipient>() {
        @Override
        public int compare(Recipient lhs, Recipient rhs) {
            if (rhs.starred != lhs.starred) {
                return rhs.starred ? 1 : -1;
            }

            int timesContactedDiff = rhs.timesContacted - lhs.timesContacted;
            if (timesContactedDiff != 0) {
                return timesContactedDiff;
            }

            if (lhs.sortKey == null && rhs.sortKey == null) {
                return 0;
            } else if (lhs.sortKey == null) {
                return 1;
            } else if (rhs.sortKey == null) {
                return -1;
            }

            return CASE_INSENSITIVE_ORDER.compare(lhs.sortKey, rhs.sortKey);
        }
    };


    private final String query;
    private final Address[] addresses;
    private final Uri contactUri;
    private final Uri lookupKeyUri;
    private final String cryptoProvider;
    private final ContentResolver contentResolver;

    private List<Recipient> cachedRecipients;
    private ForceLoadContentObserver observerContact, observerKey;

    private RecipientLoader(Context context) {
        super(context);
        this.query = null;
        this.lookupKeyUri = null;
        this.addresses = null;
        this.contactUri = null;

        this.cryptoProvider = null;
        this.contentResolver = context.getContentResolver();
    }

    public RecipientLoader(Context context, String cryptoProvider, String query) {
        super(context);
        this.query = query;
        this.lookupKeyUri = null;
        this.addresses = null;
        this.contactUri = null;
        this.cryptoProvider = cryptoProvider;

        contentResolver = context.getContentResolver();
    }

    public RecipientLoader(Context context, String cryptoProvider, Address... addresses) {
        super(context);
        this.query = null;
        this.addresses = addresses;
        this.contactUri = null;
        this.cryptoProvider = cryptoProvider;
        this.lookupKeyUri = null;

        contentResolver = context.getContentResolver();
    }

    public RecipientLoader(Context context, String cryptoProvider, Uri contactUri, boolean isLookupKey) {
        super(context);
        this.query = null;
        this.addresses = null;
        this.contactUri = isLookupKey ? null : contactUri;
        this.lookupKeyUri = isLookupKey ? contactUri : null;
        this.cryptoProvider = cryptoProvider;

        contentResolver = context.getContentResolver();
    }

    public static RecipientLoader getMostContactedRecipientLoader(Context context, final int maxRecipients) {
        return new RecipientLoader(context) {
            @Override
            public List<Recipient> loadInBackground() {
                return super.fillContactDataBySortOrder(maxRecipients);
            }
        };
    }

    @Override
    public List<Recipient> loadInBackground() {
        List<Recipient> recipients = new ArrayList<>();
        Map<String, Recipient> recipientMap = new HashMap<>();

        if (addresses != null) {
            fillContactDataFromAddresses(addresses, recipients, recipientMap);
        } else if (contactUri != null) {
            fillContactDataFromEmailContentUri(contactUri, recipients, recipientMap);
        } else if (query != null) {
            fillContactDataFromQuery(query, recipients, recipientMap);

            if (cryptoProvider != null) {
                fillContactDataFromCryptoProvider(query, recipients, recipientMap);
            }
        } else if (lookupKeyUri != null) {
            fillContactDataFromLookupKey(lookupKeyUri, recipients, recipientMap);
        } else {
            throw new IllegalStateException("loader must be initialized with query or list of addresses!");
        }

        return fillCryptoStatusData(recipients, recipientMap);
    }

    @NonNull
    private List<Recipient> fillCryptoStatusData(List<Recipient> recipients, Map<String, Recipient> recipientMap) {
        if (recipients.isEmpty()) {
            return recipients;
        }

        if (cryptoProvider != null) {
            fillCryptoStatusData(recipientMap);
        }

        return recipients;
    }


    private void fillContactDataFromCryptoProvider(String query, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        Cursor cursor;
        try {
            Uri queryUri = Uri.parse("content://" + cryptoProvider + ".provider.exported/autocrypt_status");
            cursor = contentResolver.query(queryUri, PROJECTION_CRYPTO_ADDRESSES, null,
                    new String[] { "%" + query + "%" }, null);

            if (cursor == null) {
                return;
            }
        } catch (Exception e) {
            Timber.e(e, "Couldn't obtain recipients from crypto provider!");
            return;
        }

        while (cursor.moveToNext()) {
            String uid = cursor.getString(INDEX_USER_ID);
            Address[] addresses = Address.parseUnencoded(uid);

            for (Address address : addresses) {
                String emailAddress = address.getAddress();
                if (!isSupportedEmailAddress(emailAddress) || recipientMap.containsKey(emailAddress)) {
                    continue;
                }

                Recipient recipient = new Recipient(address);
                recipients.add(recipient);
                recipientMap.put(emailAddress, recipient);
            }
        }

        cursor.close();
    }

    private void fillContactDataFromAddresses(Address[] addresses, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        for (Address address : addresses) {
            if (isSupportedEmailAddress(address.getAddress())) {
                // TODO actually query contacts - not sure if this is possible in a single query tho :(
                Recipient recipient = new Recipient(address);
                recipients.add(recipient);
                recipientMap.put(address.getAddress(), recipient);
            }
        }
    }

    private void fillContactDataFromEmailContentUri(Uri contactUri, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        Cursor cursor = contentResolver.query(contactUri, PROJECTION, null, null, null);

        if (cursor == null) {
            return;
        }

        fillContactDataFromCursor(cursor, recipients, recipientMap);
    }

    private void fillContactDataFromLookupKey(Uri lookupKeyUri, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        // We shouldn't try to access the contacts if we don't have the necessary permission
        if (!hasContactPermission())
            return;
        // We could use the contact id from the URI directly, but getting it from the lookup key is safer
        Uri contactContentUri = Contacts.lookupContact(contentResolver, lookupKeyUri);
        if (contactContentUri == null) {
            return;
        }

        String contactIdStr = getContactIdFromContactUri(contactContentUri);

        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                PROJECTION, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
                new String[] { contactIdStr }, null);

        if (cursor == null) {
            return;
        }

        fillContactDataFromCursor(cursor, recipients, recipientMap);
    }

    private static String getContactIdFromContactUri(Uri contactUri) {
        return contactUri.getLastPathSegment();
    }

    private boolean hasContactPermission() {
        return ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private Cursor getNicknameCursor(String nickname) {
        nickname = "%" + nickname + "%";

        Uri queryUriForNickname = ContactsContract.Data.CONTENT_URI;


        if (hasContactPermission()) {
            return contentResolver.query(queryUriForNickname,
                    PROJECTION_NICKNAME,
                    ContactsContract.CommonDataKinds.Nickname.NAME + " LIKE ? AND " +
                            Data.MIMETYPE + " = ?",
                    new String[] { nickname, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE },
                    null);
        } else {
            return new EmptyCursor();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void fillContactDataFromQuery(String query, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {

        boolean foundValidCursor = false;
        foundValidCursor |= fillContactDataFromNickname(query, recipients, recipientMap);
        foundValidCursor |= fillContactDataFromNameAndEmail(query, recipients, recipientMap, null);

        if (foundValidCursor) {
            Collections.sort(recipients, RECIPIENT_COMPARATOR);
            registerContentObserver();
        }

    }

    private void registerContentObserver() {
        if (observerContact != null) {
            observerContact = new ForceLoadContentObserver();
            contentResolver.registerContentObserver(Email.CONTENT_URI, false, observerContact);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private boolean fillContactDataFromNickname(String nickname, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {

        boolean hasContact = false;

        Uri queryUri = Email.CONTENT_URI;

        Cursor nicknameCursor = getNicknameCursor(nickname);

        if (nicknameCursor == null) {
            return hasContact;
        }

        try {
            while (nicknameCursor.moveToNext()) {
                String id = nicknameCursor.getString(INDEX_CONTACT_ID_FOR_NICKNAME);
                String selection = ContactsContract.Data.CONTACT_ID + " = ?";
                Cursor cursor = contentResolver
                        .query(queryUri, PROJECTION, selection, new String[] { id }, SORT_ORDER);

                fillContactDataFromCursor(cursor, recipients, recipientMap, null);

                hasContact = true;
            }
        } finally {
            nicknameCursor.close();
        }

        return hasContact;
    }

    private List<Recipient> fillContactDataBySortOrder(int maxRecipients) {
        List<Recipient> recipients = new ArrayList<>();

        Uri queryUri = Email.CONTENT_URI;

        Cursor cursor = null;
        if (hasContactPermission()) {
            cursor = contentResolver.query(queryUri, PROJECTION, null, null, SORT_ORDER);
        }

        if (cursor == null) {
            return recipients;
        }

        fillContactDataFromCursor(cursor, recipients, new HashMap<>(), maxRecipients);

        return recipients;
    }


    private boolean fillContactDataFromNameAndEmail(String query, List<Recipient> recipients,
            Map<String, Recipient> recipientMap, Integer maxTargets) {

        query = "%" + query + "%";

        Uri queryUri = Email.CONTENT_URI;

        String selection = Contacts.DISPLAY_NAME_PRIMARY + " LIKE ? " +
                " OR (" + Email.ADDRESS + " LIKE ? AND " + Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "')";
        String[] selectionArgs = { query, query };

        Cursor cursor = null;
        if (hasContactPermission()) {
            cursor = contentResolver.query(queryUri, PROJECTION, selection, selectionArgs, SORT_ORDER);
        }

        if (cursor == null) {
            return false;
        }

        fillContactDataFromCursor(cursor, recipients, recipientMap);

        return true;

    }

    private void fillContactDataFromCursor(Cursor cursor, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        fillContactDataFromCursor(cursor, recipients, recipientMap, null);
    }

    private void fillContactDataFromCursor(Cursor cursor, List<Recipient> recipients,
            Map<String, Recipient> recipientMap, @Nullable Integer maxRecipients) {

        while (cursor.moveToNext() && (maxRecipients == null || recipients.size() < maxRecipients)) {
            String name = cursor.getString(INDEX_NAME);
            String email = cursor.getString(INDEX_EMAIL);

            // already exists? just skip then
            if (email == null || !isSupportedEmailAddress(email) || recipientMap.containsKey(email)) {
                // TODO We should probably merging contacts with the same email address
                continue;
            }

            long contactId = cursor.getLong(INDEX_CONTACT_ID);
            String lookupKey = cursor.getString(INDEX_LOOKUP_KEY);
            int timesContacted = cursor.getInt(INDEX_TIMES_CONTACTED);
            String sortKey = cursor.getString(INDEX_KEY_PRIMARY);
            boolean starred = cursor.getInt(INDEX_STARRED) == 1;
            int addressType = cursor.getInt(INDEX_EMAIL_TYPE);
            String addressLabel = null;
            switch (addressType) {
                case ContactsContract.CommonDataKinds.Email.TYPE_HOME: {
                    addressLabel = getContext().getString(R.string.address_type_home);
                    break;
                }
                case ContactsContract.CommonDataKinds.Email.TYPE_WORK: {
                    addressLabel = getContext().getString(R.string.address_type_work);
                    break;
                }
                case ContactsContract.CommonDataKinds.Email.TYPE_OTHER: {
                    addressLabel = getContext().getString(R.string.address_type_other);
                    break;
                }
                case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE: {
                    // mobile isn't listed as an option contacts app, but it has a constant so we better support it
                    addressLabel = getContext().getString(R.string.address_type_mobile);
                    break;
                }
                case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM: {
                    addressLabel = cursor.getString(INDEX_EMAIL_CUSTOM_LABEL);
                    break;
                }
            }

            Recipient recipient = new Recipient(name, email, addressLabel, contactId, lookupKey,
                    timesContacted, sortKey, starred);

            if (recipient.isValidEmailAddress()) {

                recipient.photoThumbnailUri =
                        cursor.isNull(INDEX_PHOTO_URI) ? null : Uri.parse(cursor.getString(INDEX_PHOTO_URI));
                recipientMap.put(email, recipient);
                recipients.add(recipient);
            }
        }

        cursor.close();
    }

    private void fillCryptoStatusData(Map<String, Recipient> recipientMap) {
        List<String> recipientList = new ArrayList<>(recipientMap.keySet());
        String[] recipientAddresses = recipientList.toArray(new String[recipientList.size()]);

        Cursor cursor;
        Uri queryUri = Uri.parse("content://" + cryptoProvider + ".provider.exported/autocrypt_status");
        try {
            cursor = contentResolver.query(queryUri, PROJECTION_CRYPTO_STATUS, null, recipientAddresses, null);
        } catch (SecurityException e) {
            // TODO escalate error to crypto status?
            return;
        }

        initializeCryptoStatusForAllRecipients(recipientMap);

        if (cursor == null) {
            return;
        }

        while (cursor.moveToNext()) {
            String email = cursor.getString(INDEX_EMAIL_ADDRESS);
            int uidStatus = cursor.getInt(INDEX_EMAIL_STATUS);
            int autocryptStatus = cursor.getInt(INDEX_AUTOCRYPT_STATUS);

            int effectiveStatus = uidStatus > autocryptStatus ? uidStatus : autocryptStatus;

            for (Address address : Address.parseUnencoded(email)) {
                String emailAddress = address.getAddress();
                if (recipientMap.containsKey(emailAddress)) {
                    Recipient recipient = recipientMap.get(emailAddress);
                    switch (effectiveStatus) {
                        case CRYPTO_PROVIDER_STATUS_UNTRUSTED: {
                            if (recipient.getCryptoStatus() == RecipientCryptoStatus.UNAVAILABLE) {
                                recipient.setCryptoStatus(RecipientCryptoStatus.AVAILABLE_UNTRUSTED);
                            }
                            break;
                        }
                        case CRYPTO_PROVIDER_STATUS_TRUSTED: {
                            if (recipient.getCryptoStatus() != RecipientCryptoStatus.AVAILABLE_TRUSTED) {
                                recipient.setCryptoStatus(RecipientCryptoStatus.AVAILABLE_TRUSTED);
                            }
                            break;
                        }
                    }
                }
            }
        }
        cursor.close();

        if (observerKey != null) {
            observerKey = new ForceLoadContentObserver();
            contentResolver.registerContentObserver(queryUri, false, observerKey);
        }
    }

    private void initializeCryptoStatusForAllRecipients(Map<String, Recipient> recipientMap) {
        for (Recipient recipient : recipientMap.values()) {
            recipient.setCryptoStatus(RecipientCryptoStatus.UNAVAILABLE);
        }
    }

    @Override
    public void deliverResult(List<Recipient> data) {
        cachedRecipients = data;

        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {
        if (cachedRecipients != null) {
            super.deliverResult(cachedRecipients);
            return;
        }

        if (takeContentChanged() || cachedRecipients == null) {
            forceLoad();
        }
    }

    @Override
    protected void onAbandon() {
        super.onAbandon();

        if (observerKey != null) {
            contentResolver.unregisterContentObserver(observerKey);
        }
        if (observerContact != null) {
            contentResolver.unregisterContentObserver(observerContact);
        }
    }

    private boolean isSupportedEmailAddress(String address) {
        return CharsetUtil.isASCII(address);
    }
}
