/**
 *
 */
package com.fsck.k9;

import java.io.Serializable;

public class AccountStats implements Serializable {
    private static final long serialVersionUID = -5706839923710842234L;
    public long size = -1;
    private int unreadMessageCount = 0;
    public int flaggedMessageCount = 0;
    private boolean available = true;

    public int getUnreadMessageCount() {
		return unreadMessageCount;
    }
    
	public void setUnreadMessageCount(int unreadMessageCount) {
		this.unreadMessageCount = unreadMessageCount;
    }
    
    public void setAvailable(boolean available) {
		this.available = available;
	}
}
