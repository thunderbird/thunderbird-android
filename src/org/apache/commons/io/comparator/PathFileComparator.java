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
 * Compare the <b>path</b> of two files for order (see {@link File#getPath()}).
 * <p>
 * This comparator can be used to sort lists or arrays of files
 * by their path either in a case-sensitive, case-insensitive or
 * system dependant case sensitive way. A number of singleton instances
 * are provided for the various case sensitivity options (using {@link IOCase})
 * and the reverse of those options.
 * <p>
 * Example of a <i>case-sensitive</i> file path sort using the
 * {@link #PATH_COMPARATOR} singleton instance:
 * <pre>
 *       List&lt;File&gt; list = ...
 *       Collections.sort(list, PathFileComparator.PATH_COMPARATOR);
 * </pre>
 * <p>
 * Example of a <i>reverse case-insensitive</i> file path sort using the
 * {@link #PATH_INSENSITIVE_REVERSE} singleton instance:
 * <pre>
 *       File[] array = ...
 *       Arrays.sort(array, PathFileComparator.PATH_INSENSITIVE_REVERSE);
 * </pre>
 * <p>
 *
 * @version $Revision: 609243 $ $Date: 2008-01-06 00:30:42 +0000 (Sun, 06 Jan 2008) $
 * @since Commons IO 1.4
 */
public class PathFileComparator implements Comparator, Serializable {

    /** Case-sensitive path comparator instance (see {@link IOCase#SENSITIVE}) */
    public static final Comparator PATH_COMPARATOR = new PathFileComparator();

    /** Reverse case-sensitive path comparator instance (see {@link IOCase#SENSITIVE}) */
    public static final Comparator PATH_REVERSE = new ReverseComparator(PATH_COMPARATOR);

    /** Case-insensitive path comparator instance (see {@link IOCase#INSENSITIVE}) */
    public static final Comparator PATH_INSENSITIVE_COMPARATOR = new PathFileComparator(IOCase.INSENSITIVE);

    /** Reverse case-insensitive path comparator instance (see {@link IOCase#INSENSITIVE}) */
    public static final Comparator PATH_INSENSITIVE_REVERSE = new ReverseComparator(PATH_INSENSITIVE_COMPARATOR);

    /** System sensitive path comparator instance (see {@link IOCase#SYSTEM}) */
    public static final Comparator PATH_SYSTEM_COMPARATOR = new PathFileComparator(IOCase.SYSTEM);

    /** Reverse system sensitive path comparator instance (see {@link IOCase#SYSTEM}) */
    public static final Comparator PATH_SYSTEM_REVERSE = new ReverseComparator(PATH_SYSTEM_COMPARATOR);

    /** Whether the comparison is case sensitive. */
    private final IOCase caseSensitivity;

    /**
     * Construct a case sensitive file path comparator instance.
     */
    public PathFileComparator() {
        this.caseSensitivity = IOCase.SENSITIVE;
    }

    /**
     * Construct a file path comparator instance with the specified case-sensitivity.
     *
     * @param caseSensitivity  how to handle case sensitivity, null means case-sensitive
     */
    public PathFileComparator(IOCase caseSensitivity) {
        this.caseSensitivity = caseSensitivity == null ? IOCase.SENSITIVE : caseSensitivity;
    }

    /**
     * Compare the paths of two files the specified case sensitivity.
     * 
     * @param obj1 The first file to compare
     * @param obj2 The second file to compare
     * @return a negative value if the first file's path
     * is less than the second, zero if the paths are the
     * same and a positive value if the first files path
     * is greater than the second file.
     * 
     */
    public int compare(Object obj1, Object obj2) {
        File file1 = (File)obj1;
        File file2 = (File)obj2;
        return caseSensitivity.checkCompareTo(file1.getPath(), file2.getPath());
    }
}
