package com.fsck.k9.mail.store.exchange.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;

import com.fsck.k9.K9;

/**
 * Parse the result of a Ping command
 **/

public class PingParser extends Parser {

    public static final String TAG = "PingParser";

	private List<String> folderList;

    public PingParser(InputStream in) throws IOException {
    	super(in);
    	
        this.folderList = new ArrayList<String>();
    }

    @Override
    public boolean parse() throws IOException {
        int status;
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.PING_PING)
            throw new IOException();
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.PING_STATUS) {
                status = getValueInt();
                if (status >= 3) {
                    Log.e(K9.LOG_TAG, "Ping failed: " + status);
                    throw new IOException("Ping status error");
                }
            } else if (tag == Tags.PING_FOLDERS) {
            	foldersParser();
            } else
                skipTag();
        }
        return res;
    }

    public void foldersParser() throws IOException {
        while (nextTag(Tags.PING_FOLDERS) != END) {
            switch (tag) {
                case Tags.PING_FOLDER:
                    String serverId = getValue();
                    folderList.add(serverId);
                    break;
                default:
                    skipTag();
            }
        }
    }
    
    public List<String> getFolderList() {
		return folderList;
	}

    void userLog(String ...strings) {
    	Log.i(K9.LOG_TAG, Arrays.toString(strings));
    }

    void userLog(String string, int num, String string2) {
    	Log.i(K9.LOG_TAG, string + num + string2);
    }
    
}
