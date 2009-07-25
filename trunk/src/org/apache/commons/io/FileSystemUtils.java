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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * General File System utilities.
 * <p>
 * This class provides static utility methods for general file system
 * functions not provided via the JDK {@link java.io.File File} class.
 * <p>
 * The current functions provided are:
 * <ul>
 * <li>Get the free space on a drive
 * </ul>
 *
 * @author Frank W. Zammetti
 * @author Stephen Colebourne
 * @author Thomas Ledoux
 * @author James Urie
 * @author Magnus Grimsell
 * @author Thomas Ledoux
 * @version $Id: FileSystemUtils.java 453889 2006-10-07 11:56:25Z scolebourne $
 * @since Commons IO 1.1
 */
public class FileSystemUtils {

    /** Singleton instance, used mainly for testing. */
    private static final FileSystemUtils INSTANCE = new FileSystemUtils();

    /** Operating system state flag for error. */
    private static final int INIT_PROBLEM = -1;
    /** Operating system state flag for neither Unix nor Windows. */
    private static final int OTHER = 0;
    /** Operating system state flag for Windows. */
    private static final int WINDOWS = 1;
    /** Operating system state flag for Unix. */
    private static final int UNIX = 2;
    /** Operating system state flag for Posix flavour Unix. */
    private static final int POSIX_UNIX = 3;

