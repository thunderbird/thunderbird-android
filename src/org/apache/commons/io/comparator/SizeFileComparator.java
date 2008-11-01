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

import org.apache.commons.io.FileUtils;

/**
 * Compare the <b>length/size</b> of two files for order (see
 * {@link File#length()} and {@link FileUtils#sizeOfDirectory(File)}).
 * <p>
 * This comparator can be used to sort lists or arrays of files
 * by their length/size.
 * <p>
 * Example of sorting a list of files using the
 * {@link #SIZE_COMPARATOR} singleton instance:
 * <pre>
 *       List&lt;File&gt; list = ...
 *       Collections.sort(list, LengthFileComparator.LENGTH_COMPARATOR);
 * </pre>
 * <p>
 * Example of doing a <i>reverse</i> sort of an array of files using the
 * {@link #SIZE_REVERSE} singleton instance:
 * <pre>
 *       File[] array = ...
 *       Arrays.sort(array, LengthFileComparator.LENGTH_REVERSE);
 * </pre>
 * <p>
 * <strong>N.B.</strong> Directories are treated as <b>zero size</b> unless
 * <code>sumDirectoryContents</code> is <code>true</code>.
 *
 * @version $Revision: 609243 $ $Date: 2008-01-06 00:30:42 +0000 (Sun, 06 Jan 2008) $
 * @since Commons IO 1.4
 */
public class SizeFileComparator implements Comparator, Serializable {

    /** Size comparator instance - directories are treated as zero size */
    public static final Comparator SIZE_COMPARATOR = new SizeFileComparator();

    /** Reverse size comparator instance - directories are treated as zero size */
    public static final Comparator SIZE_REVERSE = new ReverseComparator(SIZE_COMPARATOR);

    /**
     * Size comparator instance which sums the size of a directory's contents
     * using {@link FileUtils#sizeOfDirectory(File)}
     */
    public static final Comparator SIZE_SUMDIR_COMPARATOR = new SizeFileComparator(true);

    /**
     * Reverse size comparator instance which sums the size of a directory's contents
     * using {@link FileUtils#sizeOfDirectory(File)}
     */
    public static final Comparator SIZE_SUMDIR_REVERSE = new ReverseComparator(SIZE_SUMDIR_COMPARATOR);

    /** Whether the sum of the directory's contents should be calculated. */
    private final boolean sumDirectoryContents;

    /**
     * Construct a file size comparator instance (directories treated as zero size).
     */
    public SizeFileComparator() {
        this.sumDirectoryContents = false;
    }

    /**
     * Construct a file size comparator instance specifying whether the size of
     * the directory contents should be aggregated.
     * <p>
     * If the <code>sumDirectoryContents</code> is <code>true</code> The size of
     * directories is calculated using  {@link FileUtils#sizeOfDirectory(File)}.
     *
     * @param sumDirectoryContents <code>true</code> if the sum of the directoryies contents
     *  should be calculated, otherwise <code>false</code> if directories should be treated
     *  as size zero (see {@link FileUtils#sizeOfDirectory(File)}).
     */
    public SizeFileComparator(boolean sumDirectoryContents) {
        this.sumDirectoryContents = sumDirectoryContents;
    }

    /**
     * Compare the length of two files.
     * 
     * @param obj1 The first file to compare
     * @param obj2 The second file to compare
     * @return a negative value if the first file's length
     * is less than the second, zero if the lengths are the
     * same and a positive value if the first files length
     * is greater than the second file.
     * 
     */
    public int compare(Object obj1, Object obj2) {
        File file1 = (File)obj1;
        File file2 = (File)obj2;
        long size1 = 0;
        if (file1.isDirectory()) {
            size1 = sumDirectoryContents && file1.exists() ? FileUtils.sizeOfDirectory(file1) : 0;
        } else {
            size1 = file1.length();
        }
        long size2 = 0;
        if (file2.isDirectory()) {
            size2 = sumDirectoryContents && file2.exists() ? FileUtils.sizeOfDirectory(file2) : 0;
        } else {
            size2 = file2.length();
        }
        long result = size1 - size2;
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
