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

/**
 * Filters files based on size, can filter either smaller files or
 * files equal to or larger than a given threshold.
 * <p>
 * For example, to print all files and directories in the
 * current directory whose size is greater than 1 MB:
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( new SizeFileFilter(1024 * 1024) );
 * for ( int i = 0; i &lt; files.length; i++ ) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * @author Rahul Akolkar
 * @version $Id: SizeFileFilter.java 591058 2007-11-01 15:47:05Z niallp $
 * @since Commons IO 1.2
 */
public class SizeFileFilter extends AbstractFileFilter implements Serializable {

    /** The size threshold. */
    private final long size;
    /** Whether the files accepted will be larger or smaller. */
    private final boolean acceptLarger;

    /**
     * Constructs a new size file filter for files equal to or 
     * larger than a certain size.
     *
     * @param size  the threshold size of the files
     * @throws IllegalArgumentException if the size is negative
     */
    public SizeFileFilter(long size) {
        this(size, true);
    }

    /**
     * Constructs a new size file filter for files based on a certain size
     * threshold.
     *
     * @param size  the threshold size of the files
     * @param acceptLarger  if true, files equal to or larger are accepted,
     * otherwise smaller ones (but not equal to)
     * @throws IllegalArgumentException if the size is negative
     */
    public SizeFileFilter(long size, boolean acceptLarger) {
        if (size < 0) {
            throw new IllegalArgumentException("The size must be non-negative");
        }
        this.size = size;
        this.acceptLarger = acceptLarger;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks to see if the size of the file is favorable.
     * <p>
     * If size equals threshold and smaller files are required,
     * file <b>IS NOT</b> selected.
     * If size equals threshold and larger files are required,
     * file <b>IS</b> selected.
     *
     * @param file  the File to check
     * @return true if the filename matches
     */
    public boolean accept(File file) {
        boolean smaller = file.length() < size;
        return acceptLarger ? !smaller : smaller;
    }

    /**
     * Provide a String representaion of this file filter.
     *
     * @return a String representaion
     */
    public String toString() {
        String condition = acceptLarger ? ">=" : "<";
        return super.toString() + "(" + condition + size + ")";
    }

}
