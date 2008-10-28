/*
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
 */
package org.apache.commons.io.filefilter;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.io.IOCase;

/**
 * Filters files based on the suffix (what the filename ends with).
 * This is used in retrieving all the files of a particular type.
 * <p>
 * For example, to retrieve and print all <code>*.java</code> files 
 * in the current directory:
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( new SuffixFileFilter(".java") );
 * for (int i = 0; i &lt; files.length; i++) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * @since Commons IO 1.0
 * @version $Revision: 606381 $ $Date: 2007-12-22 02:03:16 +0000 (Sat, 22 Dec 2007) $
 * 
 * @author Stephen Colebourne
 * @author Federico Barbieri
 * @author Serge Knystautas
 * @author Peter Donald
 */
public class SuffixFileFilter extends AbstractFileFilter implements Serializable {
    
    /** The filename suffixes to search for */
    private final String[] suffixes;

    /** Whether the comparison is case sensitive. */
    private final IOCase caseSensitivity;

    /**
     * Constructs a new Suffix file filter for a single extension.
     * 
     * @param suffix  the suffix to allow, must not be null
     * @throws IllegalArgumentException if the suffix is null
     */
    public SuffixFileFilter(String suffix) {
        this(suffix, IOCase.SENSITIVE);
    }

    /**
     * Constructs a new Suffix file filter for a single extension
     * specifying case-sensitivity.
     *
     * @param suffix  the suffix to allow, must not be null
     * @param caseSensitivity  how to handle case sensitivity, null means case-sensitive
     * @throws IllegalArgumentException if the suffix is null
     * @since Commons IO 1.4
     */
    public SuffixFileFilter(String suffix, IOCase caseSensitivity) {
        if (suffix == null) {
            throw new IllegalArgumentException("The suffix must not be null");
        }
        this.suffixes = new String[] {suffix};
        this.caseSensitivity = (caseSensitivity == null ? IOCase.SENSITIVE : caseSensitivity);
    }

    /**
     * Constructs a new Suffix file filter for an array of suffixs.
     * <p>
     * The array is not cloned, so could be changed after constructing the
     * instance. This would be inadvisable however.
     * 
     * @param suffixes  the suffixes to allow, must not be null
     * @throws IllegalArgumentException if the suffix array is null
     */
    public SuffixFileFilter(String[] suffixes) {
        this(suffixes, IOCase.SENSITIVE);
    }

    /**
     * Constructs a new Suffix file filter for an array of suffixs
     * specifying case-sensitivity.
     * <p>
     * The array is not cloned, so could be changed after constructing the
     * instance. This would be inadvisable however.
     * 
     * @param suffixes  the suffixes to allow, must not be null
     * @param caseSensitivity  how to handle case sensitivity, null means case-sensitive
     * @throws IllegalArgumentException if the suffix array is null
     * @since Commons IO 1.4
     */
    public SuffixFileFilter(String[] suffixes, IOCase caseSensitivity) {
        if (suffixes == null) {
            throw new IllegalArgumentException("The array of suffixes must not be null");
        }
        this.suffixes = suffixes;
        this.caseSensitivity = (caseSensitivity == null ? IOCase.SENSITIVE : caseSensitivity);
    }

    /**
     * Constructs a new Suffix file filter for a list of suffixes.
     * 
     * @param suffixes  the suffixes to allow, must not be null
     * @throws IllegalArgumentException if the suffix list is null
     * @throws ClassCastException if the list does not contain Strings
     */
    public SuffixFileFilter(List suffixes) {
        this(suffixes, IOCase.SENSITIVE);
    }

    /**
     * Constructs a new Suffix file filter for a list of suffixes
     * specifying case-sensitivity.
     * 
     * @param suffixes  the suffixes to allow, must not be null
     * @param caseSensitivity  how to handle case sensitivity, null means case-sensitive
     * @throws IllegalArgumentException if the suffix list is null
     * @throws ClassCastException if the list does not contain Strings
     * @since Commons IO 1.4
     */
    public SuffixFileFilter(List suffixes, IOCase caseSensitivity) {
        if (suffixes == null) {
            throw new IllegalArgumentException("The list of suffixes must not be null");
        }
        this.suffixes = (String[]) suffixes.toArray(new String[suffixes.size()]);
        this.caseSensitivity = (caseSensitivity == null ? IOCase.SENSITIVE : caseSensitivity);
    }

    /**
     * Checks to see if the filename ends with the suffix.
     * 
     * @param file  the File to check
     * @return true if the filename ends with one of our suffixes
     */
    public boolean accept(File file) {
        String name = file.getName();
        for (int i = 0; i < this.suffixes.length; i++) {
            if (caseSensitivity.checkEndsWith(name, suffixes[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks to see if the filename ends with the suffix.
     * 
     * @param file  the File directory
     * @param name  the filename
     * @return true if the filename ends with one of our suffixes
     */
    public boolean accept(File file, String name) {
        for (int i = 0; i < this.suffixes.length; i++) {
            if (caseSensitivity.checkEndsWith(name, suffixes[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provide a String representaion of this file filter.
     *
     * @return a String representaion
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append("(");
        if (suffixes != null) {
            for (int i = 0; i < suffixes.length; i++) {
                if (i > 0) {
                    buffer.append(",");
                }
                buffer.append(suffixes[i]);
            }
        }
        buffer.append(")");
        return buffer.toString();
    }
    
}