    /** The operating system flag. */
    private static final int OS;
    static {
        int os = OTHER;
        try {
            String osName = System.getProperty("os.name");
            if (osName == null) {
                throw new IOException("os.name not found");
            }
            osName = osName.toLowerCase();
            // match
            if (osName.indexOf("windows") != -1) {
                os = WINDOWS;
            } else if (osName.indexOf("linux") != -1 ||
                osName.indexOf("sun os") != -1 ||
                osName.indexOf("sunos") != -1 ||
                osName.indexOf("solaris") != -1 ||
                osName.indexOf("mpe/ix") != -1 ||
                osName.indexOf("freebsd") != -1 ||
                osName.indexOf("irix") != -1 ||
                osName.indexOf("digital unix") != -1 ||
                osName.indexOf("unix") != -1 ||
                osName.indexOf("mac os x") != -1) {
                os = UNIX;
            } else if (osName.indexOf("hp-ux") != -1 ||
                osName.indexOf("aix") != -1) {
                os = POSIX_UNIX;
            } else {
                os = OTHER;
            }

        } catch (Exception ex) {
            os = INIT_PROBLEM;
        }
        OS = os;
    }

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public FileSystemUtils() {
        super();
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the free space on a drive or volume by invoking
     * the command line.
     * This method does not normalize the result, and typically returns
     * bytes on Windows, 512 byte units on OS X and kilobytes on Unix.
     * As this is not very useful, this method is deprecated in favour
     * of {@link #freeSpaceKb(String)} which returns a result in kilobytes.
     * <p>
     * Note that some OS's are NOT currently supported, including OS/390,
     * OpenVMS and and SunOS 5. (SunOS is supported by <code>freeSpaceKb</code>.)
     * <pre>
     * FileSystemUtils.freeSpace("C:");       // Windows
     * FileSystemUtils.freeSpace("/volume");  // *nix
     * </pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows and 'df' on *nix.
     *
     * @param path  the path to get free space for, not null, not empty on Unix
     * @return the amount of free drive space on the drive or volume
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     * @throws IOException if an error occurs when finding the free space
     * @since Commons IO 1.1, enhanced OS support in 1.2 and 1.3
     * @deprecated Use freeSpaceKb(String)
     *  Deprecated from 1.3, may be removed in 2.0
     */
    public static long freeSpace(String path) throws IOException {
        return INSTANCE.freeSpaceOS(path, OS, false);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the free space on a drive or volume in kilobytes by invoking
     * the command line.
     * <pre>
     * FileSystemUtils.freeSpaceKb("C:");       // Windows
     * FileSystemUtils.freeSpaceKb("/volume");  // *nix
     * </pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows, 'df -kP' on AIX/HP-UX and 'df -k' on other Unix.
     * <p>
     * In order to work, you must be running Windows, or have a implementation of
     * Unix df that supports GNU format when passed -k (or -kP). If you are going
     * to rely on this code, please check that it works on your OS by running
     * some simple tests to compare the command line with the output from this class.
     * If your operating system isn't supported, please raise a JIRA call detailing
     * the exact result from df -k and as much other detail as possible, thanks.
     *
     * @param path  the path to get free space for, not null, not empty on Unix
     * @return the amount of free drive space on the drive or volume in kilobytes
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     * @throws IOException if an error occurs when finding the free space
     * @since Commons IO 1.2, enhanced OS support in 1.3
     */
    public static long freeSpaceKb(String path) throws IOException {
        return INSTANCE.freeSpaceOS(path, OS, true);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the free space on a drive or volume in a cross-platform manner.
     * Note that some OS's are NOT currently supported, including OS/390.
     * <pre>
     * FileSystemUtils.freeSpace("C:");  // Windows
     * FileSystemUtils.freeSpace("/volume");  // *nix
     * </pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows and 'df' on *nix.
     *
     * @param path  the path to get free space for, not null, not empty on Unix
     * @param os  the operating system code
     * @param kb  whether to normalize to kilobytes
     * @return the amount of free drive space on the drive or volume
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     * @throws IOException if an error occurs when finding the free space
     */
    long freeSpaceOS(String path, int os, boolean kb) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be empty");
        }
        switch (os) {
            case WINDOWS:
                return (kb ? freeSpaceWindows(path) / 1024 : freeSpaceWindows(path));
            case UNIX:
                return freeSpaceUnix(path, kb, false);
            case POSIX_UNIX:
                return freeSpaceUnix(path, kb, true);
            case OTHER:
                throw new IllegalStateException("Unsupported operating system");
            default:
                throw new IllegalStateException(
                  "Exception caught when determining operating system");
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Find free space on the Windows platform using the 'dir' command.
     *
     * @param path  the path to get free space for, including the colon
     * @return the amount of free drive space on the drive
     * @throws IOException if an error occurs
     */
    long freeSpaceWindows(String path) throws IOException {
        path = FilenameUtils.normalize(path);
        if (path.length() > 2 && path.charAt(1) == ':') {
            path = path.substring(0, 2);  // seems to make it work
        }
        
        // build and run the 'dir' command
        String[] cmdAttribs = new String[] {"cmd.exe", "/C", "dir /-c " + path};
        
        // read in the output of the command to an ArrayList
        List lines = performCommand(cmdAttribs, Integer.MAX_VALUE);
        
        // now iterate over the lines we just read and find the LAST
        // non-empty line (the free space bytes should be in the last element
        // of the ArrayList anyway, but this will ensure it works even if it's
        // not, still assuming it is on the last non-blank line)
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = (String) lines.get(i);
            if (line.length() > 0) {
                return parseDir(line, path);
            }
        }
        // all lines are blank
        throw new IOException(
                "Command line 'dir /-c' did not return any info " +
                "for path '" + path + "'");
    }

    /**
     * Parses the Windows dir response last line
     *
     * @param line  the line to parse
     * @param path  the path that was sent
     * @return the number of bytes
     * @throws IOException if an error occurs
     */
    long parseDir(String line, String path) throws IOException {
        // read from the end of the line to find the last numeric
        // character on the line, then continue until we find the first
        // non-numeric character, and everything between that and the last
        // numeric character inclusive is our free space bytes count
        int bytesStart = 0;
        int bytesEnd = 0;
        int j = line.length() - 1;
        innerLoop1: while (j >= 0) {
            char c = line.charAt(j);
            if (Character.isDigit(c)) {
              // found the last numeric character, this is the end of
              // the free space bytes count
              bytesEnd = j + 1;
              break innerLoop1;
            }
            j--;
        }
        innerLoop2: while (j >= 0) {
            char c = line.charAt(j);
            if (!Character.isDigit(c) && c != ',' && c != '.') {
              // found the next non-numeric character, this is the
              // beginning of the free space bytes count
              bytesStart = j + 1;
              break innerLoop2;
            }
            j--;
        }
        if (j < 0) {
            throw new IOException(
                    "Command line 'dir /-c' did not return valid info " +
                    "for path '" + path + "'");
        }
        
        // remove commas and dots in the bytes count
        StringBuffer buf = new StringBuffer(line.substring(bytesStart, bytesEnd));
        for (int k = 0; k < buf.length(); k++) {
            if (buf.charAt(k) == ',' || buf.charAt(k) == '.') {
                buf.deleteCharAt(k--);
            }
        }
        return parseBytes(buf.toString(), path);
    }

    //-----------------------------------------------------------------------
    /**
     * Find free space on the *nix platform using the 'df' command.
     *
     * @param path  the path to get free space for
     * @param kb  whether to normalize to kilobytes
     * @param posix  whether to use the posix standard format flag
     * @return the amount of free drive space on the volume
     * @throws IOException if an error occurs
     */
    long freeSpaceUnix(String path, boolean kb, boolean posix) throws IOException {
        if (path.length() == 0) {
            throw new IllegalArgumentException("Path must not be empty");
        }
        path = FilenameUtils.normalize(path);

        // build and run the 'dir' command
        String flags = "-";
        if (kb) {
            flags += "k";
        }
        if (posix) {
            flags += "P";
        }
        String[] cmdAttribs = 
            (flags.length() > 1 ? new String[] {"df", flags, path} : new String[] {"df", path});
        
        // perform the command, asking for up to 3 lines (header, interesting, overflow)
        List lines = performCommand(cmdAttribs, 3);
        if (lines.size() < 2) {
            // unknown problem, throw exception
            throw new IOException(
                    "Command line 'df' did not return info as expected " +
                    "for path '" + path + "'- response was " + lines);
        }
        String line2 = (String) lines.get(1); // the line we're interested in
        
        // Now, we tokenize the string. The fourth element is what we want.
        StringTokenizer tok = new StringTokenizer(line2, " ");
        if (tok.countTokens() < 4) {
            // could be long Filesystem, thus data on third line
            if (tok.countTokens() == 1 && lines.size() >= 3) {
                String line3 = (String) lines.get(2); // the line may be interested in
                tok = new StringTokenizer(line3, " ");
            } else {
                throw new IOException(
                        "Command line 'df' did not return data as expected " +
                        "for path '" + path + "'- check path is valid");
            }
        } else {
            tok.nextToken(); // Ignore Filesystem
        }
        tok.nextToken(); // Ignore 1K-blocks
        tok.nextToken(); // Ignore Used
        String freeSpace = tok.nextToken();
        return parseBytes(freeSpace, path);
    }

    //-----------------------------------------------------------------------
    /**
     * Parses the bytes from a string.
     * 
     * @param freeSpace  the free space string
     * @param path  the path
     * @return the number of bytes
     * @throws IOException if an error occurs
     */
    long parseBytes(String freeSpace, String path) throws IOException {
        try {
            long bytes = Long.parseLong(freeSpace);
            if (bytes < 0) {
                throw new IOException(
                        "Command line 'df' did not find free space in response " +
                        "for path '" + path + "'- check path is valid");
            }
            return bytes;
            
        } catch (NumberFormatException ex) {
            throw new IOException(
                    "Command line 'df' did not return numeric data as expected " +
                    "for path '" + path + "'- check path is valid");
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Performs the os command.
     *
     * @param cmdAttribs  the command line parameters
     * @param max The maximum limit for the lines returned
     * @return the parsed data
     * @throws IOException if an error occurs
     */
    List performCommand(String[] cmdAttribs, int max) throws IOException {
        // this method does what it can to avoid the 'Too many open files' error
        // based on trial and error and these links:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4801027
        // http://forum.java.sun.com/thread.jspa?threadID=533029&messageID=2572018
        // however, its still not perfect as the JDK support is so poor
        // (see commond-exec or ant for a better multi-threaded multi-os solution)
        
        List lines = new ArrayList(20);
        Process proc = null;
        InputStream in = null;
        OutputStream out = null;
        InputStream err = null;
        BufferedReader inr = null;
        try {
            proc = openProcess(cmdAttribs);
            in = proc.getInputStream();
            out = proc.getOutputStream();
            err = proc.getErrorStream();
            inr = new BufferedReader(new InputStreamReader(in));
            String line = inr.readLine();
            while (line != null && lines.size() < max) {
                line = line.toLowerCase().trim();
                lines.add(line);
                line = inr.readLine();
            }
            
            proc.waitFor();
            if (proc.exitValue() != 0) {
                // os command problem, throw exception
                throw new IOException(
                        "Command line returned OS error code '" + proc.exitValue() +
                        "' for command " + Arrays.asList(cmdAttribs));
            }
            if (lines.size() == 0) {
                // unknown problem, throw exception
                throw new IOException(
                        "Command line did not return any info " +
                        "for command " + Arrays.asList(cmdAttribs));
            }
            return lines;
            
        } catch (InterruptedException ex) {
            throw new IOException(
                    "Command line threw an InterruptedException '" + ex.getMessage() +
                    "' for command " + Arrays.asList(cmdAttribs));
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(err);
            IOUtils.closeQuietly(inr);
            if (proc != null) {
                proc.destroy();
            }
        }
    }

    /**
     * Opens the process to the operating system.
     *
     * @param cmdAttribs  the command line parameters
     * @return the process
     * @throws IOException if an error occurs
     */
    Process openProcess(String[] cmdAttribs) throws IOException {
        return Runtime.getRuntime().exec(cmdAttribs);
    }

}
