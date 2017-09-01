package com.fsck.k9.autocrypt;


import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


@SuppressWarnings("WeakerAccess")
public class AutocryptHeaderTest {
    static final HashMap<String, String> PARAMETERS = new HashMap<>();
    static final String ADDR = "addr";
    static final String ADDR_LONG = "veryveryverylongaddressthatspansmorethanalinelengthintheheader";
    static final byte[] KEY_DATA = ("theseare120charactersxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx").getBytes();
    static final byte[] KEY_DATA_SHORT = ("theseare15chars").getBytes();
    static final boolean IS_PREFER_ENCRYPT_MUTUAL = true;


    @Test
    public void toRawHeaderString_returnsExpected() throws Exception {
        AutocryptHeader autocryptHeader = new AutocryptHeader(PARAMETERS, ADDR, KEY_DATA, IS_PREFER_ENCRYPT_MUTUAL);
        String autocryptHeaderString = autocryptHeader.toRawHeaderString();

        String expected = "Autocrypt: addr=addr; prefer-encrypt=mutual; keydata=dGhlc2VhcmUxMjBjaGFyYWN\r\n" +
                " 0ZXJzeHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh\r\n" +
                " 4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4";
        assertEquals(expected, autocryptHeaderString);
    }

    @Test
    public void toRawHeaderString_withLongAddress_returnsExpected() throws Exception {
        AutocryptHeader autocryptHeader = new AutocryptHeader(PARAMETERS,
                ADDR_LONG, KEY_DATA, IS_PREFER_ENCRYPT_MUTUAL);
        String autocryptHeaderString = autocryptHeader.toRawHeaderString();

        String expected = "Autocrypt: addr=veryveryverylongaddressthatspansmorethanalinelengthintheheader; prefer-encrypt=mutual; keydata=\r\n" +
                " dGhlc2VhcmUxMjBjaGFyYWN0ZXJzeHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4\r\n" +
                " eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4\r\n" +
                " eHh4eHh4";
        assertEquals(expected, autocryptHeaderString);
    }

    @Test
    public void toRawHeaderString_withShortData_returnsExpected() throws Exception {
        AutocryptHeader autocryptHeader = new AutocryptHeader(PARAMETERS,
                ADDR, KEY_DATA_SHORT, IS_PREFER_ENCRYPT_MUTUAL);
        String autocryptHeaderString = autocryptHeader.toRawHeaderString();

        String expected = "Autocrypt: addr=addr; prefer-encrypt=mutual; keydata=dGhlc2VhcmUxNWNoYXJz";
        assertEquals(expected, autocryptHeaderString);
    }
}
