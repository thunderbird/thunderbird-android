/* ====================================================================
 * Copyright (c) 2006 J.T. Beetstra
 *
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to 
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * ====================================================================
 */

package com.beetstra.jutf7;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Abstract base class for UTF-7 style encoding and decoding.
 * </p>
 * 
 * @author Jaap Beetstra
 */
abstract class UTF7StyleCharset extends Charset {
    private static final List CONTAINED = Arrays.asList(new String[] {
            "US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE"
    });
    final boolean strict;
    Base64Util base64;

    /**
     * <p>
     * Besides the name and aliases, two additional parameters are required.
     * First the base 64 alphabet used; in modified UTF-7 a slightly different
     * alphabet is used. Additionally, it should be specified if encoders and
     * decoders should be strict about the interpretation of malformed encoded
     * sequences. This is used since modified UTF-7 specifically disallows some
     * constructs which are allowed (or not specifically disallowed) in UTF-7
     * (RFC 2152).
     * </p>
     * 
     * @param canonicalName The name as defined in java.nio.charset.Charset
     * @param aliases The aliases as defined in java.nio.charset.Charset
     * @param alphabet The base 64 alphabet used
     * @param strict True if strict handling of sequences is requested
     */
    protected UTF7StyleCharset(String canonicalName, String[] aliases, String alphabet,
            boolean strict) {
        super(canonicalName, aliases);
        this.base64 = new Base64Util(alphabet);
        this.strict = strict;
    }

    /*
     * (non-Javadoc)
     * @see java.nio.charset.Charset#contains(java.nio.charset.Charset)
     */
    public boolean contains(final Charset cs) {
        return CONTAINED.contains(cs.name());
    }

    /*
     * (non-Javadoc)
     * @see java.nio.charset.Charset#newDecoder()
     */
    public CharsetDecoder newDecoder() {
        return new UTF7StyleCharsetDecoder(this, base64, strict);
    }

    /*
     * (non-Javadoc)
     * @see java.nio.charset.Charset#newEncoder()
     */
    public CharsetEncoder newEncoder() {
        return new UTF7StyleCharsetEncoder(this, base64, strict);
    }

    /**
     * Tells if a character can be encoded using simple (US-ASCII) encoding or
     * requires base 64 encoding.
     * 
     * @param ch The character
     * @return True if the character can be encoded directly, false otherwise
     */
    abstract boolean canEncodeDirectly(char ch);

    /**
     * Returns character used to switch to base 64 encoding.
     * 
     * @return The shift character
     */
    abstract byte shift();

    /**
     * Returns character used to switch from base 64 encoding to simple
     * encoding.
     * 
     * @return The unshift character
     */
    abstract byte unshift();
}
