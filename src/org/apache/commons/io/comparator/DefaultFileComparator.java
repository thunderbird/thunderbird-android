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
 * Compare two files using the <b>default</b> {@link File#compareTo(File)} method.
 * <p>
 * This comparator can be used to sort lists or arrays of files
 * by using the default file comparison.
 * <p>
 * Example of sorting a list of files using the
 * {@link #DEFAULT_COMPARATOR} singleton instance:
 * <pre>
 *       List&lt;File&gt; list = ...
 *       Collections.sort(list, DefaultFileComparator.DEFAULT_COMPARATOR);
 * </pre>
 * <p>
 * Example of doing a <i>reverse</i> sort of an array of files using the
 * {@link #DEFAULT_REVERSE} singleton instance:
 * <pre>
 *       File[] array = ...
 *       Arrays.sort(array, DefaultFileComparator.DEFAULT_REVERSE);
 * </pre>
 * <p>
 *
 * @version $Revision: 609243 $ $Date: 2008-01-06 00:30:42 +0000 (Sun, 06 Jan 2008) $
 * @since Commons IO 1.4
 */
public class DefaultFileComparator implements Comparator, Serializable {

    /** Singleton default comparator instance */
    public static final Comparator DEFAULT_COMPARATOR = new DefaultFileComparator();

    /** Singleton reverse default comparator instance */
    public static final Comparator DEFAULT_REVERSE = new ReverseComparator(DEFAULT_COMPARATOR);

    /**
     * Compare the two files using the {@link File#compareTo(File)} method.
     * 
     * @param obj1 The first file to compare
     * @param obj2 The second file to compare
     * @return the result of calling file1's
     * {@link File#compareTo(File)} with file2 as the parameter.
     */
    public int compare(Object obj1, Object obj2) {
        File file1 = (File)obj1;
        File file2 = (File)obj2;
        return file1.compareTo(file2);
    }
}
