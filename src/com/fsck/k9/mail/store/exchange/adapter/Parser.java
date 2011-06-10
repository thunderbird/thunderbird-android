/*
 * Copyright (C) 2008-2009 Marc Blank
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.mail.store.exchange.adapter;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.exchange.Eas;
import com.fsck.k9.mail.store.exchange.EasException;

/**
 * Extremely fast and lightweight WBXML parser, implementing only the subset of WBXML that
 * EAS uses (as defined in the EAS specification)
 *
 */
public abstract class Parser {

    // The following constants are Wbxml standard
    public static final int START_DOCUMENT = 0;
    public static final int DONE = 1;
    public static final int START = 2;
    public static final int END = 3;
    public static final int TEXT = 4;
    public static final int END_DOCUMENT = 3;
    private static final int NOT_FETCHED = Integer.MIN_VALUE;
    private static final int NOT_ENDED = Integer.MIN_VALUE;
    private static final int EOF_BYTE = -1;
    private boolean logging = false;
    private boolean capture = false;
    private String logTag = "EAS Parser";
    
    // Where tags start in a page
    private static final int TAG_BASE = 5;

    private ArrayList<Integer> captureArray;

    // The input stream for this parser
    private InputStream in;

    // The current tag depth
    private int depth;

    // The upcoming (saved) id from the stream
    private int nextId = NOT_FETCHED;

    // The current tag table (i.e. the tag table for the current page)
    private String[] tagTable;

    // An array of tag tables, as defined in EasTags
    static private String[][] tagTables = new String[24][];

    // The stack of names of tags being processed; used when debug = true
    private String[] nameArray = new String[32];

    // The stack of tags being processed
    private int[] startTagArray = new int[32];

    // The following vars are available to all to avoid method calls that represent the state of
    // the parser at any given time
    public int endTag = NOT_ENDED;

    public int startTag;

    // The type of the last token read
    public int type;

    // The current page
    public int page;

    // The current tag
    public int tag;

    // The name of the current tag
    public String name;

    // Whether the current tag is associated with content (a value)
    private boolean noContent;

    // The value read, as a String.  Only one of text or num will be valid, depending on whether the
    // value was requested as a String or an int (to avoid wasted effort in parsing)
    public String text;

    // The value read, as an int
    public int num;

    public class EofException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    public class EodException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    public class EasParserException extends IOException {
        private static final long serialVersionUID = 1L;

        EasParserException() {
            super("WBXML format error");
        }

        EasParserException(String reason) {
            super(reason);
        }
    }

    public boolean parse() throws IOException, EasException, MessagingException {
        return false;
    }

    /**
     * Initialize the tag tables; they are constant
     *
     */
    {
        String[][] pages = Tags.pages;
        for (int i = 0; i < pages.length; i++) {
            String[] page = pages[i];
            if (page.length > 0) {
                tagTables[i] = page;
            }
        }
    }

    public Parser(InputStream in) throws IOException {
        setInput(in);
        logging = Eas.PARSER_LOG;
    }

    /**
     * Set the debug state of the parser.  When debugging is on, every token is logged (Log.v) to
     * the console.
     *
     * @param val the desired state for debug output
     */
    public void setDebug(boolean val) {
        logging = val;
    }

    /**
     * Set the tag used for logging.  When debugging is on, every token is logged (Log.v) to
     * the console.
     *
     * @param val the logging tag
     */
    public void setLoggingTag(String val) {
        logTag = val;
    }

    /**
     * Turns on data capture; this is used to create test streams that represent "live" data and
     * can be used against the various parsers.
     */
    public void captureOn() {
        capture = true;
        captureArray = new ArrayList<Integer>();
    }

