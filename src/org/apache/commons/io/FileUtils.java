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
package org.apache.commons.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.output.NullOutputStream;

/**
 * General file manipulation utilities.
 * <p>
 * Facilities are provided in the following areas:
 * <ul>
 * <li>writing to a file
 * <li>reading from a file
 * <li>make a directory including parent directories
 * <li>copying files and directories
 * <li>deleting files and directories
 * <li>converting to and from a URL
 * <li>listing files and directories by filter and extension
 * <li>comparing file content
 * <li>file last changed date
 * <li>calculating a checksum
 * </ul>
 * <p>
 * Origin of code: Excalibur, Alexandria, Commons-Utils
 *
 * @author <a href="mailto:burton@relativity.yi.org">Kevin A. Burton</A>
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph.Reck</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jefft@apache.org">Jeff Turner</a>
 * @author Matthew Hawthorne
 * @author <a href="mailto:jeremias@apache.org">Jeremias Maerki</a>
 * @author Stephen Colebourne
 * @author Ian Springer
 * @author Chris Eldredge
 * @author Jim Harrington
 * @author Niall Pemberton
 * @author Sandy McArthur
 * @version $Id: FileUtils.java 610810 2008-01-10 15:04:49Z niallp $
 */
public class FileUtils {

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public FileUtils() {
        super();
    }

    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;

    /**
     * An empty array of type <code>File</code>.
     */
    public static final File[] EMPTY_FILE_ARRAY = new File[0];

