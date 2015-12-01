package com.fsck.k9.activity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.ContactsContract;

import com.fsck.k9.activity.RecipientSelectView.Recipient;
import com.fsck.k9.mail.Address;


@TargetApi(VERSION_CODES.JELLY_BEAN) // TODO get rid of this, affects cancellation behavior!
public class RecipientLoader extends AsyncTaskLoader<List<Recipient>> {

    /** Indexes of the fields in the projection. This must match the order in
     * {@link #PROJECTION}. */
    protected static final int INDEX_NAME = 1;
    protected static final int INDEX_EMAIL = 2;
    protected static final int INDEX_CONTACT_ID = 3;
    protected static final int INDEX_PHOTO_URI = 4;

    protected static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.DATA,
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
    private final String cryptoProvider;

    private List<Recipient> cachedRecipients;

    private ForceLoadContentObserver observerContact, observerKey;
    private CancellationSignal mCancellationSignal;

    public RecipientLoader(Context context, String cryptoProvider, String query) {
        super(context);
        this.query = query;
        this.addresses = null;
        this.cryptoProvider = cryptoProvider;
    }

    public RecipientLoader(Context context, String cryptoProvider, Address... addresses) {
        super(context);
        this.query = null;
        this.addresses = addresses;
        this.cryptoProvider = cryptoProvider;
    }

    @Override
    public List<Recipient> loadInBackground() {

        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
        try {

            ArrayList<Recipient> recipients = new ArrayList<Recipient>();
            HashMap<String,Recipient> recipientMap = new HashMap<String, Recipient>();

            if (addresses != null) {
                fillContactDataFromAddresses(addresses, recipients, recipientMap);
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

        } finally {
            synchronized (this) {
                mCancellationSignal = null;
            }
        }
    }

    private void fillContactDataFromAddresses(Address[] addresses, ArrayList<Recipient> recipients,
            HashMap<String, Recipient> recipientMap) {

        for (Address address : addresses) {
            // TODO actually query photos
            Recipient recipient = new Recipient(address);
            recipients.add(recipient);
            recipientMap.put(address.getAddress(), recipient);
        }

    }

    private void fillContactDataFromQuery(
            String query, ArrayList<Recipient> recipients, HashMap<String, Recipient> recipientMap) {

        Uri queryUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI,
                Uri.encode(query));
        Cursor cursor = getContext().getContentResolver().query(
                queryUri, PROJECTION, null, null, SORT_ORDER, mCancellationSignal);

        if (cursor == null) {
            return;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            String name = cursor.getString(INDEX_NAME);
            String email = cursor.getString(INDEX_EMAIL);
            long contactId = cursor.getLong(INDEX_CONTACT_ID);

            // already exists? just skip then
            if (recipientMap.containsKey(email)) {
                // TODO merge? do something else? what do we do?
                cursor.moveToNext();
                continue;
            }

            Uri photoUri = cursor.isNull(INDEX_PHOTO_URI)
                    ? null : Uri.parse(cursor.getString(INDEX_PHOTO_URI));
            Recipient recipient = new Recipient(name, email, contactId);
            recipient.photoThumbnailUri = photoUri;

            recipientMap.put(email, recipient);
            recipients.add(recipient);

            cursor.moveToNext();
        }
        cursor.close();

        if (observerContact != null) {
            observerContact = new ForceLoadContentObserver();
            getContext().getContentResolver().registerContentObserver(queryUri, false, observerContact);
        }

    }

    private void fillCryptoStatusData(HashMap<String, Recipient> recipientMap) {
        ArrayList<String> recipientArrayList = new ArrayList<String>(recipientMap.keySet());
        String[] recipientAddresses = recipientArrayList.toArray(new String[ recipientArrayList.size() ]);

        Uri queryUri = Uri.parse("content://" + cryptoProvider + ".provider.exported/email_status");
        Cursor cursor = getContext().getContentResolver().query(
                queryUri, PROJECTION_CRYPTO_STATUS, null, recipientAddresses, null, mCancellationSignal);

        if (cursor == null) {
            return;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String email = cursor.getString(0);
            int status = cursor.getInt(1);

            for (Address address : Address.parseUnencoded(email)) {
                if (recipientMap.containsKey(address.getAddress())) {
                    recipientMap.get(address.getAddress()).cryptoStatus = status;
                }
            }

            cursor.moveToNext();
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

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }
    }

}
