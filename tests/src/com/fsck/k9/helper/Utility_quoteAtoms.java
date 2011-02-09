package com.fsck.k9.helper;

import junit.framework.TestCase;

public class Utility_quoteAtoms extends TestCase
{
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

    public void testQuote() {
        assertEquals("\"bob s. barker\"", quote("bob s. barker"));
        assertEquals("\":(\"", quote(":("));
    }

    private void noQuote(final String s) {
        assertEquals(s, Utility.quoteAtoms(s));
    }

    private String quote(final String s) {
        return Utility.quoteAtoms(s);
    }
}
