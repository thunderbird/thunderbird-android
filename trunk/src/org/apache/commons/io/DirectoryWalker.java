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
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

/**
 * Abstract class that walks through a directory hierarchy and provides
 * subclasses with convenient hooks to add specific behaviour.
 * <p>
 * This class operates with a {@link FileFilter} and maximum depth to
 * limit the files and direcories visited.
 * Commons IO supplies many common filter implementations in the 
 * <a href="filefilter/package-summary.html"> filefilter</a> package.
 * <p>
 * The following sections describe:
 *   <ul>
 *      <li><a href="#example">1. Example Implementation</a> - example
 *          <code>FileCleaner</code> implementation.</li>
 *      <li><a href="#filter">2. Filter Example</a> - using 
 *          {@link FileFilter}(s) with <code>DirectoryWalker</code>.</li>
 *      <li><a href="#cancel">3. Cancellation</a> - how to implement cancellation
 *          behaviour.</li>
 *   </ul>
 *
 * <a name="example"></a>
 * <h3>1. Example Implementation</h3>
 *
 * There are many possible extensions, for example, to delete all
 * files and '.svn' directories, and return a list of deleted files:
 * <pre>
 *  public class FileCleaner extends DirectoryWalker {
 *
 *    public FileCleaner() {
 *      super();
 *    }
 *
 *    public List clean(File startDirectory) {
 *      List results = new ArrayList();
 *      walk(startDirectory, results);
 *      return results;
 *    }
 *
 *    protected boolean handleDirectory(File directory, int depth, Collection results) {
 *      // delete svn directories and then skip
 *      if (".svn".equals(directory.getName())) {
 *        directory.delete();
 *        return false;
 *      } else {
 *        return true;
 *      }
 *
 *    }
 *
 *    protected void handleFile(File file, int depth, Collection results) {
 *      // delete file and add to list of deleted
 *      file.delete();
 *      results.add(file);
 *    }
 *  }
 * </pre>
 *
 * <a name="filter"></a>
 * <h3>2. Filter Example</h3>
 *
 * Choosing which directories and files to process can be a key aspect
 * of using this class. This information can be setup in three ways,
 * via three different constructors.
 * <p>
 * The first option is to visit all directories and files.
 * This is achieved via the no-args constructor.
 * <p>
 * The second constructor option is to supply a single {@link FileFilter}
 * that describes the files and directories to visit. Care must be taken
 * with this option as the same filter is used for both directories
 * and files.
 * <p>
 * For example, if you wanted all directories which are not hidden
 * and files which end in ".txt":
 * <pre>
 *  public class FooDirectoryWalker extends DirectoryWalker {
 *    public FooDirectoryWalker(FileFilter filter) {
 *      super(filter, -1);
 *    }
 *  }
 *  
 *  // Build up the filters and create the walker
 *    // Create a filter for Non-hidden directories
 *    IOFileFilter fooDirFilter = 
 *        FileFilterUtils.andFileFilter(FileFilterUtils.directoryFileFilter,
 *                                      HiddenFileFilter.VISIBLE);
 *
 *    // Create a filter for Files ending in ".txt"
 *    IOFileFilter fooFileFilter = 
 *        FileFilterUtils.andFileFilter(FileFilterUtils.fileFileFilter,
 *                                      FileFilterUtils.suffixFileFilter(".txt"));
 *
 *    // Combine the directory and file filters using an OR condition
 *    java.io.FileFilter fooFilter = 
 *        FileFilterUtils.orFileFilter(fooDirFilter, fooFileFilter);
 *
 *    // Use the filter to construct a DirectoryWalker implementation
 *    FooDirectoryWalker walker = new FooDirectoryWalker(fooFilter);
 * </pre>
 * <p>
 * The third constructor option is to specify separate filters, one for
 * directories and one for files. These are combined internally to form
 * the correct <code>FileFilter</code>, something which is very easy to
 * get wrong when attempted manually, particularly when trying to
 * express constructs like 'any file in directories named docs'.
 * <p>
 * For example, if you wanted all directories which are not hidden
 * and files which end in ".txt":
 * <pre>
 *  public class FooDirectoryWalker extends DirectoryWalker {
 *    public FooDirectoryWalker(IOFileFilter dirFilter, IOFileFilter fileFilter) {
 *      super(dirFilter, fileFilter, -1);
 *    }
 *  }
 *  
 *  // Use the filters to construct the walker
 *  FooDirectoryWalker walker = new FooDirectoryWalker(
 *    HiddenFileFilter.VISIBLE,
 *    FileFilterUtils.suffixFileFilter(".txt"),
 *  );
 * </pre>
 * This is much simpler than the previous example, and is why it is the preferred
 * option for filtering.
 *
 * <a name="cancel"></a>
 * <h3>3. Cancellation</h3>
 *
 * The DirectoryWalker contains some of the logic required for cancel processing.
 * Subclasses must complete the implementation.
 * <p>
 * What <code>DirectoryWalker</code> does provide for cancellation is:
 * <ul>
 *    <li>{@link CancelException} which can be thrown in any of the
 *        <i>lifecycle</i> methods to stop processing.</li>
 *    <li>The <code>walk()</code> method traps thrown {@link CancelException}
 *        and calls the <code>handleCancelled()</code> method, providing
 *        a place for custom cancel processing.</li>
 * </ul>
 * <p>
 * Implementations need to provide:
 * <ul>
 *    <li>The decision logic on whether to cancel processing or not.</li>
 *    <li>Constructing and throwing a {@link CancelException}.</li>
 *    <li>Custom cancel processing in the <code>handleCancelled()</code> method.
 * </ul>
 * <p>
 * Two possible scenarios are envisaged for cancellation:
 * <ul>
 *    <li><a href="#external">3.1 External / Mult-threaded</a> - cancellation being
 *        decided/initiated by an external process.</li>
 *    <li><a href="#internal">3.2 Internal</a> - cancellation being decided/initiated 
 *        from within a DirectoryWalker implementation.</li>
 * </ul>
 * <p>
 * The following sections provide example implementations for these two different
 * scenarios.
 *
 * <a name="external"></a>
 * <h4>3.1 External / Multi-threaded</h4>
 *
 * This example provides a public <code>cancel()</code> method that can be
 * called by another thread to stop the processing. A typical example use-case
 * would be a cancel button on a GUI. Calling this method sets a
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/classes.doc.html#36930">
 * volatile</a> flag to ensure it will work properly in a multi-threaded environment.
 * The flag is returned by the <code>handleIsCancelled()</code> method, which
 * will cause the walk to stop immediately. The <code>handleCancelled()</code>
 * method will be the next, and last, callback method received once cancellation
 * has occurred.
 *
 * <pre>
 *  public class FooDirectoryWalker extends DirectoryWalker {
 *
 *    private volatile boolean cancelled = false;
 *
 *    public void cancel() {
 *        cancelled = true;
 *    }
 *
 *    private void handleIsCancelled(File file, int depth, Collection results) {
 *        return cancelled;
 *    }
 *
 *    protected void handleCancelled(File startDirectory, Collection results, CancelException cancel) {
 *        // implement processing required when a cancellation occurs
 *    }
 *  }
 * </pre>
 *
 * <a name="internal"></a>
 * <h4>3.2 Internal</h4>
 *
 * This shows an example of how internal cancellation processing could be implemented.
 * <b>Note</b> the decision logic and throwing a {@link CancelException} could be implemented
 * in any of the <i>lifecycle</i> methods. 
 *
 * <pre>
 *  public class BarDirectoryWalker extends DirectoryWalker {
 *
 *    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
 *        // cancel if hidden directory
 *        if (directory.isHidden()) {
 *            throw new CancelException(file, depth);
 *        }
 *        return true;
 *    }
 *
 *    protected void handleFile(File file, int depth, Collection results) throws IOException {
 *        // cancel if read-only file
 *        if (!file.canWrite()) {
 *            throw new CancelException(file, depth);
 *        }
 *        results.add(file);
 *    }
 *
 *    protected void handleCancelled(File startDirectory, Collection results, CancelException cancel) {
 *        // implement processing required when a cancellation occurs
 *    }
 *  }
 * </pre>
 *
 * @since Commons IO 1.3
 * @version $Revision: 424748 $
 */
