package com.fsck.k9.activity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;


public class RecipientLoader extends AsyncTaskLoader<List<Recipient>> {

    /** Indexes of the fields in the projection. This must match the order in
     * {@link #PROJECTION}. */
    protected static final int INDEX_NAME = 1;
    protected static final int INDEX_LOOKUP_KEY = 2;
    protected static final int INDEX_EMAIL = 3;
    protected static final int INDEX_EMAIL_TYPE = 4;
    protected static final int INDEX_EMAIL_CUSTOM_LABEL = 5;
    protected static final int INDEX_CONTACT_ID = 6;
    protected static final int INDEX_PHOTO_URI = 7;

    protected static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Email.DATA,
            ContactsContract.CommonDataKinds.Email.TYPE,
            ContactsContract.CommonDataKinds.Email.LABEL,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
    };

    protected static final String[] PROJECTION_CRYPTO_STATUS = {
            "email_address",
            "email_status"
    };

    protected static final String SORT_ORDER =
            ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED + " DESC, " +
                    ContactsContract.Contacts.DISPLAY_NAME + ", " +
                    ContactsContract.CommonDataKinds.Email._ID;

    private final String query;
    private final Address[] addresses;
    private final Uri contactUri;

    private final String cryptoProvider;

    private List<Recipient> cachedRecipients;

    private ForceLoadContentObserver observerContact, observerKey;

    public RecipientLoader(Context context, String cryptoProvider, String query) {
        super(context);
        this.query = query;
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
    }

    public RecipientLoader(Context context, String cryptoProvider, Uri contactUri) {
        super(context);
        this.query = null;
        this.addresses = null;
        this.contactUri = contactUri;
        this.cryptoProvider = cryptoProvider;
    }

    @Override
    public List<Recipient> loadInBackground() {
        ArrayList<Recipient> recipients = new ArrayList<Recipient>();
        HashMap<String,Recipient> recipientMap = new HashMap<String, Recipient>();

        if (addresses != null) {
            fillContactDataFromAddresses(addresses, recipients, recipientMap);
        } else if (contactUri != null) {
            fillContactDataFromContactUri(contactUri, recipients, recipientMap);
        } else if (query != null) {
            fillContactDataFromQuery(query, recipients, recipientMap);
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

    private void fillContactDataFromAddresses(Address[] addresses, ArrayList<Recipient> recipients,
            HashMap<String, Recipient> recipientMap) {
        for (Address address : addresses) {
            // TODO actually query contacts - not sure if this is possible in a single query tho :(
            Recipient recipient = new Recipient(address);
            recipients.add(recipient);
            recipientMap.put(address.getAddress(), recipient);
        }
    }

    private void fillContactDataFromContactUri(
            Uri contactUri, ArrayList<Recipient> recipients, HashMap<String, Recipient> recipientMap) {
        // Get the contact id from the Uri
        String contactIdStr = contactUri.getLastPathSegment();

        Cursor cursor = getContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                PROJECTION, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
                new String[] { contactIdStr }, null);

        if (cursor == null) {
            return;
        }

        fillContactDataFromCursor(cursor, recipients, recipientMap);
    }

    private void fillContactDataFromQuery(
            String query, ArrayList<Recipient> recipients, HashMap<String, Recipient> recipientMap) {
        Uri queryUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI,
                Uri.encode(query));
        Cursor cursor = getContext().getContentResolver().query(queryUri, PROJECTION, null, null, SORT_ORDER);

        if (cursor == null) {
            return;
        }

        fillContactDataFromCursor(cursor, recipients, recipientMap);

        if (observerContact != null) {
            observerContact = new ForceLoadContentObserver();
            getContext().getContentResolver().registerContentObserver(queryUri, false, observerContact);
        }
    }

    private void fillContactDataFromCursor(Cursor cursor, ArrayList<Recipient> recipients,
            HashMap<String, Recipient> recipientMap) {
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
                case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                    addressLabel = getContext().getString(R.string.address_type_home);
                    break;
                case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                    addressLabel = getContext().getString(R.string.address_type_work);
                    break;
                case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
                    addressLabel = getContext().getString(R.string.address_type_other);
                    break;
                case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
                    // mobile isn't listed as an option contacts app, but it has a constant so we better support it
                    addressLabel = getContext().getString(R.string.address_type_mobile);
                    break;
                case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM:
                    addressLabel = cursor.getString(INDEX_EMAIL_CUSTOM_LABEL);
                    break;
            }

            Uri photoUri = cursor.isNull(INDEX_PHOTO_URI)
                    ? null : Uri.parse(cursor.getString(INDEX_PHOTO_URI));
            Recipient recipient = new Recipient(name, email, addressLabel, contactId, lookupKey);
            recipient.photoThumbnailUri = photoUri;

            recipientMap.put(email, recipient);
            recipients.add(recipient);
        }
        cursor.close();
    }

    private void fillCryptoStatusData(HashMap<String, Recipient> recipientMap) {
        ArrayList<String> recipientArrayList = new ArrayList<String>(recipientMap.keySet());
        String[] recipientAddresses = recipientArrayList.toArray(new String[ recipientArrayList.size() ]);

        Uri queryUri = Uri.parse("content://" + cryptoProvider + ".provider.exported/email_status");
        Cursor cursor = getContext().getContentResolver().query(
                queryUri, PROJECTION_CRYPTO_STATUS, null, recipientAddresses, null);

        // fill all values with "unavailable", even if the query fails
        for (Recipient recipient : recipientMap.values()) {
            recipient.setCryptoStatus(RecipientCryptoStatus.UNAVAILABLE);
        }

        if (cursor == null) {
            return;
        }

        while (cursor.moveToNext()) {
            String email = cursor.getString(0);
            int status = cursor.getInt(1);

            for (Address address : Address.parseUnencoded(email)) {
                if (recipientMap.containsKey(address.getAddress())) {
                    Recipient recipient = recipientMap.get(address.getAddress());
                    switch (status) {
                        case 1:
                            recipient.setCryptoStatus(RecipientCryptoStatus.AVAILABLE_UNTRUSTED);
                            break;
                        case 2:
                            recipient.setCryptoStatus(RecipientCryptoStatus.AVAILABLE_TRUSTED);
                            break;
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
