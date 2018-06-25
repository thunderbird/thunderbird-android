package com.fsck.k9.autocrypt;


import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


@SuppressWarnings("WeakerAccess")
public class AutocryptHeaderTest {
    static final HashMap<String, String> PARAMETERS = new HashMap<>();
    static final String ADDR = "addr";
    static final byte[] KEY_DATA = ("theseare120charactersxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx").getBytes();
    static final boolean IS_PREFER_ENCRYPT_MUTUAL = true;


    @Test
    public void toRawHeaderString_returnsExpected() throws Exception {
        AutocryptHeader autocryptHeader = new AutocryptHeader(PARAMETERS, ADDR, KEY_DATA, IS_PREFER_ENCRYPT_MUTUAL);
        String autocryptHeaderString = autocryptHeader.toRawHeaderString();

        String expected = "Autocrypt: addr=addr; prefer-encrypt=mutual; keydata=\r\n" +
                " dGhlc2VhcmUxMjBjaGFyYWN0ZXJzeHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4\r\n" +
                " eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4\r\n" +
                " eHh4eHh4";
        assertEquals(expected, autocryptHeaderString);
    }

    @Test
    public void gossip_toRawHeaderString_returnsExpected() throws Exception {
        AutocryptGossipHeader autocryptHeader = new AutocryptGossipHeader(ADDR, KEY_DATA);
        String autocryptHeaderString = autocryptHeader.toRawHeaderString();

        String expected = "Autocrypt-Gossip: addr=addr; keydata=\r\n" +
                " dGhlc2VhcmUxMjBjaGFyYWN0ZXJzeHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4\r\n" +
                " eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4\r\n" +
                " eHh4eHh4";
        assertEquals(expected, autocryptHeaderString);
    }
}
