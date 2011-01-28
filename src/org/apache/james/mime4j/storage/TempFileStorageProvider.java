/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A {@link StorageProvider} that stores the data in temporary files. The files
 * are stored either in a user-specified directory or the default temporary-file
 * directory (specified by system property <code>java.io.tmpdir</code>).
 * <p>
 * Example usage:
 *
 * <pre>
 * File directory = new File(&quot;/tmp/mime4j&quot;);
 * StorageProvider provider = new TempFileStorageProvider(directory);
 * DefaultStorageProvider.setInstance(provider);
 * </pre>
 */
public class TempFileStorageProvider extends AbstractStorageProvider {

    private static final String DEFAULT_PREFIX = "m4j";

    private final String prefix;
    private final String suffix;
    private final File directory;

    /**
     * Equivalent to using constructor
     * <code>TempFileStorageProvider("m4j", null, null)</code>.
     */
    public TempFileStorageProvider() {
        this(DEFAULT_PREFIX, null, null);
    }

    /**
     * Equivalent to using constructor
     * <code>TempFileStorageProvider("m4j", null, directory)</code>.
     */
    public TempFileStorageProvider(File directory) {
        this(DEFAULT_PREFIX, null, directory);
    }

    /**
     * Creates a new <code>TempFileStorageProvider</code> using the given
     * values.
     *
     * @param prefix
     *            prefix for generating the temporary file's name; must be at
     *            least three characters long.
     * @param suffix
     *            suffix for generating the temporary file's name; may be
     *            <code>null</code> to use the suffix <code>".tmp"</code>.
     * @param directory
     *            the directory in which the file is to be created, or
     *            <code>null</code> if the default temporary-file directory is
     *            to be used (specified by the system property
     *            <code>java.io.tmpdir</code>).
     * @throws IllegalArgumentException
     *             if the given prefix is less than three characters long or the
     *             given directory does not exist and cannot be created (if it
     *             is not <code>null</code>).
     */
    public TempFileStorageProvider(String prefix, String suffix, File directory) {
        if (prefix == null || prefix.length() < 3)
            throw new IllegalArgumentException("invalid prefix");

        if (directory != null && !directory.isDirectory()
                && !directory.mkdirs())
            throw new IllegalArgumentException("invalid directory");

        this.prefix = prefix;
        this.suffix = suffix;
        this.directory = directory;
    }

    public StorageOutputStream createStorageOutputStream() throws IOException {
        File file = File.createTempFile(prefix, suffix, directory);
        file.deleteOnExit();

        return new TempFileStorageOutputStream(file);
    }

    private static final class TempFileStorageOutputStream extends
            StorageOutputStream {
        private File file;
        private OutputStream out;

        public TempFileStorageOutputStream(File file) throws IOException {
            this.file = file;
            this.out = new FileOutputStream(file);
        }

        @Override
        public void close() throws IOException {
            super.close();
            out.close();
        }

        @Override
        protected void write0(byte[] buffer, int offset, int length)
                throws IOException {
            out.write(buffer, offset, length);
        }

        @Override
        protected Storage toStorage0() throws IOException {
            // out has already been closed because toStorage calls close
            return new TempFileStorage(file);
        }
    }

    private static final class TempFileStorage implements Storage {

        private File file;

        private static final Set<File> filesToDelete = new HashSet<File>();

        public TempFileStorage(File file) {
            this.file = file;
        }

        public void delete() {
            // deleting a file might not immediately succeed if there are still
            // streams left open (especially under Windows). so we keep track of
            // the files that have to be deleted and try to delete all these
            // files each time this method gets invoked.

            // a better but more complicated solution would be to start a
            // separate thread that tries to delete the files periodically.

            synchronized (filesToDelete) {
                if (file != null) {
                    filesToDelete.add(file);
                    file = null;
                }

                for (Iterator<File> iterator = filesToDelete.iterator(); iterator
                        .hasNext();) {
                    File file = iterator.next();
                    if (file.delete()) {
                        iterator.remove();
                    }
                }
            }
        }

        public InputStream getInputStream() throws IOException {
            if (file == null)
                throw new IllegalStateException("storage has been deleted");

            return new BufferedInputStream(new FileInputStream(file));
        }

    }

}
