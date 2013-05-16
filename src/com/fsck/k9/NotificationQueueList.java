package com.fsck.k9;

import java.util.LinkedList;

public class NotificationQueueList extends LinkedList<NotificationQueue> {

	private int mMaxEntries;
	
	public void NotificationQueueList() {
		mMaxEntries = 0;		
	}
	
	public void NotificationQueueList(int inMaxEntries) {
		mMaxEntries = inMaxEntries;
	}
	
	public void SetMaxListSize(int inMaxEntries) {
		mMaxEntries = inMaxEntries;
	}
	
	public boolean add(NotificationQueue inObject) {
		if (mMaxEntries != 0 && super.size() >= mMaxEntries) {
			return false;
		} else {
			return super.add(inObject);
		}
	}
	
}
