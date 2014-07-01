package com.fsck.k9.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

import com.fsck.k9.mail.filter.Base64;

public final class StringUtils {

    public static boolean isNullOrEmpty(String string){
        return string == null || string.isEmpty();
    }

    public static boolean containsAny(String haystack, String[] needles) {
        if (haystack == null) {
            return false;
        }

        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }

        return false;
    }

	/**
	 * Combines the given array of Objects into a single String using
	 * each Object's toString() method and the separator character
	 * between each part.
	 *
	 * @param parts
	 * @param separator
	 * @return new String
	 */
	public static String combine(Object[] parts, char separator) {
	    if (parts == null) {
	        return null;
	    }
	    return TextUtils.join(String.valueOf(separator), parts);
	}

	public static String base64Decode(String encoded) {
	    if (encoded == null) {
	        return null;
	    }
	    byte[] decoded = new Base64().decode(encoded.getBytes());
	    return new String(decoded);
	}

	public static String base64Encode(String s) {
	    if (s == null) {
	        return s;
	    }
	    byte[] encoded = new Base64().encode(s.getBytes());
	    return new String(encoded);
	}

	/**
	 * Ensures that the given string starts and ends with the double quote character.
	 * The string is not modified in any way except to add the
	 * double quote character to start and end if it's not already there.
	 * sample -> "sample"
	 * "sample" -> "sample"
	 * ""sample"" -> "sample"
	 * "sample"" -> "sample"
	 * sa"mp"le -> "sa"mp"le"
	 * "sa"mp"le" -> "sa"mp"le"
	 * (empty string) -> ""
	 * " -> """
	 * @param s
	 * @return
	 */
	public static String quote(String s) {
	    if (s == null) {
	        return null;
	    }
	    if (s.length() < 2 || s.charAt(0) != '"' || s.charAt(s.length() - 1) != '"') {
	        return "\"" + s + "\"";
	    } else {
	        return s;
	    }
	}

	private static final Pattern ATOM = Pattern.compile("^(?:[a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]|\\s)+$");

	/**
	 * Quote a string, if necessary, based upon the definition of an "atom," as defined by RFC2822
	 * (http://tools.ietf.org/html/rfc2822#section-3.2.4). Strings that consist purely of atoms are
	 * left unquoted; anything else is returned as a quoted string.
	 * @param text String to quote.
	 * @return Possibly quoted string.
	 */
	public static String quoteIfNotAtom(final String text) {
	    if (ATOM.matcher(text).matches()) {
	        return text;
	    } else {
	        return quote(text);
	    }
	}

	/**
	 * A fast version of  URLDecoder.decode() that works only with UTF-8 and does only two
	 * allocations. This version is around 3x as fast as the standard one and I'm using it
	 * hundreds of times in places that slow down the UI, so it helps.
	 */
	public static String fastUrlDecode(String s) {
	    try {
	        byte[] bytes = s.getBytes("UTF-8");
	        byte ch;
	        int length = 0;
	        for (int i = 0, count = bytes.length; i < count; i++) {
	            ch = bytes[i];
	            if (ch == '%') {
	                int h = (bytes[i + 1] - '0');
	                int l = (bytes[i + 2] - '0');
	                if (h > 9) {
	                    h -= 7;
	                }
	                if (l > 9) {
	                    l -= 7;
	                }
	                bytes[length] = (byte)((h << 4) | l);
	                i += 2;
	            } else if (ch == '+') {
	                bytes[length] = ' ';
	            } else {
	                bytes[length] = bytes[i];
	            }
	            length++;
	        }
	        return new String(bytes, 0, length, "UTF-8");
	    } catch (UnsupportedEncodingException uee) {
	        return null;
	    }
	}

	@SuppressLint("NewApi")
	public static String[] copyOf(String[] original, int newLength) {
	    if (Build.VERSION.SDK_INT >= 9) {
	        return Arrays.copyOf(original, newLength);
	    }
	
	    String[] newArray = new String[newLength];
	    int copyLength = (original.length >= newLength) ? newLength : original.length;
	    System.arraycopy(original, 0, newArray, 0, copyLength);
	
	    return newArray;
	}

	/**
	 * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
	 *
	 * <p>Leading spaces on a new line are stripped.
	 * Trailing spaces are not stripped.</p>
	 *
	 * <pre>
	 * WordUtils.wrap(null, *, *, *) = null
	 * WordUtils.wrap("", *, *, *) = ""
	 * </pre>
	 *
	 * This is from the Apache Commons Lang library.
	 * http://svn.apache.org/viewvc/commons/proper/lang
	 *   /trunk/src/main/java/org/apache/commons/lang3/text/WordUtils.java
	 * SVN Revision 925967, Mon Mar 22 06:16:49 2010 UTC
	 *
	 * Licensed to the Apache Software Foundation (ASF) under one or more
	 * contributor license agreements.  See the NOTICE file distributed with
	 * this work for additional information regarding copyright ownership.
	 * The ASF licenses this file to You under the Apache License, Version 2.0
	 * (the "License"); you may not use this file except in compliance with
	 * the License.  You may obtain a copy of the License at
	 *
	 *      http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 *
	 * @param str  the String to be word wrapped, may be null
	 * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
	 * @param newLineStr  the string to insert for a new line,
	 *  <code>null</code> uses the system property line separator
	 * @param wrapLongWords  true if long words (such as URLs) should be wrapped
	 * @return a line with newlines inserted, <code>null</code> if null input
	 */
	private static String wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
	    if (str == null) {
	        return null;
	    }
	    if (newLineStr == null) {
	        newLineStr = "\r\n";
	    }
	    if (wrapLength < 1) {
	        wrapLength = 1;
	    }
	    int inputLineLength = str.length();
	    int offset = 0;
	    StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);
	
	    while ((inputLineLength - offset) > wrapLength) {
	        if (str.charAt(offset) == ' ') {
	            offset++;
	            continue;
	        }
	        int spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset);
	
	        if (spaceToWrapAt >= offset) {
	            // normal case
	            wrappedLine.append(str.substring(offset, spaceToWrapAt));
	            wrappedLine.append(newLineStr);
	            offset = spaceToWrapAt + 1;
	        } else {
	            // really long word or URL
	            if (wrapLongWords) {
	                // wrap really long word one line at a time
	                wrappedLine.append(str.substring(offset, wrapLength + offset));
	                wrappedLine.append(newLineStr);
	                offset += wrapLength;
	            } else {
	                // do not wrap really long word, just extend beyond limit
	                spaceToWrapAt = str.indexOf(' ', wrapLength + offset);
	                if (spaceToWrapAt >= 0) {
	                    wrappedLine.append(str.substring(offset, spaceToWrapAt));
	                    wrappedLine.append(newLineStr);
	                    offset = spaceToWrapAt + 1;
	                } else {
	                    wrappedLine.append(str.substring(offset));
	                    offset = inputLineLength;
	                }
	            }
	        }
	    }
	
	    // Whatever is left in line is short enough to just pass through
	    wrappedLine.append(str.substring(offset));
	
	    return wrappedLine.toString();
	}

    public static String wrap(String str, int wrapLength) {

    	StringBuilder result = new StringBuilder();
    	BufferedReader reader = new BufferedReader(new StringReader(str));
    	
    	try {
    		String line;
    		while ((line = reader.readLine()) != null) {
    			result.append(wrap(line, wrapLength, null, false));
    			result.append("\r\n");
    		}
    	} catch (IOException e) {
    		// cannot happen
    	}
        
        return result.toString();
    }

    /**
     * Html-encode the string.
     * @param s the string to be encoded
     * @return the encoded string
     */
    public static CharSequence htmlEncode(CharSequence s) {
        StringBuilder sb = new StringBuilder(s.length() << 1);
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            switch (c) {
            case '<':
                sb.append("&lt;"); //$NON-NLS-1$
                break;
            case '>':
                sb.append("&gt;"); //$NON-NLS-1$
                break;
            case '&':
                sb.append("&amp;"); //$NON-NLS-1$
                break;
            case '\'':
                //http://www.w3.org/TR/xhtml1
                // The named character reference &apos; (the apostrophe, U+0027) was introduced in
                // XML 1.0 but does not appear in HTML. Authors should therefore use &#39; instead
                // of &apos; to work as expected in HTML 4 user agents.
                sb.append("&#39;"); //$NON-NLS-1$
                break;
            case '"':
                sb.append("&quot;"); //$NON-NLS-1$
                break;
            default:
                sb.append(c);
            }
        }
        return sb;
    }
}
