package com.fsck.k9.helper;


import java.util.regex.Pattern;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.fsck.k9.Account;
import com.fsck.k9.CoreResourceProvider;
import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Address;

public class MessageHelper {
    /**
     * If the number of addresses exceeds this value the addresses aren't
     * resolved to the names of Android contacts.
     *
     * <p>
     * TODO: This number was chosen arbitrarily and should be determined by
     * performance tests.
     * </p>
     *
     * @see #toFriendly(Address[], com.fsck.k9.helper.Contacts)
     */
    private static final int TOO_MANY_ADDRESSES = 50;
    private static final Pattern SPOOF_ADDRESS_PATTERN = Pattern.compile("[^(]@");

    private static MessageHelper sInstance;

    public synchronized static MessageHelper getInstance(final Context context) {
        if (sInstance == null) {
            CoreResourceProvider resourceProvider = DI.get(CoreResourceProvider.class);
            sInstance = new MessageHelper(context, resourceProvider);
        }
        return sInstance;
    }

    private final CoreResourceProvider resourceProvider;
    private Context mContext;

    private MessageHelper(Context context, CoreResourceProvider resourceProvider) {
        mContext = context;
        this.resourceProvider = resourceProvider;
    }

    public CharSequence getSenderDisplayName(Address address) {
        if (address == null) {
            return resourceProvider.contactUnknownSender();
        }

        Contacts contactHelper = K9.isShowContactName() ? Contacts.getInstance(mContext) : null;
        return toFriendly(address, contactHelper);
    }

    public CharSequence getRecipientDisplayNames(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return resourceProvider.contactUnknownRecipient();
        }

        Contacts contactHelper = K9.isShowContactName() ? Contacts.getInstance(mContext) : null;
        CharSequence recipients = toFriendly(addresses, contactHelper);
        return new SpannableStringBuilder(resourceProvider.contactDisplayNamePrefix()).append(recipients);
    }

    public boolean toMe(Account account, Address[] toAddrs) {
        for (Address address : toAddrs) {
            if (account.isAnIdentity(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of the contact this email address belongs to if
     * the {@link Contacts contacts} parameter is not {@code null} and a
     * contact is found. Otherwise the personal portion of the {@link Address}
     * is returned. If that isn't available either, the email address is
     * returned.
     *
     * @param address An {@link com.fsck.k9.mail.Address}
     * @param contacts A {@link Contacts} instance or {@code null}.
     * @return A "friendly" name for this {@link Address}.
     */
    public static CharSequence toFriendly(Address address, Contacts contacts) {
        return toFriendly(address,contacts,
                K9.isShowCorrespondentNames(),
                K9.isChangeContactNameColor(),
                K9.getContactNameColor());
    }

    public static CharSequence toFriendly(Address[] addresses, Contacts contacts) {
        if (addresses == null) {
            return null;
        }

        if (addresses.length >= TOO_MANY_ADDRESSES) {
            // Don't look up contacts if the number of addresses is very high.
            contacts = null;
        }

        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            sb.append(toFriendly(addresses[i], contacts));
            if (i < addresses.length - 1) {
                sb.append(',');
            }
        }
        return sb;
    }

    /* package, for testing */ static CharSequence toFriendly(Address address, Contacts contacts,
                                                 boolean showCorrespondentNames,
                                                 boolean changeContactNameColor,
                                                 int contactNameColor) {
        if (!showCorrespondentNames) {
            return address.getAddress();
        } else if (contacts != null) {
            final String name = contacts.getNameForAddress(address.getAddress());
            if (name != null) {
                if (changeContactNameColor) {
                    final SpannableString coloredName = new SpannableString(name);
                    coloredName.setSpan(new ForegroundColorSpan(contactNameColor),
                            0,
                            coloredName.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    return coloredName;
                } else {
                    return name;
                }
            }
        }

        if (!TextUtils.isEmpty(address.getPersonal()) && !isSpoofAddress(address.getPersonal())) {
            return address.getPersonal();
        } else {
            return address.getAddress();
        }
    }

    private static boolean isSpoofAddress(String displayName) {
        return displayName.contains("@") && SPOOF_ADDRESS_PATTERN.matcher(displayName).find();
    }
}
