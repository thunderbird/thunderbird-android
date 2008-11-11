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
package org.apache.commons.io.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * FileWriter that will create and honor lock files to allow simple
 * cross thread file lock handling.
 * <p>
 * This class provides a simple alternative to <code>FileWriter</code>
 * that will use a lock file to prevent duplicate writes.
 * <p>
 * By default, the file will be overwritten, but this may be changed to append.
 * The lock directory may be specified, but defaults to the system property
 * <code>java.io.tmpdir</code>.
 * The encoding may also be specified, and defaults to the platform default.
 *
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author <a href="mailto:ms@collab.net">Michael Salmon</a>
 * @author <a href="mailto:jon@collab.net">Jon S. Stevens</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author Stephen Colebourne
 * @author Andy Lehane
 * @version $Id: LockableFileWriter.java 610010 2008-01-08 14:50:59Z niallp $
 */
public class LockableFileWriter extends Writer {
    // Cannot extend ProxyWriter, as requires writer to be
    // known when super() is called

    /** The extension for the lock file. */
    private static final String LCK = ".lck";

    /** The writer to decorate. */
    private final Writer out;
    /** The lock file. */
    private final File lockFile;

    /**
     * Constructs a LockableFileWriter.
     * If the file exists, it is overwritten.
     *
     * @param fileName  the file to write to, not null
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(String fileName) throws IOException {
        this(fileName, false, null);
    }

    /**
     * Constructs a LockableFileWriter.
     *
     * @param fileName  file to write to, not null
     * @param append  true if content should be appended, false to overwrite
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(String fileName, boolean append) throws IOException {
        this(fileName, append, null);
    }

    /**
     * Constructs a LockableFileWriter.
     *
     * @param fileName  the file to write to, not null
     * @param append  true if content should be appended, false to overwrite
     * @param lockDir  the directory in which the lock file should be held
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(String fileName, boolean append, String lockDir) throws IOException {
        this(new File(fileName), append, lockDir);
    }

    /**
     * Constructs a LockableFileWriter.
     * If the file exists, it is overwritten.
     *
     * @param file  the file to write to, not null
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(File file) throws IOException {
        this(file, false, null);
    }

    /**
     * Constructs a LockableFileWriter.
     *
     * @param file  the file to write to, not null
     * @param append  true if content should be appended, false to overwrite
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(File file, boolean append) throws IOException {
        this(file, append, null);
    }

    /**
     * Constructs a LockableFileWriter.
     *
     * @param file  the file to write to, not null
     * @param append  true if content should be appended, false to overwrite
     * @param lockDir  the directory in which the lock file should be held
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(File file, boolean append, String lockDir) throws IOException {
        this(file, null, append, lockDir);
    }

    /**
     * Constructs a LockableFileWriter with a file encoding.
     *
     * @param file  the file to write to, not null
     * @param encoding  the encoding to use, null means platform default
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(File file, String encoding) throws IOException {
        this(file, encoding, false, null);
    }

    /**
     * Constructs a LockableFileWriter with a file encoding.
     *
     * @param file  the file to write to, not null
     * @param encoding  the encoding to use, null means platform default
     * @param append  true if content should be appended, false to overwrite
     * @param lockDir  the directory in which the lock file should be held
     * @throws NullPointerException if the file is null
     * @throws IOException in case of an I/O error
     */
    public LockableFileWriter(File file, String encoding, boolean append,
            String lockDir) throws IOException {
        super();
        // init file to create/append
        file = file.getAbsoluteFile();
        if (file.getParentFile() != null) {
            FileUtils.forceMkdir(file.getParentFile());
        }
        if (file.isDirectory()) {
            throw new IOException("File specified is a directory");
        }
        
        // init lock file
        if (lockDir == null) {
            lockDir = System.getProperty("java.io.tmpdir");
        }
        File lockDirFile = new File(lockDir);
        FileUtils.forceMkdir(lockDirFile);
        testLockDir(lockDirFile);
        lockFile = new File(lockDirFile, file.getName() + LCK);
        
        // check if locked
        createLock();
        
        // init wrapped writer
        out = initWriter(file, encoding, append);
    }

    //-----------------------------------------------------------------------
    /**
     * Tests that we can write to the lock directory.
     *
     * @param lockDir  the File representing the lock directory
     * @throws IOException if we cannot write to the lock directory
     * @throws IOException if we cannot find the lock file
     */
    private void testLockDir(File lockDir) throws IOException {
        if (!lockDir.exists()) {
            throw new IOException(
                    "Could not find lockDir: " + lockDir.getAbsolutePath());
        }
        if (!lockDir.canWrite()) {
            throw new IOException(
                    "Could not write to lockDir: " + lockDir.getAbsolutePath());
        }
    }

    /**
     * Creates the lock file.
     *
     * @throws IOException if we cannot create the file
     */
    private void createLock() throws IOException {
        synchronized (LockableFileWriter.class) {
            if (!lockFile.createNewFile()) {
                throw new IOException("Can't write file, lock " +
                        lockFile.getAbsolutePath() + " exists");
            }
            lockFile.deleteOnExit();
        }
    }

    /**
     * Initialise the wrapped file writer.
     * Ensure that a cleanup occurs if the writer creation fails.
     *
     * @param file  the file to be accessed
     * @param encoding  the encoding to use
     * @param append  true to append
     * @return The initialised writer
     * @throws IOException if an error occurs
     */
    private Writer initWriter(File file, String encoding, boolean append) throws IOException {
        boolean fileExistedAlready = file.exists();
        OutputStream stream = null;
        Writer writer = null;
        try {
            if (encoding == null) {
                writer = new FileWriter(file.getAbsolutePath(), append);
            } else {
                stream = new FileOutputStream(file.getAbsolutePath(), append);
                writer = new OutputStreamWriter(stream, encoding);
            }
        } catch (IOException ex) {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(stream);
            lockFile.delete();
            if (fileExistedAlready == false) {
                file.delete();
            }
            throw ex;
        } catch (RuntimeException ex) {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(stream);
            lockFile.delete();
            if (fileExistedAlready == false) {
                file.delete();
            }
            throw ex;
        }
        return writer;
    }

    //-----------------------------------------------------------------------
    /**
     * Closes the file writer.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        try {
            out.close();
        } finally {
            lockFile.delete();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Write a character.
     * @param idx the character to write
     * @throws IOException if an I/O error occurs
     */
    public void write(int idx) throws IOException {
        out.write(idx);
    }

    /**
     * Write the characters from an array.
     * @param chr the characters to write
     * @throws IOException if an I/O error occurs
     */
    public void write(char[] chr) throws IOException {
        out.write(chr);
    }

    /**
     * Write the specified characters from an array.
     * @param chr the characters to write
     * @param st The start offset
     * @param end The number of characters to write
     * @throws IOException if an I/O error occurs
     */
    public void write(char[] chr, int st, int end) throws IOException {
        out.write(chr, st, end);
    }

    /**
     * Write the characters from a string.
     * @param str the string to write
     * @throws IOException if an I/O error occurs
     */
    public void write(String str) throws IOException {
        out.write(str);
    }

    /**
     * Write the specified characters from a string.
     * @param str the string to write
     * @param st The start offset
     * @param end The number of characters to write
     * @throws IOException if an I/O error occurs
     */
    public void write(String str, int st, int end) throws IOException {
        out.write(str, st, end);
    }

    /**
     * Flush the stream.
     * @throws IOException if an I/O error occurs
     */
    public void flush() throws IOException {
        out.flush();
    }

}
