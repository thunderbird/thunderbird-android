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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Date;

/**
 * Useful utilities for working with file filters. It provides access to all
 * file filter implementations in this package so you don't have to import
 * every class you use.
 * 
 * @since Commons IO 1.0
 * @version $Id: FileFilterUtils.java 609286 2008-01-06 10:01:26Z scolebourne $
 * 
 * @author Stephen Colebourne
 * @author Jeremias Maerki
 * @author Masato Tezuka
 * @author Rahul Akolkar
 */
public class FileFilterUtils {
    
    /**
     * FileFilterUtils is not normally instantiated.
     */
    public FileFilterUtils() {
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a filter that returns true if the filename starts with the specified text.
     * 
     * @param prefix  the filename prefix
     * @return a prefix checking filter
     */
    public static IOFileFilter prefixFileFilter(String prefix) {
        return new PrefixFileFilter(prefix);
    }

    /**
     * Returns a filter that returns true if the filename ends with the specified text.
     * 
     * @param suffix  the filename suffix
     * @return a suffix checking filter
     */
    public static IOFileFilter suffixFileFilter(String suffix) {
        return new SuffixFileFilter(suffix);
    }

    /**
     * Returns a filter that returns true if the filename matches the specified text.
     * 
     * @param name  the filename
     * @return a name checking filter
     */
    public static IOFileFilter nameFileFilter(String name) {
        return new NameFileFilter(name);
    }

    /**
     * Returns a filter that checks if the file is a directory.
     * 
     * @return file filter that accepts only directories and not files
     */
    public static IOFileFilter directoryFileFilter() {
        return DirectoryFileFilter.DIRECTORY;
    }

    /**
     * Returns a filter that checks if the file is a file (and not a directory).
     * 
     * @return file filter that accepts only files and not directories
     */
    public static IOFileFilter fileFileFilter() {
        return FileFileFilter.FILE;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a filter that ANDs the two specified filters.
     * 
     * @param filter1  the first filter
     * @param filter2  the second filter
     * @return a filter that ANDs the two specified filters
     */
    public static IOFileFilter andFileFilter(IOFileFilter filter1, IOFileFilter filter2) {
        return new AndFileFilter(filter1, filter2);
    }

    /**
     * Returns a filter that ORs the two specified filters.
     * 
     * @param filter1  the first filter
     * @param filter2  the second filter
     * @return a filter that ORs the two specified filters
     */
    public static IOFileFilter orFileFilter(IOFileFilter filter1, IOFileFilter filter2) {
        return new OrFileFilter(filter1, filter2);
    }

    /**
     * Returns a filter that NOTs the specified filter.
     * 
     * @param filter  the filter to invert
     * @return a filter that NOTs the specified filter
     */
    public static IOFileFilter notFileFilter(IOFileFilter filter) {
        return new NotFileFilter(filter);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a filter that always returns true.
     * 
     * @return a true filter
     */
    public static IOFileFilter trueFileFilter() {
        return TrueFileFilter.TRUE;
    }

    /**
     * Returns a filter that always returns false.
     * 
     * @return a false filter
     */
    public static IOFileFilter falseFileFilter() {
        return FalseFileFilter.FALSE;
    }
    
    //-----------------------------------------------------------------------
    /**
     * Returns an <code>IOFileFilter</code> that wraps the
     * <code>FileFilter</code> instance.
     * 
     * @param filter  the filter to be wrapped
     * @return a new filter that implements IOFileFilter
     */
    public static IOFileFilter asFileFilter(FileFilter filter) {
        return new DelegateFileFilter(filter);
    }

    /**
     * Returns an <code>IOFileFilter</code> that wraps the
     * <code>FilenameFilter</code> instance.
     * 
     * @param filter  the filter to be wrapped
     * @return a new filter that implements IOFileFilter
     */
    public static IOFileFilter asFileFilter(FilenameFilter filter) {
        return new DelegateFileFilter(filter);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a filter that returns true if the file was last modified after
     * the specified cutoff time.
     *
     * @param cutoff  the time threshold
     * @return an appropriately configured age file filter
     * @since Commons IO 1.2
     */
    public static IOFileFilter ageFileFilter(long cutoff) {
        return new AgeFileFilter(cutoff);
    }

    /**
     * Returns a filter that filters files based on a cutoff time.
     *
     * @param cutoff  the time threshold
     * @param acceptOlder  if true, older files get accepted, if false, newer
     * @return an appropriately configured age file filter
     * @since Commons IO 1.2
     */
    public static IOFileFilter ageFileFilter(long cutoff, boolean acceptOlder) {
        return new AgeFileFilter(cutoff, acceptOlder);
    }

    /**
     * Returns a filter that returns true if the file was last modified after
     * the specified cutoff date.
     *
     * @param cutoffDate  the time threshold
     * @return an appropriately configured age file filter
     * @since Commons IO 1.2
     */
    public static IOFileFilter ageFileFilter(Date cutoffDate) {
        return new AgeFileFilter(cutoffDate);
    }

    /**
     * Returns a filter that filters files based on a cutoff date.
     *
     * @param cutoffDate  the time threshold
     * @param acceptOlder  if true, older files get accepted, if false, newer
     * @return an appropriately configured age file filter
     * @since Commons IO 1.2
     */
    public static IOFileFilter ageFileFilter(Date cutoffDate, boolean acceptOlder) {
        return new AgeFileFilter(cutoffDate, acceptOlder);
    }

    /**
     * Returns a filter that returns true if the file was last modified after
     * the specified reference file.
     *
     * @param cutoffReference  the file whose last modification
     *        time is usesd as the threshold age of the files
     * @return an appropriately configured age file filter
     * @since Commons IO 1.2
     */
    public static IOFileFilter ageFileFilter(File cutoffReference) {
        return new AgeFileFilter(cutoffReference);
    }

    /**
     * Returns a filter that filters files based on a cutoff reference file.
     *
     * @param cutoffReference  the file whose last modification
     *        time is usesd as the threshold age of the files
     * @param acceptOlder  if true, older files get accepted, if false, newer
     * @return an appropriately configured age file filter
     * @since Commons IO 1.2
     */
    public static IOFileFilter ageFileFilter(File cutoffReference, boolean acceptOlder) {
        return new AgeFileFilter(cutoffReference, acceptOlder);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a filter that returns true if the file is bigger than a certain size.
     *
     * @param threshold  the file size threshold
     * @return an appropriately configured SizeFileFilter
     * @since Commons IO 1.2
     */
    public static IOFileFilter sizeFileFilter(long threshold) {
        return new SizeFileFilter(threshold);
    }

    /**
     * Returns a filter that filters based on file size.
     *
     * @param threshold  the file size threshold
     * @param acceptLarger  if true, larger files get accepted, if false, smaller
     * @return an appropriately configured SizeFileFilter
     * @since Commons IO 1.2
     */
    public static IOFileFilter sizeFileFilter(long threshold, boolean acceptLarger) {
        return new SizeFileFilter(threshold, acceptLarger);
    }

    /**
     * Returns a filter that accepts files whose size is &gt;= minimum size
     * and &lt;= maximum size.
     *
     * @param minSizeInclusive the minimum file size (inclusive)
     * @param maxSizeInclusive the maximum file size (inclusive)
     * @return an appropriately configured IOFileFilter
     * @since Commons IO 1.3
     */
    public static IOFileFilter sizeRangeFileFilter(long minSizeInclusive, long maxSizeInclusive ) {
        IOFileFilter minimumFilter = new SizeFileFilter(minSizeInclusive, true);
        IOFileFilter maximumFilter = new SizeFileFilter(maxSizeInclusive + 1L, false);
        return new AndFileFilter(minimumFilter, maximumFilter);
    }

    //-----------------------------------------------------------------------
    /* Constructed on demand and then cached */
    private static IOFileFilter cvsFilter;

    /* Constructed on demand and then cached */
    private static IOFileFilter svnFilter;

    /**
     * Decorates a filter to make it ignore CVS directories.
     * Passing in <code>null</code> will return a filter that accepts everything
     * except CVS directories.
     * 
     * @param filter  the filter to decorate, null means an unrestricted filter
     * @return the decorated filter, never null
     * @since Commons IO 1.1 (method existed but had bug in 1.0)
     */
    public static IOFileFilter makeCVSAware(IOFileFilter filter) {
        if (cvsFilter == null) {
            cvsFilter = notFileFilter(
                andFileFilter(directoryFileFilter(), nameFileFilter("CVS")));
        }
        if (filter == null) {
            return cvsFilter;
        } else {
            return andFileFilter(filter, cvsFilter);
        }
    }

    /**
     * Decorates a filter to make it ignore SVN directories.
     * Passing in <code>null</code> will return a filter that accepts everything
     * except SVN directories.
     * 
     * @param filter  the filter to decorate, null means an unrestricted filter
     * @return the decorated filter, never null
     * @since Commons IO 1.1
     */
    public static IOFileFilter makeSVNAware(IOFileFilter filter) {
        if (svnFilter == null) {
            svnFilter = notFileFilter(
                andFileFilter(directoryFileFilter(), nameFileFilter(".svn")));
        }
        if (filter == null) {
            return svnFilter;
        } else {
            return andFileFilter(filter, svnFilter);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Decorates a filter so that it only applies to directories and not to files.
     * 
     * @param filter  the filter to decorate, null means an unrestricted filter
     * @return the decorated filter, never null
     * @since Commons IO 1.3
     */
    public static IOFileFilter makeDirectoryOnly(IOFileFilter filter) {
        if (filter == null) {
            return DirectoryFileFilter.DIRECTORY;
        }
        return new AndFileFilter(DirectoryFileFilter.DIRECTORY, filter);
    }

    /**
     * Decorates a filter so that it only applies to files and not to directories.
     * 
     * @param filter  the filter to decorate, null means an unrestricted filter
     * @return the decorated filter, never null
     * @since Commons IO 1.3
     */
    public static IOFileFilter makeFileOnly(IOFileFilter filter) {
        if (filter == null) {
            return FileFileFilter.FILE;
        }
        return new AndFileFilter(FileFileFilter.FILE, filter);
    }

}
