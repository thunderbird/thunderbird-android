
package com.fsck.k9.mail;

import android.database.Cursor;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.Utility;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.mime4j.field.address.NamedMailbox;
import org.apache.james.mime4j.field.address.parser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Address
{

    /**
     * Immutable empty {@link Address} array
     */
    private static final Address[] EMPTY_ADDRESS_ARRAY = new Address[0];
    private static Map<String,String> sContactsName = new ConcurrentHashMap<String, String>();
    private static final String NO_ENTRY = "";

    String mAddress;

    String mPersonal;

    public Address(String address, String personal)
    {
        this.mAddress = address;
        if ("".equals(personal))
        {
            personal = null;
        }
        if (personal!=null)
        {
            personal = personal.trim();
        }
        this.mPersonal = personal;
    }

    public Address(String address)
    {
        this.mAddress = address;
    }

    public String getAddress()
    {
        return mAddress;
    }

    public void setAddress(String address)
    {
        this.mAddress = address;
    }

    public String getPersonal()
    {
        return mPersonal;
    }

    public void setPersonal(String personal)
    {
        if ("".equals(personal))
        {
            personal = null;
        }
        if (personal!=null)
        {
            personal = personal.trim();
        }
        this.mPersonal = personal;
    }

    /**
     * Parse a comma separated list of email addresses in human readable format and return an
     * array of Address objects, RFC-822 encoded.
     *
     * @param addressList
     * @return An array of 0 or more Addresses.
     */
    public static Address[] parseUnencoded(String addressList)
    {
        List<Address> addresses = new ArrayList<Address>();
        if (addressList!=null
                && !"".equals(addressList))
        {
            Rfc822Token[] tokens =  Rfc822Tokenizer.tokenize(addressList);
            for (Rfc822Token token : tokens)
            {
                String address = token.getAddress();
                if (address!=null
                        && !"".equals(address))
                {
                    addresses.add(new Address(token.getAddress(), token.getName()));
                }
            }
        }
        return addresses.toArray(EMPTY_ADDRESS_ARRAY);
    }

    /**
     * Parse a comma separated list of addresses in RFC-822 format and return an
     * array of Address objects.
     *
     * @param addressList
     * @return An array of 0 or more Addresses.
     */
    public static Address[] parse(String addressList)
    {
        ArrayList<Address> addresses = new ArrayList<Address>();
        if (addressList == null
                && !"".equals(addressList))
        {
            return EMPTY_ADDRESS_ARRAY;
        }
        try
        {
            MailboxList parsedList = AddressList.parse(addressList).flatten();
            for (int i = 0, count = parsedList.size(); i < count; i++)
            {
                org.apache.james.mime4j.field.address.Address address = parsedList.get(i);
                if (address instanceof NamedMailbox)
                {
                    NamedMailbox namedMailbox = (NamedMailbox)address;
                    addresses.add(new Address(namedMailbox.getLocalPart() + "@"
                                              + namedMailbox.getDomain(), namedMailbox.getName()));
                }
                else if (address instanceof Mailbox)
                {
                    Mailbox mailbox = (Mailbox)address;
                    addresses.add(new Address(mailbox.getLocalPart() + "@" + mailbox.getDomain()));
                }
                else
                {
                    Log.e(K9.LOG_TAG, "Unknown address type from Mime4J: "
                          + address.getClass().toString());
                }

            }
        }
        catch (ParseException pe)
        {
        }
        return addresses.toArray(EMPTY_ADDRESS_ARRAY);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Address)
        {
            return getAddress().equals(((Address) o).getAddress());
        }
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return getAddress().hashCode();
    }

    @Override
    public String toString()
    {
        if (mPersonal != null)
        {
            return Utility.quoteString(mPersonal) + " <" + mAddress + ">";
        }
        else
        {
            return mAddress;
        }
    }

    public static String toString(Address[] addresses)
    {
        if (addresses == null)
        {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < addresses.length; i++)
        {
            sb.append(addresses[i].toString());
            if (i < addresses.length - 1)
            {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    public String toEncodedString()
    {
        if (mPersonal != null)
        {
            return EncoderUtil.encodeAddressDisplayName(mPersonal) + " <" + mAddress + ">";
        }
        else
        {
            return mAddress;
        }
    }

    public static String toEncodedString(Address[] addresses)
    {
        if (addresses == null)
        {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < addresses.length; i++)
        {
            sb.append(addresses[i].toEncodedString());
            if (i < addresses.length - 1)
            {
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
    public CharSequence toFriendly()
    {
        return toFriendly((Contacts)null);
    }

    public CharSequence toFriendly(Contacts contacts)
    {
        if (contacts != null)
        {
            String name = sContactsName.get(mAddress);
            if (name != null && name != NO_ENTRY)
            {
                return Html.fromHtml("<font color=\"Blue\">" + name + "</font>"); // TODO: use setSpan
            }
            if (name == null)
            {
                Cursor cursor = contacts.searchByAddress(mAddress);
                if (cursor != null)
                {
                    try
                    {
                        if (cursor.getCount() > 0)
                        {
                            cursor.moveToFirst();
                            name = contacts.getName(cursor);
                            sContactsName.put(mAddress, name);
                            return Html.fromHtml("<font color=\"Blue\">" + name + "</font>"); // TODO: use setSpan
                        }
                        else
                        {
                            sContactsName.put(mAddress, NO_ENTRY);
                        }
                    }
                    finally
                    {
                        Log.i(K9.LOG_TAG, "cursor closed");
                        // cursor.close(); // TODO: should close cursor.
                    }
                }
            }
        }

        if (mPersonal != null && mPersonal.length() > 0)
        {
            return mPersonal;
        }
        else
        {
            return mAddress;
        }
    }

    public static CharSequence toFriendly(Address[] addresses)
    {
        return toFriendly(addresses, null);
    }

    public static CharSequence toFriendly(Address[] addresses, Contacts contacts)
    {
        if (addresses == null)
        {
            return null;
        }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < addresses.length; i++)
        {
            sb.append(addresses[i].toFriendly(contacts));
            if (i < addresses.length - 1)
            {
                sb.append(',');
            }
        }
        return sb;
    }

    /**
     * Unpacks an address list previously packed with packAddressList()
     * @param list
     * @return
     */
    public static Address[] unpack(String addressList)
    {
        if (addressList == null)
        {
            return new Address[] { };
        }
        ArrayList<Address> addresses = new ArrayList<Address>();
        int length = addressList.length();
        int pairStartIndex = 0;
        int pairEndIndex = 0;
        int addressEndIndex = 0;
        while (pairStartIndex < length)
        {
            pairEndIndex = addressList.indexOf(",\u0000", pairStartIndex);
            if (pairEndIndex == -1)
            {
                pairEndIndex = length;
            }
            addressEndIndex = addressList.indexOf(";\u0000", pairStartIndex);
            String address = null;
            String personal = null;
            if (addressEndIndex == -1 || addressEndIndex > pairEndIndex)
            {
                address = addressList.substring(pairStartIndex, pairEndIndex);
            }
            else
            {
                address = addressList.substring(pairStartIndex, addressEndIndex);
                personal =addressList.substring(addressEndIndex + 2, pairEndIndex);
            }
            addresses.add(new Address(address, personal));
            pairStartIndex = pairEndIndex + 2;
        }
        return addresses.toArray(new Address[] { });
    }

    /**
     * Packs an address list into a String that is very quick to read
     * and parse. Packed lists can be unpacked with unpackAddressList()
     * The packed list is a "\u0000," seperated list of:
     * address\u0000;personal
     * @param list
     * @return
     */
    public static String pack(Address[] addresses)
    {
        if (addresses == null)
        {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0, count = addresses.length; i < count; i++)
        {
            Address address = addresses[i];
            sb.append(address.getAddress());
            String personal = address.getPersonal();
            if (personal != null)
            {
                sb.append(";\u0000");
                // Escape quotes in the address part on the way in
                personal.replaceAll("\"","\\\"");
                sb.append(personal);
            }
            if (i < count - 1)
            {
                sb.append(",\u0000");
            }
        }
        return sb.toString();
    }
}
