package com.fsck.k9.helper;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.MessageInfoHolder;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Message.RecipientType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageHelper {
    /**
     * If the number of addresses exceeds this value the addresses aren't
     * resolved to the names of Android contacts.
     *
     * <p>
     * TODO: This number was chosen arbitrarily and should be determined by
     * performance tests.
     * </p>
     */
    private static final int TOO_MANY_ADDRESSES = 50;

    /**
     * Number of address 'translations' we'll cache to prevent accessing the
     * contacts api over and over again for the same information.
     */
    private final int MAX_DISPLAYNAME_ENTRIES = 150;

    private static MessageHelper sInstance;
    private Map<Address, CharSequence> mDisplayNameCache;

    public synchronized static MessageHelper getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new MessageHelper(context);
        }
        return sInstance;
    }

    private Context mContext;

    private MessageHelper(final Context context) {
        mContext = context;
        mDisplayNameCache = new LinkedHashMap(MAX_DISPLAYNAME_ENTRIES+1, .75F, true) {
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_DISPLAYNAME_ENTRIES;
            }
        };
    }

    public void populate(final MessageInfoHolder target, final Message message,
                         final FolderInfoHolder folder, final Account account) {
        final Contacts contactHelper = K9.showContactName() ? Contacts.getInstance(mContext) : null;
        try {
            target.message = message;
            target.compareArrival = message.getInternalDate();
            target.compareDate = message.getSentDate();
            if (target.compareDate == null) {
                target.compareDate = message.getInternalDate();
            }

            target.folder = folder;

            target.read = message.isSet(Flag.SEEN);
            target.answered = message.isSet(Flag.ANSWERED);
            target.forwarded = message.isSet(Flag.FORWARDED);
            target.flagged = message.isSet(Flag.FLAGGED);

            Address[] addrs = message.getFrom();
            target.sender = getDisplayName(account, addrs, message.getRecipients(RecipientType.TO));

            if (addrs.length > 0) {
                target.senderAddress = addrs[0].getAddress();
            } else {
                // a reasonable fallback "whomever we were corresponding with
                target.senderAddress = target.sender.toString();
            }

            target.uid = message.getUid();

            target.account = account.getUuid();
            target.uri = "email://messages/" + account.getAccountNumber() + "/" + message.getFolder().getName() + "/" + message.getUid();

        } catch (MessagingException me) {
            Log.w(K9.LOG_TAG, "Unable to load message info", me);
        }
    }

    public CharSequence getDisplayName(Account account, Address[] fromAddrs, Address[] toAddrs) {
        Contacts contactHelper = K9.showContactName() ? Contacts.getInstance(mContext) : null;

        Address[] addresses;
        SpannableStringBuilder displayName;

        if (fromAddrs.length > 0 && account.isAnIdentity(fromAddrs[0])) {
            displayName = new SpannableStringBuilder(mContext.getString(R.string.message_to_label));
            addresses = toAddrs;
        } else {
            displayName = new SpannableStringBuilder();
            addresses = fromAddrs;
        }

        // this doesn't consider possibly cached contacts...
        if (addresses.length >= TOO_MANY_ADDRESSES) {
            // Don't look up contacts if the number of addresses is very high.
            contactHelper = null;
        }

        return buildAddresses(contactHelper, addresses, displayName);
    }

    public CharSequence buildFriendlyAddresses(Address[] addresses, Contacts contactHelper) {
        return buildAddresses(contactHelper, addresses, new SpannableStringBuilder());
    }

    private CharSequence buildAddresses(Contacts contactHelper, Address[] addresses, SpannableStringBuilder sb) {
        CharSequence name;
        for (int i = 0; i < addresses.length; i++) {
            name = mDisplayNameCache.get(addresses[i]);
            if (name == null) {
                name = addresses[i].toFriendly(contactHelper);
                mDisplayNameCache.put(addresses[i], name);
            }

            sb.append(name);
            if (i < addresses.length - 1) {
                sb.append(',');
            }
        }
        return sb;
    }

    public boolean toMe(Account account, Address[] toAddrs) {
        for (Address address : toAddrs) {
            if (account.isAnIdentity(address)) {
                return true;
            }
        }
        return false;
    }
}
