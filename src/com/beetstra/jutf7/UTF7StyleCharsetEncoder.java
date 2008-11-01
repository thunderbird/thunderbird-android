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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * <p>
 * The CharsetEncoder used to encode both variants of the UTF-7 charset and the
 * modified-UTF-7 charset.
 * </p>
 * <p>
 * <strong>Please note this class does not behave strictly according to the
 * specification in Sun Java VMs before 1.6.</strong> This is done to get around
 * a bug in the implementation of
 * {@link java.nio.charset.CharsetEncoder#encode(CharBuffer)}. Unfortunately,
 * that method cannot be overridden.
 * </p>
 * 
 * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6221056">JDK
 *      bug 6221056< /a>
 * @author Jaap Beetstra
 */
class UTF7StyleCharsetEncoder extends CharsetEncoder {
    private static final float AVG_BYTES_PER_CHAR = 1.5f;
    private static final float MAX_BYTES_PER_CHAR = 5.0f;
    private final UTF7StyleCharset cs;
    private final Base64Util base64;
    private final byte shift;
    private final byte unshift;
    private final boolean strict;
    private boolean base64mode;
    private int bitsToOutput;
    private int sextet;
    static boolean useUglyHackToForceCallToFlushInJava5;
    static {
        String version = System.getProperty("java.specification.version");
        String vendor = System.getProperty("java.vm.vendor");
        useUglyHackToForceCallToFlushInJava5 = "1.4".equals(version) || "1.5".equals(version);
        useUglyHackToForceCallToFlushInJava5 &= "Sun Microsystems Inc.".equals(vendor);
    }

    UTF7StyleCharsetEncoder(UTF7StyleCharset cs, Base64Util base64, boolean strict) {
        super(cs, AVG_BYTES_PER_CHAR, MAX_BYTES_PER_CHAR);
        this.cs = cs;
        this.base64 = base64;
        this.strict = strict;
        this.shift = cs.shift();
        this.unshift = cs.unshift();
    }

    /*
     * (non-Javadoc)
     * @see java.nio.charset.CharsetEncoder#implReset()
     */
    protected void implReset() {
        base64mode = false;
        sextet = 0;
        bitsToOutput = 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method might return <code>CoderResult.OVERFLOW</code> (as
     * is required by the specification) if insufficient space is available in
     * the output buffer. However, calling it again on JDKs before Java 6
     * triggers a bug in
     * {@link java.nio.charset.CharsetEncoder#flush(ByteBuffer)} causing it to
     * throw an IllegalStateException (the buggy method is <code>final</code>,
     * thus cannot be overridden).
     * </p>
     * 
     * @see <a
     *      href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6227608">
     *      JDK bug 6227608< /a>
     * @param out The output byte buffer
     * @return A coder-result object describing the reason for termination
     */
    protected CoderResult implFlush(ByteBuffer out) {
        if (base64mode) {
            if (out.remaining() < 2)
                return CoderResult.OVERFLOW;
            if (bitsToOutput != 0)
                out.put(base64.getChar(sextet));
            out.put(unshift);
        }
        return CoderResult.UNDERFLOW;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method might return <code>CoderResult.OVERFLOW</code>,
     * even though there is sufficient space available in the output buffer.
     * This is done to force the broken implementation of
     * {@link java.nio.charset.CharsetEncoder#encode(CharBuffer)} to call flush
     * (the buggy method is <code>final</code>, thus cannot be overridden).
     * </p>
     * <p>
     * However, String.getBytes() fails if CoderResult.OVERFLOW is returned,
     * since this assumes it always allocates sufficient bytes (maxBytesPerChar
     * * nr_of_chars). Thus, as an extra check, the size of the input buffer is
     * compared against the size of the output buffer. A static variable is used
     * to indicate if a broken java version is used.
     * </p>
     * <p>
     * It is not possible to directly write the last few bytes, since more bytes
     * might be waiting to be encoded then those available in the input buffer.
     * </p>
     * 
     * @see <a
     *      href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6221056">
     *      JDK bug 6221056< /a>
     * @param in The input character buffer
     * @param out The output byte buffer
     * @return A coder-result object describing the reason for termination
     */
    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        while (in.hasRemaining()) {
            if (out.remaining() < 4)
                return CoderResult.OVERFLOW;
            char ch = in.get();
            if (cs.canEncodeDirectly(ch)) {
                unshift(out, ch);
                out.put((byte)ch);
            } else if (!base64mode && ch == shift) {
                out.put(shift);
                out.put(unshift);
            } else
                encodeBase64(ch, out);
        }
        /*
         * <HACK type="ugly"> These lines are required to trick JDK 1.5 and
         * earlier into flushing when using Charset.encode(String),
         * Charset.encode(CharBuffer) or CharsetEncoder.encode(CharBuffer)
         * Without them, the last few bytes may be missing.
         */
        if (base64mode && useUglyHackToForceCallToFlushInJava5
                && out.limit() != MAX_BYTES_PER_CHAR * in.limit())
            return CoderResult.OVERFLOW;
        /* </HACK> */
        return CoderResult.UNDERFLOW;
    }

    /**
     * <p>
     * Writes the bytes necessary to leave <i>base 64 mode</i>. This might
     * include an unshift character.
     * </p>
     * 
     * @param out
     * @param ch
     */
    private void unshift(ByteBuffer out, char ch) {
        if (!base64mode)
            return;
        if (bitsToOutput != 0)
            out.put(base64.getChar(sextet));
        if (base64.contains(ch) || ch == unshift || strict)
            out.put(unshift);
        base64mode = false;
        sextet = 0;
        bitsToOutput = 0;
    }

    /**
     * <p>
     * Writes the bytes necessary to encode a character in <i>base 64 mode</i>.
     * All bytes which are fully determined will be written. The fields
     * <code>bitsToOutput</code> and <code>sextet</code> are used to remember
     * the bytes not yet fully determined.
     * </p>
     * 
     * @param out
     * @param ch
     */
    private void encodeBase64(char ch, ByteBuffer out) {
        if (!base64mode)
            out.put(shift);
        base64mode = true;
        bitsToOutput += 16;
        while (bitsToOutput >= 6) {
            bitsToOutput -= 6;
            sextet += (ch >> bitsToOutput);
            sextet &= 0x3F;
            out.put(base64.getChar(sextet));
            sextet = 0;
        }
        sextet = (ch << (6 - bitsToOutput)) & 0x3F;
    }
}
