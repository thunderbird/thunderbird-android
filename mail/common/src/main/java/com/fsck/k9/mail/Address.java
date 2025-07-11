
package com.fsck.k9.mail;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.thunderbird.core.logging.legacy.Log;
import com.fsck.k9.mail.helper.Rfc822Token;
import com.fsck.k9.mail.helper.Rfc822Tokenizer;
import com.fsck.k9.mail.helper.TextUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.field.address.DefaultAddressParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

public class Address implements Serializable {
    private static final Pattern ATOM = Pattern.compile("^(?:[a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]|\\s)+$");

    /**
     * Immutable empty {@link Address} array
     */
    private static final Address[] EMPTY_ADDRESS_ARRAY = new Address[0];

    @NotNull
    private String mAddress;

    private String mPersonal;

    public Address(Address address) {
        mAddress = address.mAddress;
        mPersonal = address.mPersonal;
    }

    public Address(String address, String personal) {
        this(address, personal, true);
    }

    public Address(String address) {
        this(address, null, true);
    }

    private Address(String address, String personal, boolean parse) {
        if (address == null) {
            throw new IllegalArgumentException("address");
        }
        if (parse) {
            Rfc822Token[] tokens =  Rfc822Tokenizer.tokenize(address);
            if (tokens.length > 0) {
                Rfc822Token token = tokens[0];
                if (token.getAddress() == null) {
                    throw new IllegalArgumentException("token.getAddress()");
                }
                mAddress = token.getAddress();
                String name = token.getName();
                if (!TextUtils.isEmpty(name)) {
                    /*
                     * Don't use the "personal" argument if "address" is of the form:
                     * James Bond <james.bond@mi6.uk>
                     *
                     * See issue 2920
                     */
                    mPersonal = name;
                } else {
                    mPersonal = (personal == null) ? null : personal.trim();
                }
            } else {
                Log.e("Invalid address: %s", address);
            }
        } else {
            mAddress = address;
            mPersonal = personal;
        }
    }

    public String getAddress() {
        return mAddress;
    }

    public String getHostname() {
        if (mAddress == null) {
            return null;
        }

        int hostIdx = mAddress.lastIndexOf("@");

        if (hostIdx == -1) {
            return null;
        }

        return mAddress.substring(hostIdx + 1);
    }

    public String getPersonal() {
        return mPersonal;
    }