    /**
     * Turns off data capture; writes the captured data to a specified file.
     */
    public void captureOff(Context context, String file) {
        try {
            FileOutputStream out = context.openFileOutput(file, Context.MODE_WORLD_WRITEABLE);
            out.write(captureArray.toString().getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            // This is debug code; exceptions aren't interesting.
        } catch (IOException e) {
            // This is debug code; exceptions aren't interesting.
        }
    }

    /**
     * Return the value of the current tag, as a String
     *
     * @return the String value of the current tag
     * @throws IOException
     */
    public String getValue() throws IOException {
        // The false argument tells getNext to return the value as a String
        getNext(false);
        // This means there was no value given, just <Foo/>; we'll return empty string for now
        if (type == END) {
            if (logging) {
                log("No value for tag: " + tagTable[startTag - TAG_BASE]);
            }
            return "";
        }
        // Save the value
        String val = text;
        // Read the next token; it had better be the end of the current tag
        getNext(false);
        // If not, throw an exception
        if (type != END) {
            throw new IOException("No END found!");
        }
        endTag = startTag;
        return val;
    }

    /**
     * Return the value of the current tag, as an integer
     *
     * @return the integer value of the current tag
     * @throws IOException
     */
   public int getValueInt() throws IOException {
        // The true argument to getNext indicates the desire for an integer return value
        getNext(true);
        if (type == END) {
            return 0;
        }
        // Save the value
        int val = num;
        // Read the next token; it had better be the end of the current tag
        getNext(false);
        // If not, throw an exception
        if (type != END) {
            throw new IOException("No END found!");
        }
        endTag = startTag;
        return val;
    }

    /**
     * Return the next tag found in the stream; special tags END and END_DOCUMENT are used to
     * mark the end of the current tag and end of document.  If we hit end of document without
     * looking for it, generate an EodException.  The tag returned consists of the page number
     * shifted PAGE_SHIFT bits OR'd with the tag retrieved from the stream.  Thus, all tags returned
     * are unique.
     *
     * @param endingTag the tag that would represent the end of the tag we're processing
     * @return the next tag found
     * @throws IOException
     */
    public int nextTag(int endingTag) throws IOException {
        // Lose the page information
        endTag = endingTag &= Tags.PAGE_MASK;
        while (getNext(false) != DONE) {
            // If we're a start, set tag to include the page and return it
            if (type == START) {
                tag = page | startTag;
                return tag;
            // If we're at the ending tag we're looking for, return the END signal
            } else if (type == END && startTag == endTag) {
                return END;
            }
        }
        // We're at end of document here.  If we're looking for it, return END_DOCUMENT
        if (endTag == START_DOCUMENT) {
            return END_DOCUMENT;
        }
        // Otherwise, we've prematurely hit end of document, so exception out
        // EodException is a subclass of IOException; this will be treated as an IO error by
        // SyncManager.
        throw new EodException();
    }

    /**
     * Skip anything found in the stream until the end of the current tag is reached.  This can be
     * used to ignore stretches of xml that aren't needed by the parser.
     *
     * @throws IOException
     */
    public void skipTag() throws IOException {
        int thisTag = startTag;
        // Just loop until we hit the end of the current tag
        while (getNext(false) != DONE) {
            if (type == END && startTag == thisTag) {
                return;
            }
        }

        // If we're at end of document, that's bad
        throw new EofException();
    }

    /**
     * Retrieve the next token from the input stream
     *
     * @return the token found
     * @throws IOException
     */
    public int nextToken() throws IOException {
        getNext(false);
        return type;
    }

    /**
     * Initializes the parser with an input stream; reads the first 4 bytes (which are always the
     * same in EAS, and then sets the tag table to point to page 0 (by definition, the starting
     * page).
     *
     * @param in the InputStream associated with this parser
     * @throws IOException
     */
    public void setInput(InputStream in) throws IOException {
        this.in = in;
        readByte(); // version
        readInt();  // ?
        readInt();  // 106 (UTF-8)
        readInt();  // string table length
        tagTable = tagTables[0];
    }

    /*package*/ void resetInput(InputStream in) {
        this.in = in;
    }
    
    void log(String str) {
        int cr = str.indexOf('\n');
        if (cr > 0) {
            str = str.substring(0, cr);
        }
        Log.v(logTag, str);
//        if (Eas.FILE_LOG) {
//            FileLogger.log(logTag, str);
//        }
    }

    /**
     * Return the next piece of data from the stream.  The return value indicates the type of data
     * that has been retrieved - START (start of tag), END (end of tag), DONE (end of stream), or
     * TEXT (the value of a tag)
     *
     * @param asInt whether a TEXT value should be parsed as a String or an int.
     * @return the type of data retrieved
     * @throws IOException
     */
    private final int getNext(boolean asInt) throws IOException {
        int savedEndTag = endTag;
        if (type == END) {
            depth--;
        } else {
            endTag = NOT_ENDED;
        }

        if (noContent) {
            type = END;
            noContent = false;
            endTag = savedEndTag;
            return type;
        }

        text = null;
        name = null;

        int id = nextId ();
        while (id == Wbxml.SWITCH_PAGE) {
            nextId = NOT_FETCHED;
            // Get the new page number
            int pg = readByte();
            // Save the shifted page to add into the startTag in nextTag
            page = pg << Tags.PAGE_SHIFT;
            // Retrieve the current tag table
            tagTable = tagTables[pg];
            id = nextId();
        }
        nextId = NOT_FETCHED;

        switch (id) {
            case EOF_BYTE:
                // End of document
                type = DONE;
                break;

            case Wbxml.END:
                // End of tag
                type = END;
                if (logging) {
                    name = nameArray[depth];
                    //log("</" + name + '>');
                }
                // Retrieve the now-current startTag from our stack
                startTag = endTag = startTagArray[depth];
                break;

            case Wbxml.STR_I:
                // Inline string
                type = TEXT;
                if (asInt) {
                    num = readInlineInt();
                } else {
                    text = readInlineString();
                }
                if (logging) {
                    name = tagTable[startTag - TAG_BASE];
                    log(name + ": " + (asInt ? Integer.toString(num) : text));
                }
                break;

            default:
                // Start of tag
                type = START;
                // The tag is in the low 6 bits
                startTag = id & 0x3F;
                // If the high bit is set, there is content (a value) to be read
                noContent = (id & 0x40) == 0;
                depth++;
                if (logging) {
                    name = tagTable[startTag - TAG_BASE];
                    //log('<' + name + '>');
                    nameArray[depth] = name;
                }
                // Save the startTag to our stack
                startTagArray[depth] = startTag;
        }

        // Return the type of data we're dealing with
        return type;
    }

    /**
     * Read an int from the input stream, and capture it if necessary for debugging.  Seems a small
     * price to pay...
     *
     * @return the int read
     * @throws IOException
     */
    private int read() throws IOException {
        int i;
        i = in.read();
        if (capture) {
            captureArray.add(i);
        }
        return i;
    }

    private int nextId() throws IOException {
        if (nextId == NOT_FETCHED) {
            nextId = read();
        }
        return nextId;
    }

    private int readByte() throws IOException {
        int i = read();
        if (i == EOF_BYTE) {
            throw new EofException();
        }
        return i;
    }

    /**
     * Read an integer from the stream; this is called when the parser knows that what follows is
     * an inline string representing an integer (e.g. the Read tag in Email has a value known to
     * be either "0" or "1")
     *
     * @return the integer as parsed from the stream
     * @throws IOException
     */
    private int readInlineInt() throws IOException {
        int result = 0;

        while (true) {
            int i = readByte();
            // Inline strings are always terminated with a zero byte
            if (i == 0) {
                return result;
            }
            if (i >= '0' && i <= '9') {
                result = (result * 10) + (i - '0');
            } else {
                throw new IOException("Non integer");
            }
        }
    }

    private int readInt() throws IOException {
        int result = 0;
        int i;

        do {
            i = readByte();
            result = (result << 7) | (i & 0x7f);
        } while ((i & 0x80) != 0);

        return result;
    }

    /**
     * Read an inline string from the stream
     *
     * @return the String as parsed from the stream
     * @throws IOException
     */
    private String readInlineString() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        while (true) {
            int i = read();
            if (i == 0) {
                break;
            } else if (i == EOF_BYTE) {
                throw new EofException();
            }
            outputStream.write(i);
        }
        outputStream.flush();
        String res = outputStream.toString("UTF-8");
        outputStream.close();
        return res;
    }
}