public abstract class DirectoryWalker {

    /**
     * The file filter to use to filter files and directories.
     */
    private final FileFilter filter;
    /**
     * The limit on the directory depth to walk.
     */
    private final int depthLimit;

    /**
     * Construct an instance with no filtering and unlimited <i>depth</i>.
     */
    protected DirectoryWalker() {
        this(null, -1);
    }

    /**
     * Construct an instance with a filter and limit the <i>depth</i> navigated to.
     * <p>
     * The filter controls which files and directories will be navigated to as
     * part of the walk. The {@link FileFilterUtils} class is useful for combining
     * various filters together. A <code>null</code> filter means that no
     * filtering should occur and all files and directories will be visited.
     *
     * @param filter  the filter to apply, null means visit all files
     * @param depthLimit  controls how <i>deep</i> the hierarchy is
     *  navigated to (less than 0 means unlimited)
     */
    protected DirectoryWalker(FileFilter filter, int depthLimit) {
        this.filter = filter;
        this.depthLimit = depthLimit;
    }

    /**
     * Construct an instance with a directory and a file filter and an optional
     * limit on the <i>depth</i> navigated to.
     * <p>
     * The filters control which files and directories will be navigated to as part
     * of the walk. This constructor uses {@link FileFilterUtils#makeDirectoryOnly(IOFileFilter)}
     * and {@link FileFilterUtils#makeFileOnly(IOFileFilter)} internally to combine the filters.
     * A <code>null</code> filter means that no filtering should occur.
     *
     * @param directoryFilter  the filter to apply to directories, null means visit all directories
     * @param fileFilter  the filter to apply to files, null means visit all files
     * @param depthLimit  controls how <i>deep</i> the hierarchy is
     *  navigated to (less than 0 means unlimited)
     */
    protected DirectoryWalker(IOFileFilter directoryFilter, IOFileFilter fileFilter, int depthLimit) {
        if (directoryFilter == null && fileFilter == null) {
            this.filter = null;
        } else {
            directoryFilter = (directoryFilter != null ? directoryFilter : TrueFileFilter.TRUE);
            fileFilter = (fileFilter != null ? fileFilter : TrueFileFilter.TRUE);
            directoryFilter = FileFilterUtils.makeDirectoryOnly(directoryFilter);
            fileFilter = FileFilterUtils.makeFileOnly(fileFilter);
            this.filter = FileFilterUtils.orFileFilter(directoryFilter, fileFilter);
        }
        this.depthLimit = depthLimit;
    }

