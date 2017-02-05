package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Data;

import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;


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

    private static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Email.DATA,
            ContactsContract.CommonDataKinds.Email.TYPE,
            ContactsContract.CommonDataKinds.Email.LABEL,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
    };
    private static final String SORT_ORDER = "" +
            ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED + " DESC, " +
            ContactsContract.Contacts.SORT_KEY_PRIMARY;

    private static final int INDEX_EMAIL_ADDRESS = 0;
    private static final int INDEX_EMAIL_STATUS = 1;

    private static final String[] PROJECTION_CRYPTO_STATUS = {
            "email_address",
            "email_status"
    };

    private static final int CRYPTO_PROVIDER_STATUS_UNTRUSTED = 1;
    private static final int CRYPTO_PROVIDER_STATUS_TRUSTED = 2;


    private final String query;
    private final Address[] addresses;
    private final Uri contactUri;
    private final Uri lookupKeyUri;
    private final String cryptoProvider;

    private List<Recipient> cachedRecipients;
    private ForceLoadContentObserver observerContact, observerKey;


    public RecipientLoader(Context context, String cryptoProvider, String query) {
        super(context);
        this.query = query;
        this.lookupKeyUri = null;
        this.addresses = null;
        this.contactUri = null;
        this.cryptoProvider = cryptoProvider;
    }

    public RecipientLoader(Context context, String cryptoProvider, Address... addresses) {
        super(context);
        this.query = null;
        this.addresses = addresses;
        this.contactUri = null;
        this.cryptoProvider = cryptoProvider;
        this.lookupKeyUri = null;
    }

    public RecipientLoader(Context context, String cryptoProvider, Uri contactUri, boolean isLookupKey) {
        super(context);
        this.query = null;
        this.addresses = null;
        this.contactUri = isLookupKey ? null : contactUri;
        this.lookupKeyUri = isLookupKey ? contactUri : null;
        this.cryptoProvider = cryptoProvider;
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
        } else if (lookupKeyUri != null) {
            fillContactDataFromLookupKey(lookupKeyUri, recipients, recipientMap);
        } else {
            throw new IllegalStateException("loader must be initialized with query or list of addresses!");
        }

        if (recipients.isEmpty()) {
            return recipients;
        }

        if (cryptoProvider != null) {
            fillCryptoStatusData(recipientMap);
        }

        return recipients;
    }

    private void fillContactDataFromAddresses(Address[] addresses, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        for (Address address : addresses) {
            // TODO actually query contacts - not sure if this is possible in a single query tho :(
            Recipient recipient = new Recipient(address);
            recipients.add(recipient);
            recipientMap.put(address.getAddress(), recipient);
        }
    }

    private void fillContactDataFromEmailContentUri(Uri contactUri, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        Cursor cursor = getContext().getContentResolver().query(contactUri, PROJECTION, null, null, null);

        if (cursor == null) {
            return;
        }

        fillContactDataFromCursor(cursor, recipients, recipientMap);
    }

    private void fillContactDataFromLookupKey(Uri lookupKeyUri, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {
        // We could use the contact id from the URI directly, but getting it from the lookup key is safer
        Uri contactContentUri = Contacts.lookupContact(getContext().getContentResolver(), lookupKeyUri);
        if (contactContentUri == null) {
            return;
        }

        String contactIdStr = getContactIdFromContactUri(contactContentUri);

        Cursor cursor = getContext().getContentResolver().query(
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


    private void fillContactDataFromQuery(String query, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {

        ContentResolver contentResolver = getContext().getContentResolver();

        query = "%" + query + "%";
        Uri queryUri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String selection = Contacts.DISPLAY_NAME_PRIMARY + " LIKE ? " +
                " OR (" + Email.ADDRESS + " LIKE ? AND " + Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "')";
        String[] selectionArgs = { query, query };
        Cursor cursor = contentResolver.query(queryUri, PROJECTION, selection, selectionArgs, SORT_ORDER);

        if (cursor == null) {
            return;
        }

        fillContactDataFromCursor(cursor, recipients, recipientMap);

        if (observerContact != null) {
            observerContact = new ForceLoadContentObserver();
            contentResolver.registerContentObserver(queryUri, false, observerContact);
        }
    }

    private void fillContactDataFromCursor(Cursor cursor, List<Recipient> recipients,
            Map<String, Recipient> recipientMap) {

        while (cursor.moveToNext()) {
            String name = cursor.getString(INDEX_NAME);
            String email = cursor.getString(INDEX_EMAIL);
            long contactId = cursor.getLong(INDEX_CONTACT_ID);
            String lookupKey = cursor.getString(INDEX_LOOKUP_KEY);

            // already exists? just skip then
            if (recipientMap.containsKey(email)) {
                // TODO merge? do something else? what do we do?
                continue;
            }

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

            Recipient recipient = new Recipient(name, email, addressLabel, contactId, lookupKey);
            if (recipient.isValidEmailAddress()) {
                Uri photoUri = cursor.isNull(INDEX_PHOTO_URI) ? null : Uri.parse(cursor.getString(INDEX_PHOTO_URI));

                recipient.photoThumbnailUri = photoUri;
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
        Uri queryUri = Uri.parse("content://" + cryptoProvider + ".provider.exported/email_status");
        try {
            cursor = getContext().getContentResolver().query(queryUri, PROJECTION_CRYPTO_STATUS, null,
                    recipientAddresses, null);
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
            int status = cursor.getInt(INDEX_EMAIL_STATUS);

            for (Address address : Address.parseUnencoded(email)) {
                String emailAddress = address.getAddress();
                if (recipientMap.containsKey(emailAddress)) {
                    Recipient recipient = recipientMap.get(emailAddress);
                    switch (status) {
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
            getContext().getContentResolver().registerContentObserver(queryUri, false, observerKey);
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
            getContext().getContentResolver().unregisterContentObserver(observerKey);
        }
        if (observerContact != null) {
            getContext().getContentResolver().unregisterContentObserver(observerContact);
        }
    }
}
