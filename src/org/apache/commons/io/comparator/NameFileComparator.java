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
package org.apache.commons.io.comparator;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.io.IOCase;

/**
 * Compare the <b>names</b> of two files for order (see {@link File#getName()}).
 * <p>
 * This comparator can be used to sort lists or arrays of files
 * by their name either in a case-sensitive, case-insensitive or
 * system dependant case sensitive way. A number of singleton instances
 * are provided for the various case sensitivity options (using {@link IOCase})
 * and the reverse of those options.
 * <p>
 * Example of a <i>case-sensitive</i> file name sort using the
 * {@link #NAME_COMPARATOR} singleton instance:
 * <pre>
 *       List&lt;File&gt; list = ...
 *       Collections.sort(list, NameFileComparator.NAME_COMPARATOR);
 * </pre>
 * <p>
 * Example of a <i>reverse case-insensitive</i> file name sort using the
 * {@link #NAME_INSENSITIVE_REVERSE} singleton instance:
 * <pre>
 *       File[] array = ...
 *       Arrays.sort(array, NameFileComparator.NAME_INSENSITIVE_REVERSE);
 * </pre>
 * <p>
 *
 * @version $Revision: 609243 $ $Date: 2008-01-06 00:30:42 +0000 (Sun, 06 Jan 2008) $
 * @since Commons IO 1.4
 */
public class NameFileComparator implements Comparator, Serializable {

    /** Case-sensitive name comparator instance (see {@link IOCase#SENSITIVE}) */
    public static final Comparator NAME_COMPARATOR = new NameFileComparator();

    /** Reverse case-sensitive name comparator instance (see {@link IOCase#SENSITIVE}) */
    public static final Comparator NAME_REVERSE = new ReverseComparator(NAME_COMPARATOR);

    /** Case-insensitive name comparator instance (see {@link IOCase#INSENSITIVE}) */
    public static final Comparator NAME_INSENSITIVE_COMPARATOR = new NameFileComparator(IOCase.INSENSITIVE);

    /** Reverse case-insensitive name comparator instance (see {@link IOCase#INSENSITIVE}) */
    public static final Comparator NAME_INSENSITIVE_REVERSE = new ReverseComparator(NAME_INSENSITIVE_COMPARATOR);

    /** System sensitive name comparator instance (see {@link IOCase#SYSTEM}) */
    public static final Comparator NAME_SYSTEM_COMPARATOR = new NameFileComparator(IOCase.SYSTEM);

    /** Reverse system sensitive name comparator instance (see {@link IOCase#SYSTEM}) */
    public static final Comparator NAME_SYSTEM_REVERSE = new ReverseComparator(NAME_SYSTEM_COMPARATOR);

    /** Whether the comparison is case sensitive. */
    private final IOCase caseSensitivity;

    /**
     * Construct a case sensitive file name comparator instance.
     */
    public NameFileComparator() {
        this.caseSensitivity = IOCase.SENSITIVE;
    }

    /**
     * Construct a file name comparator instance with the specified case-sensitivity.
     *
     * @param caseSensitivity  how to handle case sensitivity, null means case-sensitive
     */
    public NameFileComparator(IOCase caseSensitivity) {
        this.caseSensitivity = caseSensitivity == null ? IOCase.SENSITIVE : caseSensitivity;
    }

    /**
     * Compare the names of two files with the specified case sensitivity.
     * 
     * @param obj1 The first file to compare
     * @param obj2 The second file to compare
     * @return a negative value if the first file's name
     * is less than the second, zero if the names are the
     * same and a positive value if the first files name
     * is greater than the second file.
     */
    public int compare(Object obj1, Object obj2) {
        File file1 = (File)obj1;
        File file2 = (File)obj2;
        return caseSensitivity.checkCompareTo(file1.getName(), file2.getName());
    }
}