    //-----------------------------------------------------------------------
    /**
     * Internal method that walks the directory hierarchy in a depth-first manner.
     * <p>
     * Users of this class do not need to call this method. This method will
     * be called automatically by another (public) method on the specific subclass.
     * <p>
     * Writers of subclasses should call this method to start the directory walk.
     * Once called, this method will emit events as it walks the hierarchy.
     * The event methods have the prefix <code>handle</code>.
     *
     * @param startDirectory  the directory to start from, not null
     * @param results  the collection of result objects, may be updated
     * @throws NullPointerException if the start directory is null
     * @throws IOException if an I/O Error occurs
     */
    protected final void walk(File startDirectory, Collection results) throws IOException {
        if (startDirectory == null) {
            throw new NullPointerException("Start Directory is null");
        }
        try {
            handleStart(startDirectory, results);
            walk(startDirectory, 0, results);
            handleEnd(results);
        } catch(CancelException cancel) {
            handleCancelled(startDirectory, results, cancel);
        }
    }

    /**
     * Main recursive method to examine the directory hierarchy.
     *
     * @param directory  the directory to examine, not null
     * @param depth  the directory level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    private void walk(File directory, int depth, Collection results) throws IOException {
        checkIfCancelled(directory, depth, results);
        if (handleDirectory(directory, depth, results)) {
            handleDirectoryStart(directory, depth, results);
            int childDepth = depth + 1;
            if (depthLimit < 0 || childDepth <= depthLimit) {
                checkIfCancelled(directory, depth, results);
                File[] childFiles = (filter == null ? directory.listFiles() : directory.listFiles(filter));
                if (childFiles == null) {
                    handleRestricted(directory, childDepth, results);
                } else {
                    for (int i = 0; i < childFiles.length; i++) {
                        File childFile = childFiles[i];
                        if (childFile.isDirectory()) {
                            walk(childFile, childDepth, results);
                        } else {
                            checkIfCancelled(childFile, childDepth, results);
                            handleFile(childFile, childDepth, results);
                            checkIfCancelled(childFile, childDepth, results);
                        }
                    }
                }
            }
            handleDirectoryEnd(directory, depth, results);
        }
        checkIfCancelled(directory, depth, results);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks whether the walk has been cancelled by calling {@link #handleIsCancelled},
     * throwing a <code>CancelException</code> if it has.
     * <p>
     * Writers of subclasses should not normally call this method as it is called
     * automatically by the walk of the tree. However, sometimes a single method,
     * typically {@link #handleFile}, may take a long time to run. In that case,
     * you may wish to check for cancellation by calling this method.
     * 
     * @param file  the current file being processed
     * @param depth  the current file level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    protected final void checkIfCancelled(File file, int depth, Collection results) throws IOException {
        if (handleIsCancelled(file, depth, results)) {
            throw new CancelException(file, depth);
        }
    }

    /**
     * Overridable callback method invoked to determine if the entire walk
     * operation should be immediately cancelled.
     * <p>
     * This method should be implemented by those subclasses that want to
     * provide a public <code>cancel()</code> method available from another
     * thread. The design pattern for the subclass should be as follows:
     * <pre>
     *  public class FooDirectoryWalker extends DirectoryWalker {
     *    private volatile boolean cancelled = false;
     *
     *    public void cancel() {
     *        cancelled = true;
     *    }
     *    private void handleIsCancelled(File file, int depth, Collection results) {
     *        return cancelled;
     *    }
     *    protected void handleCancelled(File startDirectory,
     *              Collection results, CancelException cancel) {
     *        // implement processing required when a cancellation occurs
     *    }
     *  }
     * </pre>
     * <p>
     * If this method returns true, then the directory walk is immediately
     * cancelled. The next callback method will be {@link #handleCancelled}.
     * <p>
     * This implementation returns false.
     *
     * @param file  the file or directory being processed
     * @param depth  the current directory level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @return true if the walk has been cancelled
     * @throws IOException if an I/O Error occurs
     */
    protected boolean handleIsCancelled(
            File file, int depth, Collection results) throws IOException {
        // do nothing - overridable by subclass
        return false;  // not cancelled
    }

