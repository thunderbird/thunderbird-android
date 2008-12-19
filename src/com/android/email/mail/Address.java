
package com.android.email.mail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.mime4j.field.address.NamedMailbox;
import org.apache.james.mime4j.field.address.parser.ParseException;

import android.util.Config;
import android.util.Log;

import com.android.email.Email;
import com.android.email.Utility;
import com.android.email.mail.internet.MimeUtility;

public class Address {
    String mAddress;

    String mPersonal;

    public Address(String address, String personal) {
        this.mAddress = address;
        this.mPersonal = personal;
    }

    public Address(String address) {
        this.mAddress = address;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    public String getPersonal() {
        return mPersonal;
    }

    public void setPersonal(String personal) {
        this.mPersonal = personal;
    }

    /**
     * Parse a comma separated list of addresses in RFC-822 format and return an
     * array of Address objects.
     * 
     * @param addressList
     * @return An array of 0 or more Addresses.
     */
    public static Address[] parse(String addressList) {
        ArrayList<Address> addresses = new ArrayList<Address>();
        if (addressList == null) {
            return new Address[] {};
        }
        try {
            MailboxList parsedList = AddressList.parse(addressList).flatten();
            for (int i = 0, count = parsedList.size(); i < count; i++) {
                org.apache.james.mime4j.field.address.Address address = parsedList.get(i);
                if (address instanceof NamedMailbox) {
                    NamedMailbox namedMailbox = (NamedMailbox)address;
                    addresses.add(new Address(namedMailbox.getLocalPart() + "@"
                            + namedMailbox.getDomain(), namedMailbox.getName()));
                } else if (address instanceof Mailbox) {
                    Mailbox mailbox = (Mailbox)address;
                    addresses.add(new Address(mailbox.getLocalPart() + "@" + mailbox.getDomain()));
                } else {
                    Log.e(Email.LOG_TAG, "Unknown address type from Mime4J: "
                            + address.getClass().toString());
                }

            }
        } catch (ParseException pe) {
        }
        return addresses.toArray(new Address[] {});
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Address) {
            return getAddress().equals(((Address) o).getAddress());
        }
        return super.equals(o);
    }

    public String toString() {
        if (mPersonal != null) {
            if (mPersonal.matches(".*[\\(\\)<>@,;:\\\\\".\\[\\]].*")) {
                return Utility.quoteString(mPersonal) + " <" + mAddress + ">";
            } else {
                return mPersonal + " <" + mAddress + ">";
            }
        } else {
            return mAddress;
        }
    }

    public static String toString(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < addresses.length; i++) {
            sb.append(addresses[i].toString());
            if (i < addresses.length - 1) {
                sb.append(',');
            }
        }
        return sb.toString();
    }
    
    /**
     * Returns either the personal portion of the Address or the address portion if the personal
     * is not available.
     * @return
     */
    public String toFriendly() {
        if (mPersonal != null && mPersonal.length() > 0) {
            return  mPersonal;
        }
        else {
            return mAddress;
        }
    }
    
    public static String toFriendly(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < addresses.length; i++) {
            sb.append(addresses[i].toFriendly());
            if (i < addresses.length - 1) {
                sb.append(',');
            }
        }
        return sb.toString();
    }
    
    /**
     * Unpacks an address list previously packed with packAddressList()
     * @param list
     * @return
     */
    public static Address[] unpack(String addressList) {
        if (addressList == null) {
            return new Address[] { };
        }
        ArrayList<Address> addresses = new ArrayList<Address>();
        int length = addressList.length();
        int pairStartIndex = 0;
        int pairEndIndex = 0;
        int addressEndIndex = 0;
        while (pairStartIndex < length) {
            pairEndIndex = addressList.indexOf(',', pairStartIndex);
            if (pairEndIndex == -1) {
                pairEndIndex = length;
            }
            addressEndIndex = addressList.indexOf(';', pairStartIndex);
            String address = null;
            String personal = null;
            if (addressEndIndex == -1 || addressEndIndex > pairEndIndex) {
                address = Utility.fastUrlDecode(addressList.substring(pairStartIndex, pairEndIndex));
            }
            else {
                address = Utility.fastUrlDecode(addressList.substring(pairStartIndex, addressEndIndex));
                personal = Utility.fastUrlDecode(addressList.substring(addressEndIndex + 1, pairEndIndex));
            }
            addresses.add(new Address(address, personal));
            pairStartIndex = pairEndIndex + 1;
        }
        return addresses.toArray(new Address[] { });
    }
    
    /**
     * Packs an address list into a String that is very quick to read
     * and parse. Packed lists can be unpacked with unpackAddressList()
     * The packed list is a comma seperated list of:
     * URLENCODE(address)[;URLENCODE(personal)] 
     * @param list
     * @return
     */
    public static String pack(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0, count = addresses.length; i < count; i++) {
            Address address = addresses[i];
            try {
                sb.append(URLEncoder.encode(address.getAddress(), "UTF-8"));
                if (address.getPersonal() != null) {
                    sb.append(';');
                    sb.append(URLEncoder.encode(address.getPersonal(), "UTF-8"));
                }
                if (i < count - 1) {
                    sb.append(',');
                }
            }
            catch (UnsupportedEncodingException uee) {
                return null;
            }
        }
        return sb.toString();
    }
}
