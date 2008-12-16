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

/**
 * Compare the <b>last modified date/time</b> of two files for order 
 * (see {@link File#lastModified()}).
 * <p>
 * This comparator can be used to sort lists or arrays of files
 * by their last modified date/time.
 * <p>
 * Example of sorting a list of files using the
 * {@link #LASTMODIFIED_COMPARATOR} singleton instance:
 * <pre>
 *       List&lt;File&gt; list = ...
 *       Collections.sort(list, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
 * </pre>
 * <p>
 * Example of doing a <i>reverse</i> sort of an array of files using the
 * {@link #LASTMODIFIED_REVERSE} singleton instance:
 * <pre>
 *       File[] array = ...
 *       Arrays.sort(array, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
 * </pre>
 * <p>
 *
 * @version $Revision: 609243 $ $Date: 2008-01-06 00:30:42 +0000 (Sun, 06 Jan 2008) $
 * @since Commons IO 1.4
 */
public class LastModifiedFileComparator implements Comparator, Serializable {

    /** Last modified comparator instance */
    public static final Comparator LASTMODIFIED_COMPARATOR = new LastModifiedFileComparator();

    /** Reverse last modified comparator instance */
    public static final Comparator LASTMODIFIED_REVERSE = new ReverseComparator(LASTMODIFIED_COMPARATOR);

    /**
     * Compare the last the last modified date/time of two files.
     * 
     * @param obj1 The first file to compare
     * @param obj2 The second file to compare
     * @return a negative value if the first file's lastmodified date/time
     * is less than the second, zero if the lastmodified date/time are the
     * same and a positive value if the first files lastmodified date/time
     * is greater than the second file.
     * 
     */
    public int compare(Object obj1, Object obj2) {
        File file1 = (File)obj1;
        File file2 = (File)obj2;
        long result = file1.lastModified() - file2.lastModified();
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