    /**
     * Overridable callback method invoked when the operation is cancelled.
     * The file being processed when the cancellation occurred can be
     * obtained from the exception.
     * <p>
     * This implementation just re-throws the {@link CancelException}.
     *
     * @param startDirectory  the directory that the walk started from
     * @param results  the collection of result objects, may be updated
     * @param cancel  the exception throw to cancel further processing
     * containing details at the point of cancellation. 
     * @throws IOException if an I/O Error occurs
     */
    protected void handleCancelled(File startDirectory, Collection results,
                       CancelException cancel) throws IOException {
        // re-throw exception - overridable by subclass
        throw cancel;
    }

    //-----------------------------------------------------------------------
    /**
     * Overridable callback method invoked at the start of processing.
     * <p>
     * This implementation does nothing.
     *
     * @param startDirectory  the directory to start from
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    protected void handleStart(File startDirectory, Collection results) throws IOException {
        // do nothing - overridable by subclass
    }

    /**
     * Overridable callback method invoked to determine if a directory should be processed.
     * <p>
     * This method returns a boolean to indicate if the directory should be examined or not.
     * If you return false, the entire directory and any subdirectories will be skipped.
     * Note that this functionality is in addition to the filtering by file filter.
     * <p>
     * This implementation does nothing and returns true.
     *
     * @param directory  the current directory being processed
     * @param depth  the current directory level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @return true to process this directory, false to skip this directory
     * @throws IOException if an I/O Error occurs
     */
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        // do nothing - overridable by subclass
        return true;  // process directory
    }

    /**
     * Overridable callback method invoked at the start of processing each directory.
     * <p>
     * This implementation does nothing.
     *
     * @param directory  the current directory being processed
     * @param depth  the current directory level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    protected void handleDirectoryStart(File directory, int depth, Collection results) throws IOException {
        // do nothing - overridable by subclass
    }

    /**
     * Overridable callback method invoked for each (non-directory) file.
     * <p>
     * This implementation does nothing.
     *
     * @param file  the current file being processed
     * @param depth  the current directory level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        // do nothing - overridable by subclass
    }

    /**
     * Overridable callback method invoked for each restricted directory.
     * <p>
     * This implementation does nothing.
     *
     * @param directory  the restricted directory
     * @param depth  the current directory level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    protected void handleRestricted(File directory, int depth, Collection results) throws IOException  {
        // do nothing - overridable by subclass
    }

    /**
     * Overridable callback method invoked at the end of processing each directory.
     * <p>
     * This implementation does nothing.
     *
     * @param directory  the directory being processed
     * @param depth  the current directory level (starting directory = 0)
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    protected void handleDirectoryEnd(File directory, int depth, Collection results) throws IOException {
        // do nothing - overridable by subclass
    }

    /**
     * Overridable callback method invoked at the end of processing.
     * <p>
     * This implementation does nothing.
     *
     * @param results  the collection of result objects, may be updated
     * @throws IOException if an I/O Error occurs
     */
    protected void handleEnd(Collection results) throws IOException {
        // do nothing - overridable by subclass
    }

    //-----------------------------------------------------------------------
    /**
     * CancelException is thrown in DirectoryWalker to cancel the current
     * processing.
     */
    public static class CancelException extends IOException {

        /** Serialization id. */
        private static final long serialVersionUID = 1347339620135041008L;
        
        /** The file being processed when the exception was thrown. */
        private File file;
        /** The file depth when the exception was thrown. */
        private int depth = -1;

        /**
         * Constructs a <code>CancelException</code> with
         * the file and depth when cancellation occurred.
         *
         * @param file  the file when the operation was cancelled, may be null
         * @param depth  the depth when the operation was cancelled, may be null
         */
        public CancelException(File file, int depth) {
            this("Operation Cancelled", file, depth);
        }

        /**
         * Constructs a <code>CancelException</code> with
         * an appropriate message and the file and depth when
         * cancellation occurred.
         *
         * @param message  the detail message
         * @param file  the file when the operation was cancelled
         * @param depth  the depth when the operation was cancelled
         */
        public CancelException(String message, File file, int depth) {
            super(message);
            this.file = file;
            this.depth = depth;
        }

        /**
         * Return the file when the operation was cancelled.
         *
         * @return the file when the operation was cancelled
         */
        public File getFile() {
            return file;
        }

        /**
         * Return the depth when the operation was cancelled.
         *
         * @return the depth when the operation was cancelled
         */
        public int getDepth() {
            return depth;
        }
    }
}
