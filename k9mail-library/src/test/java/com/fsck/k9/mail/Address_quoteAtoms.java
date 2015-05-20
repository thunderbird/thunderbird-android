package com.fsck.k9.mail;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class Address_quoteAtoms {
    @Test
    public void testNoQuote() {
        // Alpha
        noQuote("a");
        noQuote("aa");
        noQuote("aaa aaa");

        // Numeric
        noQuote("1");
        noQuote("12");
        noQuote("123 456");

        // Alpha Numeric
        noQuote("abc 123");

        // Specials
        noQuote("!");
        noQuote("#");
        noQuote("$");
        noQuote("%");
        noQuote("&");
        noQuote("'");
        noQuote("*");
        noQuote("+");
        noQuote("-");
        noQuote("/");
        noQuote("=");
        noQuote("?");
        noQuote("^");
        noQuote("_");
        noQuote("`");
        noQuote("{");
        noQuote("|");
        noQuote("}");
        noQuote("~");

        // Combos
        noQuote("bob barker! #1!");
        noQuote("!");
        noQuote("#&#!");
        noQuote("{|}");
        noQuote("'-=+=-'");
    }

    @Test
    public void testQuote() {
        assertEquals("\"bob s. barker\"", quote("bob s. barker"));
        assertEquals("\":(\"", quote(":("));
    }

    private void noQuote(final String s) {
        assertEquals(s, Address.quoteAtoms(s));
    }

    private String quote(final String s) {
        return Address.quoteAtoms(s);
    }
}