    /**
     * Parse a comma separated list of email addresses in human readable format and return an
     * array of Address objects, RFC-822 encoded.
     *
     * @param addressList
     * @return An array of 0 or more Addresses.
     */
    public static Address[] parseUnencoded(String addressList) {
        List<Address> addresses = new ArrayList<>();
        if (!TextUtils.isEmpty(addressList)) {
            Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(addressList);
            for (Rfc822Token token : tokens) {
                String address = token.getAddress();
                if (!TextUtils.isEmpty(address)) {
                    String name = TextUtils.isEmpty(token.getName()) ? null : token.getName();
                    addresses.add(new Address(token.getAddress(), name, false));
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
    public static Address[] parse(String addressList) {
        if (TextUtils.isEmpty(addressList)) {
            return EMPTY_ADDRESS_ARRAY;
        }
        List<Address> addresses = new ArrayList<>();
        try {
            MailboxList parsedList =  DefaultAddressParser.DEFAULT.parseAddressList(addressList, DecodeMonitor.SILENT).flatten();

            for (int i = 0, count = parsedList.size(); i < count; i++) {
                Mailbox mailbox = parsedList.get(i);
                addresses.add(new Address(mailbox.getLocalPart() + "@" + mailbox.getDomain(), mailbox.getName(), false));
            }
        } catch (MimeException pe) {
            Log.e(pe, "MimeException in Address.parse()");
            // broken addresses are never added to the resulting array
        }
        return addresses.toArray(EMPTY_ADDRESS_ARRAY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Address address = (Address) o;

        if (mAddress != null ? !mAddress.equals(address.mAddress) : address.mAddress != null) {
            return false;
        }

        return mPersonal != null ? mPersonal.equals(address.mPersonal) : address.mPersonal == null;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (mAddress != null) {
            hash += mAddress.hashCode();
        }
        if (mPersonal != null) {
            hash += 3 * mPersonal.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        if (!TextUtils.isEmpty(mPersonal)) {
            return quoteAtoms(mPersonal) + " <" + mAddress + ">";
        } else {
            return mAddress;
        }
    }

    public static String toString(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        return TextUtils.join(", ", addresses);
    }

    public String toEncodedString() {
        if (!TextUtils.isEmpty(mPersonal)) {
            return EncoderUtil.encodeAddressDisplayName(mPersonal) + " <" + mAddress + ">";
        } else {
            return mAddress;
        }
    }

    /**
     * Unpacks an address list previously packed with packAddressList()
     * @param addressList Packed address list.
     * @return Unpacked list.
     */
    public static Address[] unpack(String addressList) {
        if (addressList == null) {
            return new Address[] { };
        }
        List<Address> addresses = new ArrayList<>();
        int length = addressList.length();
        int pairStartIndex = 0;
        int pairEndIndex = 0;
        int addressEndIndex = 0;
        while (pairStartIndex < length) {
            pairEndIndex = addressList.indexOf(",\u0001", pairStartIndex);
            if (pairEndIndex == -1) {
                pairEndIndex = length;
            }
            addressEndIndex = addressList.indexOf(";\u0001", pairStartIndex);
            String address = null;
            String personal = null;
            if (addressEndIndex == -1 || addressEndIndex > pairEndIndex) {
                address = addressList.substring(pairStartIndex, pairEndIndex);
            } else {
                address = addressList.substring(pairStartIndex, addressEndIndex);
                personal = addressList.substring(addressEndIndex + 2, pairEndIndex);
            }
            addresses.add(new Address(address, personal, false));
            pairStartIndex = pairEndIndex + 2;
        }
        return addresses.toArray(new Address[addresses.size()]);
    }

    /**
     * Packs an address list into a String that is very quick to read
     * and parse. Packed lists can be unpacked with unpackAddressList()
     * The packed list is a ",\u0001" separated list of:
     * address;\u0001personal
     * @param addresses Array of addresses to pack.
     * @return Packed addresses.
     */
    public static String pack(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, count = addresses.length; i < count; i++) {
            Address address = addresses[i];
            sb.append(address.getAddress());
            String personal = address.getPersonal();
            if (personal != null) {
                sb.append(";\u0001");
                // Escape quotes in the address part on the way in
                personal = personal.replaceAll("\"", "\\\"");
                sb.append(personal);
            }
            if (i < count - 1) {
                sb.append(",\u0001");
            }
        }
        return sb.toString();
    }

    /**
     * Quote a string, if necessary, based upon the definition of an "atom," as defined by RFC2822
     * (http://tools.ietf.org/html/rfc2822#section-3.2.4). Strings that consist purely of atoms are
     * left unquoted; anything else is returned as a quoted string.
     * @param text String to quote.
     * @return Possibly quoted string.
     */
    public static String quoteAtoms(final String text) {
        if (ATOM.matcher(text).matches()) {
            return text;
        } else {
            return quoteString(text);
        }
    }

    /**
     * Ensures that the given string starts and ends with the double quote character.
     * The string is not modified in any way except to add the double quote character to start
     * and end if it's not already there.
     * sample -> "sample"
     * "sample" -> "sample"
     * ""sample"" -> ""sample""
     * "sample"" -> "sample"
     * sa"mp"le -> "sa"mp"le"
     * "sa"mp"le" -> "sa"mp"le"
     * (empty string) -> ""
     * " -> """
     * @param s
     * @return
     */
    @VisibleForTesting
    static String quoteString(String s) {
        if (s == null) {
            return null;
        }
        if (!s.matches("^\".*\"$")) {
            return "\"" + s + "\"";
        } else {
            return s;
        }
    }

    /**
     *  Returns true if either the localpart or the domain of this
     *  address contains any non-ASCII characters, and false if all
     *  characters used are within ASCII.
     *
     *  Note that this returns false for an address such as "Naïve
     *  Assumption &lt;naive.assumption@example.com&gt;", because both
     *  localpart and domain are all-ASCII. There's an ï there, but
     *  it's not in either localpart or domain.
     */
    public boolean needsUnicode() {
        if (mAddress == null)
            return false;
        int i = mAddress.length()-1;
        while (i >= 0 && mAddress.charAt(i) < 128)
            i--;
        return i >= 0;
    }
}
