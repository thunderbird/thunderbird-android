package com.fsck.k9.helper;

import android.support.annotation.VisibleForTesting;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Address;

import biweekly.property.Attendee;
import biweekly.property.Organizer;

/**
 * Performs similar functionality to {@link MessageHelper} but for iCalendar data
 */
public class ICalendarHelper {
    /**
     * If the number of addresses exceeds this value the addresses aren't
     * resolved to the names of Android contacts.
     *
     * <p>
     * TODO: This number was chosen arbitrarily and should be determined by
     * performance tests.
     * </p>
     *
     * @see #toFriendly(Attendee[], com.fsck.k9.helper.Contacts)
     */
    private static final int TOO_MANY_ADDRESSES = 50;

    /**
     * Returns the name of the contact this attendee belongs to if
     * the {@link Contacts contacts} parameter is not {@code null} and a
     * contact is found. Otherwise the personal portion of the {@link Attendee}
     * is returned. If that isn't available either, the email address is
     * returned.
     *
     * @param attendee An {@link biweekly.property.Attendee}
     * @param contacts A {@link Contacts} instance or {@code null}.
     * @return A "friendly" name for this {@link Address}.
     */
    //TODO: Should this use separate settings for name vs email?
    public static CharSequence toFriendly(Attendee attendee, Contacts contacts) {
        return toFriendly(attendee,contacts,
                K9.showCorrespondentNames(),
                K9.changeContactNameColor(),
                K9.getContactNameColor());
    }

    //TODO: Should this use separate settings for name vs email?
    public static CharSequence toFriendly(Organizer organizer, Contacts contacts) {
        return toFriendly(organizer,contacts,
                K9.showCorrespondentNames(),
                K9.changeContactNameColor(),
                K9.getContactNameColor());
    }

    public static CharSequence toFriendly(Attendee[] attendees, Contacts contacts) {
        if (attendees == null) {
            return null;
        }

        if (attendees.length >= TOO_MANY_ADDRESSES) {
            // Don't look up contacts if the number of addresses is very high.
            contacts = null;
        }

        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < attendees.length; i++) {
            sb.append(toFriendly(attendees[i], contacts));
            if (i < attendees.length - 1) {
                sb.append(", ");
            }
        }
        return sb;
    }

    @VisibleForTesting
    static CharSequence toFriendly(Attendee attendee, Contacts contacts,
                                   boolean showCorrespondentNames,
                                   boolean changeContactNameColor,
                                   int contactNameColor) {
        if (!showCorrespondentNames) {
            return attendee.getEmail();
        } else if (contacts != null) {
            final String name = contacts.getNameForAddress(attendee.getEmail());
            // TODO: The results should probably be cached for performance reasons.
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

        return (!TextUtils.isEmpty(attendee.getCommonName())) ? attendee.getCommonName() : attendee.getEmail();
    }

    @VisibleForTesting
    static CharSequence toFriendly(Organizer organizer, Contacts contacts,
                                   boolean showCorrespondentNames,
                                   boolean changeContactNameColor,
                                   int contactNameColor) {
        if (organizer == null) {
            return null;
        }
        if (!showCorrespondentNames) {
            return organizer.getEmail();
        } else if (contacts != null) {
            final String name = contacts.getNameForAddress(organizer.getEmail());
            // TODO: The results should probably be cached for performance reasons.
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

        return (!TextUtils.isEmpty(organizer.getCommonName())) ? organizer.getCommonName() : organizer.getEmail();
    }
}