    //-----------------------------------------------------------------------
    /**
     * Opens a {@link FileInputStream} for the specified file, providing better
     * error messages than simply calling <code>new FileInputStream(file)</code>.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * <p>
     * An exception is thrown if the file does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be read.
     * 
     * @param file  the file to open for input, must not be <code>null</code>
     * @return a new {@link FileInputStream} for the specified file
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be read
     * @since Commons IO 1.3
     */
    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    //-----------------------------------------------------------------------
    /**
     * Opens a {@link FileOutputStream} for the specified file, checking and
     * creating the parent directory if it does not exist.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * <p>
     * The parent directory will be created if it does not exist.
     * The file will be created if it does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be written to.
     * An exception is thrown if the parent directory cannot be created.
     * 
     * @param file  the file to open for output, must not be <code>null</code>
     * @return a new {@link FileOutputStream} for the specified file
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be written to
     * @throws IOException if a parent directory needs creating but that fails
     * @since Commons IO 1.3
     */
    public static FileOutputStream openOutputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null && parent.exists() == false) {
                if (parent.mkdirs() == false) {
                    throw new IOException("File '" + file + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a human-readable version of the file size, where the input
     * represents a specific number of bytes.
     *
     * @param size  the number of bytes
     * @return a human-readable display value (includes units)
     */
    public static String byteCountToDisplaySize(long size) {
        String displaySize;

        if (size / ONE_GB > 0) {
            displaySize = String.valueOf(size / ONE_GB) + " GB";
        } else if (size / ONE_MB > 0) {
            displaySize = String.valueOf(size / ONE_MB) + " MB";
        } else if (size / ONE_KB > 0) {
            displaySize = String.valueOf(size / ONE_KB) + " KB";
        } else {
            displaySize = String.valueOf(size) + " bytes";
        }
        return displaySize;
    }

    //-----------------------------------------------------------------------
    /**
     * Implements the same behaviour as the "touch" utility on Unix. It creates
     * a new file with size 0 or, if the file exists already, it is opened and
     * closed without modifying it, but updating the file date and time.
     * <p>
     * NOTE: As from v1.3, this method throws an IOException if the last
     * modified date of the file cannot be set. Also, as from v1.3 this method
     * creates parent directories if they do not exist.
     *
     * @param file  the File to touch
     * @throws IOException If an I/O problem occurs
     */
    public static void touch(File file) throws IOException {
        if (!file.exists()) {
            OutputStream out = openOutputStream(file);
            IOUtils.closeQuietly(out);
        }
        boolean success = file.setLastModified(System.currentTimeMillis());
        if (!success) {
            throw new IOException("Unable to set the last modification time for " + file);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Converts a Collection containing java.io.File instanced into array
     * representation. This is to account for the difference between
     * File.listFiles() and FileUtils.listFiles().
     *
     * @param files  a Collection containing java.io.File instances
     * @return an array of java.io.File
     */
    public static File[] convertFileCollectionToFileArray(Collection files) {
         return (File[]) files.toArray(new File[files.size()]);
    }

    //-----------------------------------------------------------------------
    /**
     * Finds files within a given directory (and optionally its
     * subdirectories). All files found are filtered by an IOFileFilter.
     *
     * @param files the collection of files found.
     * @param directory the directory to search in.
     * @param filter the filter to apply to files and directories.
     */
    private static void innerListFiles(Collection files, File directory,
            IOFileFilter filter) {
        File[] found = directory.listFiles((FileFilter) filter);
        if (found != null) {
            for (int i = 0; i < found.length; i++) {
                if (found[i].isDirectory()) {
                    innerListFiles(files, found[i], filter);
                } else {
                    files.add(found[i]);
                }
            }
        }
    }

    /**
     * Finds files within a given directory (and optionally its
     * subdirectories). All files found are filtered by an IOFileFilter.
     * <p>
     * If your search should recurse into subdirectories you can pass in
     * an IOFileFilter for directories. You don't need to bind a
     * DirectoryFileFilter (via logical AND) to this filter. This method does
     * that for you.
     * <p>
     * An example: If you want to search through all directories called
     * "temp" you pass in <code>FileFilterUtils.NameFileFilter("temp")</code>
     * <p>
     * Another common usage of this method is find files in a directory
     * tree but ignoring the directories generated CVS. You can simply pass
     * in <code>FileFilterUtils.makeCVSAware(null)</code>.
     *
     * @param directory  the directory to search in
     * @param fileFilter  filter to apply when finding files.
     * @param dirFilter  optional filter to apply when finding subdirectories.
     * If this parameter is <code>null</code>, subdirectories will not be included in the
     * search. Use TrueFileFilter.INSTANCE to match all directories.
     * @return an collection of java.io.File with the matching files
     * @see org.apache.commons.io.filefilter.FileFilterUtils
     * @see org.apache.commons.io.filefilter.NameFileFilter
     */
    public static Collection listFiles(
            File directory, IOFileFilter fileFilter, IOFileFilter dirFilter) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Parameter 'directory' is not a directory");
        }
        if (fileFilter == null) {
            throw new NullPointerException("Parameter 'fileFilter' is null");
        }

        //Setup effective file filter
        IOFileFilter effFileFilter = FileFilterUtils.andFileFilter(fileFilter,
            FileFilterUtils.notFileFilter(DirectoryFileFilter.INSTANCE));

        //Setup effective directory filter
        IOFileFilter effDirFilter;
        if (dirFilter == null) {
            effDirFilter = FalseFileFilter.INSTANCE;
        } else {
            effDirFilter = FileFilterUtils.andFileFilter(dirFilter,
                DirectoryFileFilter.INSTANCE);
        }

        //Find files
        Collection files = new java.util.LinkedList();
        innerListFiles(files, directory,
            FileFilterUtils.orFileFilter(effFileFilter, effDirFilter));
        return files;
    }

    /**
     * Allows iteration over the files in given directory (and optionally
     * its subdirectories).
     * <p>
     * All files found are filtered by an IOFileFilter. This method is
     * based on {@link #listFiles(File, IOFileFilter, IOFileFilter)}.
     *
     * @param directory  the directory to search in
     * @param fileFilter  filter to apply when finding files.
     * @param dirFilter  optional filter to apply when finding subdirectories.
     * If this parameter is <code>null</code>, subdirectories will not be included in the
     * search. Use TrueFileFilter.INSTANCE to match all directories.
     * @return an iterator of java.io.File for the matching files
     * @see org.apache.commons.io.filefilter.FileFilterUtils
     * @see org.apache.commons.io.filefilter.NameFileFilter
     * @since Commons IO 1.2
     */
    public static Iterator iterateFiles(
            File directory, IOFileFilter fileFilter, IOFileFilter dirFilter) {
        return listFiles(directory, fileFilter, dirFilter).iterator();
    }

    //-----------------------------------------------------------------------
    /**
     * Converts an array of file extensions to suffixes for use
     * with IOFileFilters.
     *
     * @param extensions  an array of extensions. Format: {"java", "xml"}
     * @return an array of suffixes. Format: {".java", ".xml"}
     */
    private static String[] toSuffixes(String[] extensions) {
        String[] suffixes = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            suffixes[i] = "." + extensions[i];
        }
        return suffixes;
    }


    /**
     * Finds files within a given directory (and optionally its subdirectories)
     * which match an array of extensions.
     *
     * @param directory  the directory to search in
     * @param extensions  an array of extensions, ex. {"java","xml"}. If this
     * parameter is <code>null</code>, all files are returned.
     * @param recursive  if true all subdirectories are searched as well
     * @return an collection of java.io.File with the matching files
     */
    public static Collection listFiles(
            File directory, String[] extensions, boolean recursive) {
        IOFileFilter filter;
        if (extensions == null) {
            filter = TrueFileFilter.INSTANCE;
        } else {
            String[] suffixes = toSuffixes(extensions);
            filter = new SuffixFileFilter(suffixes);
        }
        return listFiles(directory, filter,
            (recursive ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE));
    }

    /**
     * Allows iteration over the files in a given directory (and optionally
     * its subdirectories) which match an array of extensions. This method
     * is based on {@link #listFiles(File, String[], boolean)}.
     *
     * @param directory  the directory to search in
     * @param extensions  an array of extensions, ex. {"java","xml"}. If this
     * parameter is <code>null</code>, all files are returned.
     * @param recursive  if true all subdirectories are searched as well
     * @return an iterator of java.io.File with the matching files
     * @since Commons IO 1.2
     */
    public static Iterator iterateFiles(
            File directory, String[] extensions, boolean recursive) {
        return listFiles(directory, extensions, recursive).iterator();
    }

    //-----------------------------------------------------------------------
    /**
     * Compares the contents of two files to determine if they are equal or not.
     * <p>
     * This method checks to see if the two files are different lengths
     * or if they point to the same file, before resorting to byte-by-byte
     * comparison of the contents.
     * <p>
     * Code origin: Avalon
     *
     * @param file1  the first file
     * @param file2  the second file
     * @return true if the content of the files are equal or they both don't
     * exist, false otherwise
     * @throws IOException in case of an I/O error
     */
    public static boolean contentEquals(File file1, File file2) throws IOException {
        boolean file1Exists = file1.exists();
        if (file1Exists != file2.exists()) {
            return false;
        }

        if (!file1Exists) {
            // two not existing files are equal
            return true;
        }

        if (file1.isDirectory() || file2.isDirectory()) {
            // don't want to compare directory contents
            throw new IOException("Can't compare directories, only files");
        }

        if (file1.length() != file2.length()) {
            // lengths differ, cannot be equal
            return false;
        }

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        InputStream input1 = null;
        InputStream input2 = null;
        try {
            input1 = new FileInputStream(file1);
            input2 = new FileInputStream(file2);
            return IOUtils.contentEquals(input1, input2);

        } finally {
            IOUtils.closeQuietly(input1);
            IOUtils.closeQuietly(input2);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Convert from a <code>URL</code> to a <code>File</code>.
     * <p>
     * From version 1.1 this method will decode the URL.
     * Syntax such as <code>file:///my%20docs/file.txt</code> will be
     * correctly decoded to <code>/my docs/file.txt</code>.
     *
     * @param url  the file URL to convert, <code>null</code> returns <code>null</code>
     * @return the equivalent <code>File</code> object, or <code>null</code>
     *  if the URL's protocol is not <code>file</code>
     * @throws IllegalArgumentException if the file is incorrectly encoded
     */
    public static File toFile(URL url) {
        if (url == null || !url.getProtocol().equals("file")) {
            return null;
        } else {
            String filename = url.getFile().replace('/', File.separatorChar);
            int pos =0;
            while ((pos = filename.indexOf('%', pos)) >= 0) {
                if (pos + 2 < filename.length()) {
                    String hexStr = filename.substring(pos + 1, pos + 3);
                    char ch = (char) Integer.parseInt(hexStr, 16);
                    filename = filename.substring(0, pos) + ch + filename.substring(pos + 3);
                }
            }
            return new File(filename);
        }
    }

    /**
     * Converts each of an array of <code>URL</code> to a <code>File</code>.
     * <p>
     * Returns an array of the same size as the input.
     * If the input is <code>null</code>, an empty array is returned.
     * If the input contains <code>null</code>, the output array contains <code>null</code> at the same
     * index.
     * <p>
     * This method will decode the URL.
     * Syntax such as <code>file:///my%20docs/file.txt</code> will be
     * correctly decoded to <code>/my docs/file.txt</code>.
     *
     * @param urls  the file URLs to convert, <code>null</code> returns empty array
     * @return a non-<code>null</code> array of Files matching the input, with a <code>null</code> item
     *  if there was a <code>null</code> at that index in the input array
     * @throws IllegalArgumentException if any file is not a URL file
     * @throws IllegalArgumentException if any file is incorrectly encoded
     * @since Commons IO 1.1
     */
    public static File[] toFiles(URL[] urls) {
        if (urls == null || urls.length == 0) {
            return EMPTY_FILE_ARRAY;
        }
        File[] files = new File[urls.length];
        for (int i = 0; i < urls.length; i++) {
            URL url = urls[i];
            if (url != null) {
                if (url.getProtocol().equals("file") == false) {
                    throw new IllegalArgumentException(
                            "URL could not be converted to a File: " + url);
                }
                files[i] = toFile(url);
            }
        }
        return files;
    }

    /**
     * Converts each of an array of <code>File</code> to a <code>URL</code>.
     * <p>
     * Returns an array of the same size as the input.
     *
     * @param files  the files to convert
     * @return an array of URLs matching the input
     * @throws IOException if a file cannot be converted
     */
    public static URL[] toURLs(File[] files) throws IOException {
        URL[] urls = new URL[files.length];

        for (int i = 0; i < urls.length; i++) {
            urls[i] = files[i].toURL();
        }

        return urls;
    }

    //-----------------------------------------------------------------------
    /**
     * Copies a file to a directory preserving the file date.
     * <p>
     * This method copies the contents of the specified source file
     * to a file of the same name in the specified destination directory.
     * The destination directory is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     *
     * @param srcFile  an existing file to copy, must not be <code>null</code>
     * @param destDir  the directory to place the copy in, must not be <code>null</code>
     *
     * @throws NullPointerException if source or destination is null
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @see #copyFile(File, File, boolean)
     */
    public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
        copyFileToDirectory(srcFile, destDir, true);
    }

    /**
     * Copies a file to a directory optionally preserving the file date.
     * <p>
     * This method copies the contents of the specified source file
     * to a file of the same name in the specified destination directory.
     * The destination directory is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     *
     * @param srcFile  an existing file to copy, must not be <code>null</code>
     * @param destDir  the directory to place the copy in, must not be <code>null</code>
     * @param preserveFileDate  true if the file date of the copy
     *  should be the same as the original
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @see #copyFile(File, File, boolean)
     * @since Commons IO 1.3
     */
    public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (destDir.exists() && destDir.isDirectory() == false) {
            throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
        }
        copyFile(srcFile, new File(destDir, srcFile.getName()), preserveFileDate);
    }

    /**
     * Copies a file to a new location preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to the
     * specified destination file. The directory holding the destination file is
     * created if it does not exist. If the destination file exists, then this
     * method will overwrite it.
     * 
     * @param srcFile  an existing file to copy, must not be <code>null</code>
     * @param destFile  the new file, must not be <code>null</code>
     * 
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @see #copyFileToDirectory(File, File)
     */
    public static void copyFile(File srcFile, File destFile) throws IOException {
        copyFile(srcFile, destFile, true);
    }

    /**
     * Copies a file to a new location.
     * <p>
     * This method copies the contents of the specified source file
     * to the specified destination file.
     * The directory holding the destination file is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     *
     * @param srcFile  an existing file to copy, must not be <code>null</code>
     * @param destFile  the new file, must not be <code>null</code>
     * @param preserveFileDate  true if the file date of the copy
     *  should be the same as the original
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @see #copyFileToDirectory(File, File, boolean)
     */
    public static void copyFile(File srcFile, File destFile,
            boolean preserveFileDate) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (srcFile.exists() == false) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        }
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        }
        if (destFile.getParentFile() != null && destFile.getParentFile().exists() == false) {
            if (destFile.getParentFile().mkdirs() == false) {
                throw new IOException("Destination '" + destFile + "' directory cannot be created");
            }
        }
        if (destFile.exists() && destFile.canWrite() == false) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        doCopyFile(srcFile, destFile, preserveFileDate);
    }

    /**
     * Internal copy file method.
     * 
     * @param srcFile  the validated source file, must not be <code>null</code>
     * @param destFile  the validated destination file, must not be <code>null</code>
     * @param preserveFileDate  whether to preserve the file date
     * @throws IOException if an error occurs
     */
    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream input = new FileInputStream(srcFile);
        try {
            FileOutputStream output = new FileOutputStream(destFile);
            try {
                IOUtils.copy(input, output);
            } finally {
                IOUtils.closeQuietly(output);
            }
        } finally {
            IOUtils.closeQuietly(input);
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" +
                    srcFile + "' to '" + destFile + "'");
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Copies a directory to within another directory preserving the file dates.
     * <p>
     * This method copies the source directory and all its contents to a
     * directory of the same name in the specified destination directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * @param srcDir  an existing directory to copy, must not be <code>null</code>
     * @param destDir  the directory to place the copy in, must not be <code>null</code>
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.2
     */
    public static void copyDirectoryToDirectory(File srcDir, File destDir) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (srcDir.exists() && srcDir.isDirectory() == false) {
            throw new IllegalArgumentException("Source '" + destDir + "' is not a directory");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (destDir.exists() && destDir.isDirectory() == false) {
            throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
        }
        copyDirectory(srcDir, new File(destDir, srcDir.getName()), true);
    }

    /**
     * Copies a whole directory to a new location preserving the file dates.
     * <p>
     * This method copies the specified directory and all its child
     * directories and files to the specified destination.
     * The destination is the new location and name of the directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * @param srcDir  an existing directory to copy, must not be <code>null</code>
     * @param destDir  the new directory, must not be <code>null</code>
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.1
     */
    public static void copyDirectory(File srcDir, File destDir) throws IOException {
        copyDirectory(srcDir, destDir, true);
    }

    /**
     * Copies a whole directory to a new location.
     * <p>
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * @param srcDir  an existing directory to copy, must not be <code>null</code>
     * @param destDir  the new directory, must not be <code>null</code>
     * @param preserveFileDate  true if the file date of the copy
     *  should be the same as the original
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.1
     */
    public static void copyDirectory(File srcDir, File destDir,
            boolean preserveFileDate) throws IOException {
        copyDirectory(srcDir, destDir, null, preserveFileDate);
    }

    /**
     * Copies a filtered directory to a new location preserving the file dates.
     * <p>
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * <h4>Example: Copy directories only</h4> 
     *  <pre>
     *  // only copy the directory structure
     *  FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY);
     *  </pre>
     *
     * <h4>Example: Copy directories and txt files</h4>
     *  <pre>
     *  // Create a filter for ".txt" files
     *  IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
     *  IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
     *
     *  // Create a filter for either directories or ".txt" files
     *  FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
     *
     *  // Copy using the filter
     *  FileUtils.copyDirectory(srcDir, destDir, filter);
     *  </pre>
     *
     * @param srcDir  an existing directory to copy, must not be <code>null</code>
     * @param destDir  the new directory, must not be <code>null</code>
     * @param filter  the filter to apply, null means copy all directories and files
     *  should be the same as the original
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.4
     */
    public static void copyDirectory(File srcDir, File destDir,
            FileFilter filter) throws IOException {
        copyDirectory(srcDir, destDir, filter, true);
    }

    /**
     * Copies a filtered directory to a new location.
     * <p>
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * <h4>Example: Copy directories only</h4> 
     *  <pre>
     *  // only copy the directory structure
     *  FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY, false);
     *  </pre>
     *
     * <h4>Example: Copy directories and txt files</h4>
     *  <pre>
     *  // Create a filter for ".txt" files
     *  IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
     *  IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
     *
     *  // Create a filter for either directories or ".txt" files
     *  FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
     *
     *  // Copy using the filter
     *  FileUtils.copyDirectory(srcDir, destDir, filter, false);
     *  </pre>
     * 
     * @param srcDir  an existing directory to copy, must not be <code>null</code>
     * @param destDir  the new directory, must not be <code>null</code>
     * @param filter  the filter to apply, null means copy all directories and files
     * @param preserveFileDate  true if the file date of the copy
     *  should be the same as the original
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.4
     */
    public static void copyDirectory(File srcDir, File destDir,
            FileFilter filter, boolean preserveFileDate) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (srcDir.exists() == false) {
            throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        }
        if (srcDir.isDirectory() == false) {
            throw new IOException("Source '" + srcDir + "' exists but is not a directory");
        }
        if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
            throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
        }

        // Cater for destination being directory within the source directory (see IO-141)
        List exclusionList = null;
        if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
            File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
            if (srcFiles != null && srcFiles.length > 0) {
                exclusionList = new ArrayList(srcFiles.length);
                for (int i = 0; i < srcFiles.length; i++) {
                    File copiedFile = new File(destDir, srcFiles[i].getName());
                    exclusionList.add(copiedFile.getCanonicalPath());
                }
            }
        }
        doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList);
    }

    /**
     * Internal copy directory method.
     * 
     * @param srcDir  the validated source directory, must not be <code>null</code>
     * @param destDir  the validated destination directory, must not be <code>null</code>
     * @param filter  the filter to apply, null means copy all directories and files
     * @param preserveFileDate  whether to preserve the file date
     * @param exclusionList  List of files and directories to exclude from the copy, may be null
     * @throws IOException if an error occurs
     * @since Commons IO 1.1
     */
    private static void doCopyDirectory(File srcDir, File destDir, FileFilter filter,
            boolean preserveFileDate, List exclusionList) throws IOException {
        if (destDir.exists()) {
            if (destDir.isDirectory() == false) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (destDir.mkdirs() == false) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
            if (preserveFileDate) {
                destDir.setLastModified(srcDir.lastModified());
            }
        }
        if (destDir.canWrite() == false) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        // recurse
        File[] files = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + srcDir);
        }
        for (int i = 0; i < files.length; i++) {
            File copiedFile = new File(destDir, files[i].getName());
            if (exclusionList == null || !exclusionList.contains(files[i].getCanonicalPath())) {
                if (files[i].isDirectory()) {
                    doCopyDirectory(files[i], copiedFile, filter, preserveFileDate, exclusionList);
                } else {
                    doCopyFile(files[i], copiedFile, preserveFileDate);
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Copies bytes from the URL <code>source</code> to a file
     * <code>destination</code>. The directories up to <code>destination</code>
     * will be created if they don't already exist. <code>destination</code>
     * will be overwritten if it already exists.
     *
     * @param source  the <code>URL</code> to copy bytes from, must not be <code>null</code>
     * @param destination  the non-directory <code>File</code> to write bytes to
     *  (possibly overwriting), must not be <code>null</code>
     * @throws IOException if <code>source</code> URL cannot be opened
     * @throws IOException if <code>destination</code> is a directory
     * @throws IOException if <code>destination</code> cannot be written
     * @throws IOException if <code>destination</code> needs creating but can't be
     * @throws IOException if an IO error occurs during copying
     */
    public static void copyURLToFile(URL source, File destination) throws IOException {
        InputStream input = source.openStream();
        try {
            FileOutputStream output = openOutputStream(destination);
            try {
                IOUtils.copy(input, output);
            } finally {
                IOUtils.closeQuietly(output);
            }
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Deletes a directory recursively. 
     *
     * @param directory  directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message =
                "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
     * </ul>
     *
     * @param file  file or directory to delete, can be <code>null</code>
     * @return <code>true</code> if the file or directory was deleted, otherwise
     * <code>false</code>
     *
     * @since Commons IO 1.4
     */
    public static boolean deleteQuietly(File file) {
        if (file == null) {
            return false;
        }
        try {
            if (file.isDirectory()) {
                cleanDirectory(file);
            }
        } catch (Exception e) {
        }

        try {
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Waits for NFS to propagate a file creation, imposing a timeout.
     * <p>
     * This method repeatedly tests {@link File#exists()} until it returns
     * true up to the maximum time specified in seconds.
     *
     * @param file  the file to check, must not be <code>null</code>
     * @param seconds  the maximum time in seconds to wait
     * @return true if file exists
     * @throws NullPointerException if the file is <code>null</code>
     */
    public static boolean waitFor(File file, int seconds) {
        int timeout = 0;
        int tick = 0;
        while (!file.exists()) {
            if (tick++ >= 10) {
                tick = 0;
                if (timeout++ > seconds) {
                    return false;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
                // ignore exception
            } catch (Exception ex) {
                break;
            }
        }
        return true;
    }

    //-----------------------------------------------------------------------
    /**
     * Reads the contents of a file into a String.
     * The file is always closed.
     *
     * @param file  the file to read, must not be <code>null</code>
     * @param encoding  the encoding to use, <code>null</code> means platform default
     * @return the file contents, never <code>null</code>
     * @throws IOException in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     */
    public static String readFileToString(File file, String encoding) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return IOUtils.toString(in, encoding);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }


    /**
     * Reads the contents of a file into a String using the default encoding for the VM. 
     * The file is always closed.
     *
     * @param file  the file to read, must not be <code>null</code>
     * @return the file contents, never <code>null</code>
     * @throws IOException in case of an I/O error
     * @since Commons IO 1.3.1
     */
    public static String readFileToString(File file) throws IOException {
        return readFileToString(file, null);
    }

    /**
     * Reads the contents of a file into a byte array.
     * The file is always closed.
     *
     * @param file  the file to read, must not be <code>null</code>
     * @return the file contents, never <code>null</code>
     * @throws IOException in case of an I/O error
     * @since Commons IO 1.1
     */
    public static byte[] readFileToByteArray(File file) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return IOUtils.toByteArray(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Reads the contents of a file line by line to a List of Strings.
     * The file is always closed.
     *
     * @param file  the file to read, must not be <code>null</code>
     * @param encoding  the encoding to use, <code>null</code> means platform default
     * @return the list of Strings representing each line in the file, never <code>null</code>
     * @throws IOException in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since Commons IO 1.1
     */
    public static List readLines(File file, String encoding) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return IOUtils.readLines(in, encoding);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Reads the contents of a file line by line to a List of Strings using the default encoding for the VM.
     * The file is always closed.
     *
     * @param file  the file to read, must not be <code>null</code>
     * @return the list of Strings representing each line in the file, never <code>null</code>
     * @throws IOException in case of an I/O error
     * @since Commons IO 1.3
     */
    public static List readLines(File file) throws IOException {
        return readLines(file, null);
    }

    /**
     * Returns an Iterator for the lines in a <code>File</code>.
     * <p>
     * This method opens an <code>InputStream</code> for the file.
     * When you have finished with the iterator you should close the stream
     * to free internal resources. This can be done by calling the
     * {@link LineIterator#close()} or
     * {@link LineIterator#closeQuietly(LineIterator)} method.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * LineIterator it = FileUtils.lineIterator(file, "UTF-8");
     * try {
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   LineIterator.closeQuietly(iterator);
     * }
     * </pre>
     * <p>
     * If an exception occurs during the creation of the iterator, the
     * underlying stream is closed.
     *
     * @param file  the file to open for input, must not be <code>null</code>
     * @param encoding  the encoding to use, <code>null</code> means platform default
     * @return an Iterator of the lines in the file, never <code>null</code>
     * @throws IOException in case of an I/O error (file closed)
     * @since Commons IO 1.2
     */
    public static LineIterator lineIterator(File file, String encoding) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return IOUtils.lineIterator(in, encoding);
        } catch (IOException ex) {
            IOUtils.closeQuietly(in);
            throw ex;
        } catch (RuntimeException ex) {
            IOUtils.closeQuietly(in);
            throw ex;
        }
    }

    /**
     * Returns an Iterator for the lines in a <code>File</code> using the default encoding for the VM.
     *
     * @param file  the file to open for input, must not be <code>null</code>
     * @return an Iterator of the lines in the file, never <code>null</code>
     * @throws IOException in case of an I/O error (file closed)
     * @since Commons IO 1.3
     * @see #lineIterator(File, String)
     */
    public static LineIterator lineIterator(File file) throws IOException {
        return lineIterator(file, null);
    }

    //-----------------------------------------------------------------------
    /**
     * Writes a String to a file creating the file if it does not exist.
     *
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     *
     * @param file  the file to write
     * @param data  the content to write to the file
     * @param encoding  the encoding to use, <code>null</code> means platform default
     * @throws IOException in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     */
    public static void writeStringToFile(File file, String data, String encoding) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file);
            IOUtils.write(data, out, encoding);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Writes a String to a file creating the file if it does not exist using the default encoding for the VM.
     * 
     * @param file  the file to write
     * @param data  the content to write to the file
     * @throws IOException in case of an I/O error
     */
    public static void writeStringToFile(File file, String data) throws IOException {
        writeStringToFile(file, data, null);
    }

    /**
     * Writes a byte array to a file creating the file if it does not exist.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     *
     * @param file  the file to write to
     * @param data  the content to write to the file
     * @throws IOException in case of an I/O error
     * @since Commons IO 1.1
     */
    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file);
            out.write(data);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Writes the <code>toString()</code> value of each item in a collection to
     * the specified <code>File</code> line by line.
     * The specified character encoding and the default line ending will be used.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     *
     * @param file  the file to write to
     * @param encoding  the encoding to use, <code>null</code> means platform default
     * @param lines  the lines to write, <code>null</code> entries produce blank lines
     * @throws IOException in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since Commons IO 1.1
     */
    public static void writeLines(File file, String encoding, Collection lines) throws IOException {
        writeLines(file, encoding, lines, null);
    }

    /**
     * Writes the <code>toString()</code> value of each item in a collection to
     * the specified <code>File</code> line by line.
     * The default VM encoding and the default line ending will be used.
     *
     * @param file  the file to write to
     * @param lines  the lines to write, <code>null</code> entries produce blank lines
     * @throws IOException in case of an I/O error
     * @since Commons IO 1.3
     */
    public static void writeLines(File file, Collection lines) throws IOException {
        writeLines(file, null, lines, null);
    }

    /**
     * Writes the <code>toString()</code> value of each item in a collection to
     * the specified <code>File</code> line by line.
     * The specified character encoding and the line ending will be used.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     *
     * @param file  the file to write to
     * @param encoding  the encoding to use, <code>null</code> means platform default
     * @param lines  the lines to write, <code>null</code> entries produce blank lines
     * @param lineEnding  the line separator to use, <code>null</code> is system default
     * @throws IOException in case of an I/O error
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
     * @since Commons IO 1.1
     */
    public static void writeLines(File file, String encoding, Collection lines, String lineEnding) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file);
            IOUtils.writeLines(lines, lineEnding, out, encoding);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Writes the <code>toString()</code> value of each item in a collection to
     * the specified <code>File</code> line by line.
     * The default VM encoding and the specified line ending will be used.
     *
     * @param file  the file to write to
     * @param lines  the lines to write, <code>null</code> entries produce blank lines
     * @param lineEnding  the line separator to use, <code>null</code> is system default
     * @throws IOException in case of an I/O error
     * @since Commons IO 1.3
     */
    public static void writeLines(File file, Collection lines, String lineEnding) throws IOException {
        writeLines(file, null, lines, lineEnding);
    }

    //-----------------------------------------------------------------------
    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     *      (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file  file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws FileNotFoundException if the file was not found
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent){
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message =
                    "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Schedules a file to be deleted when JVM exits.
     * If file is directory delete it and all sub-directories.
     *
     * @param file  file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the file is <code>null</code>
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDeleteOnExit(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectoryOnExit(file);
        } else {
            file.deleteOnExit();
        }
    }

    /**
     * Schedules a directory recursively for deletion on JVM exit.
     *
     * @param directory  directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws IOException in case deletion is unsuccessful
     */
    private static void deleteDirectoryOnExit(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectoryOnExit(directory);
        directory.deleteOnExit();
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory  directory to clean, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws IOException in case cleaning is unsuccessful
     */
    private static void cleanDirectoryOnExit(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDeleteOnExit(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Makes a directory, including any necessary but nonexistent parent
     * directories. If there already exists a file with specified name or
     * the directory cannot be created then an exception is thrown.
     *
     * @param directory  directory to create, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws IOException if the directory cannot be created
     */
    public static void forceMkdir(File directory) throws IOException {
        if (directory.exists()) {
            if (directory.isFile()) {
                String message =
                    "File "
                        + directory
                        + " exists and is "
                        + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                String message =
                    "Unable to create directory " + directory;
                throw new IOException(message);
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     *
     * @param directory  directory to inspect, must not be <code>null</code>
     * @return size of directory in bytes, 0 if directory is security restricted
     * @throws NullPointerException if the directory is <code>null</code>
     */
    public static long sizeOfDirectory(File directory) {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        long size = 0;

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            return 0L;
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (file.isDirectory()) {
                size += sizeOfDirectory(file);
            } else {
                size += file.length();
            }
        }

        return size;
    }

    //-----------------------------------------------------------------------
    /**
     * Tests if the specified <code>File</code> is newer than the reference
     * <code>File</code>.
     *
     * @param file  the <code>File</code> of which the modification date must
     * be compared, must not be <code>null</code>
     * @param reference  the <code>File</code> of which the modification date
     * is used, must not be <code>null</code>
     * @return true if the <code>File</code> exists and has been modified more
     * recently than the reference <code>File</code>
     * @throws IllegalArgumentException if the file is <code>null</code>
     * @throws IllegalArgumentException if the reference file is <code>null</code> or doesn't exist
     */
     public static boolean isFileNewer(File file, File reference) {
        if (reference == null) {
            throw new IllegalArgumentException("No specified reference file");
        }
        if (!reference.exists()) {
            throw new IllegalArgumentException("The reference file '"
                    + file + "' doesn't exist");
        }
        return isFileNewer(file, reference.lastModified());
    }

    /**
     * Tests if the specified <code>File</code> is newer than the specified
     * <code>Date</code>.
     * 
     * @param file  the <code>File</code> of which the modification date
     * must be compared, must not be <code>null</code>
     * @param date  the date reference, must not be <code>null</code>
     * @return true if the <code>File</code> exists and has been modified
     * after the given <code>Date</code>.
     * @throws IllegalArgumentException if the file is <code>null</code>
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isFileNewer(File file, Date date) {
        if (date == null) {
            throw new IllegalArgumentException("No specified date");
        }
        return isFileNewer(file, date.getTime());
    }

    /**
     * Tests if the specified <code>File</code> is newer than the specified
     * time reference.
     *
     * @param file  the <code>File</code> of which the modification date must
     * be compared, must not be <code>null</code>
     * @param timeMillis  the time reference measured in milliseconds since the
     * epoch (00:00:00 GMT, January 1, 1970)
     * @return true if the <code>File</code> exists and has been modified after
     * the given time reference.
     * @throws IllegalArgumentException if the file is <code>null</code>
     */
     public static boolean isFileNewer(File file, long timeMillis) {
        if (file == null) {
            throw new IllegalArgumentException("No specified file");
        }
        if (!file.exists()) {
            return false;
        }
        return file.lastModified() > timeMillis;
    }


    //-----------------------------------------------------------------------
    /**
     * Tests if the specified <code>File</code> is older than the reference
     * <code>File</code>.
     *
     * @param file  the <code>File</code> of which the modification date must
     * be compared, must not be <code>null</code>
     * @param reference  the <code>File</code> of which the modification date
     * is used, must not be <code>null</code>
     * @return true if the <code>File</code> exists and has been modified before
     * the reference <code>File</code>
     * @throws IllegalArgumentException if the file is <code>null</code>
     * @throws IllegalArgumentException if the reference file is <code>null</code> or doesn't exist
     */
     public static boolean isFileOlder(File file, File reference) {
        if (reference == null) {
            throw new IllegalArgumentException("No specified reference file");
        }
        if (!reference.exists()) {
            throw new IllegalArgumentException("The reference file '"
                    + file + "' doesn't exist");
        }
        return isFileOlder(file, reference.lastModified());
    }

    /**
     * Tests if the specified <code>File</code> is older than the specified
     * <code>Date</code>.
     * 
     * @param file  the <code>File</code> of which the modification date
     * must be compared, must not be <code>null</code>
     * @param date  the date reference, must not be <code>null</code>
     * @return true if the <code>File</code> exists and has been modified
     * before the given <code>Date</code>.
     * @throws IllegalArgumentException if the file is <code>null</code>
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isFileOlder(File file, Date date) {
        if (date == null) {
            throw new IllegalArgumentException("No specified date");
        }
        return isFileOlder(file, date.getTime());
    }

    /**
     * Tests if the specified <code>File</code> is older than the specified
     * time reference.
     *
     * @param file  the <code>File</code> of which the modification date must
     * be compared, must not be <code>null</code>
     * @param timeMillis  the time reference measured in milliseconds since the
     * epoch (00:00:00 GMT, January 1, 1970)
     * @return true if the <code>File</code> exists and has been modified before
     * the given time reference.
     * @throws IllegalArgumentException if the file is <code>null</code>
     */
     public static boolean isFileOlder(File file, long timeMillis) {
        if (file == null) {
            throw new IllegalArgumentException("No specified file");
        }
        if (!file.exists()) {
            return false;
        }
        return file.lastModified() < timeMillis;
    }

    //-----------------------------------------------------------------------
    /**
     * Computes the checksum of a file using the CRC32 checksum routine.
     * The value of the checksum is returned.
     *
     * @param file  the file to checksum, must not be <code>null</code>
     * @return the checksum value
     * @throws NullPointerException if the file or checksum is <code>null</code>
     * @throws IllegalArgumentException if the file is a directory
     * @throws IOException if an IO error occurs reading the file
     * @since Commons IO 1.3
     */
    public static long checksumCRC32(File file) throws IOException {
        CRC32 crc = new CRC32();
        checksum(file, crc);
        return crc.getValue();
    }

    /**
     * Computes the checksum of a file using the specified checksum object.
     * Multiple files may be checked using one <code>Checksum</code> instance
     * if desired simply by reusing the same checksum object.
     * For example:
     * <pre>
     *   long csum = FileUtils.checksum(file, new CRC32()).getValue();
     * </pre>
     *
     * @param file  the file to checksum, must not be <code>null</code>
     * @param checksum  the checksum object to be used, must not be <code>null</code>
     * @return the checksum specified, updated with the content of the file
     * @throws NullPointerException if the file or checksum is <code>null</code>
     * @throws IllegalArgumentException if the file is a directory
     * @throws IOException if an IO error occurs reading the file
     * @since Commons IO 1.3
     */
    public static Checksum checksum(File file, Checksum checksum) throws IOException {
        if (file.isDirectory()) {
            throw new IllegalArgumentException("Checksums can't be computed on directories");
        }
        InputStream in = null;
        try {
            in = new CheckedInputStream(new FileInputStream(file), checksum);
            IOUtils.copy(in, new NullOutputStream());
        } finally {
            IOUtils.closeQuietly(in);
        }
        return checksum;
    }

    /**
     * Moves a directory.
     * <p>
     * When the destination directory is on another file system, do a "copy and delete".
     *
     * @param srcDir the directory to be moved
     * @param destDir the destination directory
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     * @since Commons IO 1.4
     */
    public static void moveDirectory(File srcDir, File destDir) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!srcDir.exists()) {
            throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        }
        if (!srcDir.isDirectory()) {
            throw new IOException("Source '" + srcDir + "' is not a directory");
        }
        if (destDir.exists()) {
            throw new IOException("Destination '" + destDir + "' already exists");
        }
        boolean rename = srcDir.renameTo(destDir);
        if (!rename) {
            copyDirectory( srcDir, destDir );
            deleteDirectory( srcDir );
            if (srcDir.exists()) {
                throw new IOException("Failed to delete original directory '" + srcDir +
                        "' after copy to '" + destDir + "'");
            }
        }
    }

    /**
     * Moves a directory to another directory.
     *
     * @param src the file to be moved
     * @param destDir the destination file
     * @param createDestDir If <code>true</code> create the destination directory,
     * otherwise if <code>false</code> throw an IOException
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     * @since Commons IO 1.4
     */
    public static void moveDirectoryToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
        if (src == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination directory must not be null");
        }
        if (!destDir.exists() && createDestDir) {
            destDir.mkdirs();
        }
        if (!destDir.exists()) {
            throw new FileNotFoundException("Destination directory '" + destDir +
                    "' does not exist [createDestDir=" + createDestDir +"]");
        }
        if (!destDir.isDirectory()) {
            throw new IOException("Destination '" + destDir + "' is not a directory");
        }
        moveDirectory(src, new File(destDir, src.getName()));
    
    }

    /**
     * Moves a file.
     * <p>
     * When the destination file is on another file system, do a "copy and delete".
     *
     * @param srcFile the file to be moved
     * @param destFile the destination file
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     * @since Commons IO 1.4
     */
    public static void moveFile(File srcFile, File destFile) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' is a directory");
        }
        if (destFile.exists()) {
            throw new IOException("Destination '" + destFile + "' already exists");
        }
        if (destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' is a directory");
        }
        boolean rename = srcFile.renameTo(destFile);
        if (!rename) {
            copyFile( srcFile, destFile );
            if (!srcFile.delete()) {
                FileUtils.deleteQuietly(destFile);
                throw new IOException("Failed to delete original file '" + srcFile +
                        "' after copy to '" + destFile + "'");
            }
        }
    }

    /**
     * Moves a file to a directory.
     *
     * @param srcFile the file to be moved
     * @param destDir the destination file
     * @param createDestDir If <code>true</code> create the destination directory,
     * otherwise if <code>false</code> throw an IOException
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     * @since Commons IO 1.4
     */
    public static void moveFileToDirectory(File srcFile, File destDir, boolean createDestDir) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination directory must not be null");
        }
        if (!destDir.exists() && createDestDir) {
            destDir.mkdirs();
        }
        if (!destDir.exists()) {
            throw new FileNotFoundException("Destination directory '" + destDir +
                    "' does not exist [createDestDir=" + createDestDir +"]");
        }
        if (!destDir.isDirectory()) {
            throw new IOException("Destination '" + destDir + "' is not a directory");
        }
        moveFile(srcFile, new File(destDir, srcFile.getName()));
    }

    /**
     * Moves a file or directory to the destination directory.
     * <p>
     * When the destination is on another file system, do a "copy and delete".
     *
     * @param src the file or directory to be moved
     * @param destDir the destination directory 
     * @param createDestDir If <code>true</code> create the destination directory,
     * otherwise if <code>false</code> throw an IOException
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     * @since Commons IO 1.4
     */
    public static void moveToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
        if (src == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!src.exists()) {
            throw new FileNotFoundException("Source '" + src + "' does not exist");
        }
        if (src.isDirectory()) {
            moveDirectoryToDirectory(src, destDir, createDestDir);
        } else {
            moveFileToDirectory(src, destDir, createDestDir);
        }
    }

}
