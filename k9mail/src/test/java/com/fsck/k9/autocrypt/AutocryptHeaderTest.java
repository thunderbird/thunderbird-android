package com.fsck.k9.autocrypt;


import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


@SuppressWarnings("WeakerAccess")
public class AutocryptHeaderTest {
    static final HashMap<String, String> PARAMETERS = new HashMap<>();
    static final String ADDR = "addr";
    static final byte[] KEY_DATA = ("theseare120charactersxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx").getBytes();
    static final boolean IS_PREFER_ENCRYPT_MUTUAL = true;


    private AutocryptHeader autocryptHeader;


    @Before
    public void setUp() throws Exception {
        autocryptHeader = new AutocryptHeader(PARAMETERS, ADDR, KEY_DATA, IS_PREFER_ENCRYPT_MUTUAL);
    }


    @Test
    public void toRawHeaderString_returnsExpected() throws Exception {
        String autocryptHeaderString = autocryptHeader.toRawHeaderString();

        String expected = "Autocrypt: addr=addr;prefer-encrypt=mutual;keydata=dGhlc2VhcmUxMjBjaGFyYWN0Z\n" +
                " XJzeHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4e\n" +
                " Hh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4";
        assertEquals(expected, autocryptHeaderString);
    }
}
