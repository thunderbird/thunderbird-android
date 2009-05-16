/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.commons.codec;

import java.util.Comparator;

/**
 * Strings are comparable, and this comparator allows 
 * you to configure it with an instance of a class
 * which implements StringEncoder.  This comparator
 * is used to sort Strings by an encoding scheme such
 * as Soundex, Metaphone, etc.  This class can come in
 * handy if one need to sort Strings by an encoded
 * form of a name such as Soundex.
 *
 * @author Apache Software Foundation
 * @version $Id: StringEncoderComparator.java,v 1.14 2004/06/21 23:24:17 ggregory Exp $
 */
public class StringEncoderComparator implements Comparator {

    /**
     * Internal encoder instance.
     */
    private StringEncoder stringEncoder;

    /**
     * Constructs a new instance.
     */
    public StringEncoderComparator() {
        // no init.
    }

    /**
     * Constructs a new instance with the given algorithm.
     * @param stringEncoder the StringEncoder used for comparisons.
     */
    public StringEncoderComparator(StringEncoder stringEncoder) {
        this.stringEncoder = stringEncoder;
    }

    /**
     * Compares two strings based not on the strings 
     * themselves, but on an encoding of the two 
     * strings using the StringEncoder this Comparator
     * was created with.
     * 
     * If an {@link EncoderException} is encountered, return <code>0</code>.
     * 
     * @param o1 the object to compare
     * @param o2 the object to compare to
     * @return the Comparable.compareTo() return code or 0 if an encoding error was caught.
     * @see Comparable
     */
    public int compare(Object o1, Object o2) {

        int compareCode = 0;

        try {
            Comparable s1 = (Comparable) ((Encoder) this.stringEncoder).encode(o1);
            Comparable s2 = (Comparable) ((Encoder) this.stringEncoder).encode(o2);
            compareCode = s1.compareTo(s2);
        } 
        catch (EncoderException ee) {
            compareCode = 0;
        }
        return compareCode;
    }

}
