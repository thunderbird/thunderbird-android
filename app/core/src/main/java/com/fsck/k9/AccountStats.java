/**
 *
 */
package com.fsck.k9;

import java.io.Serializable;

public class AccountStats implements Serializable {
    private static final long serialVersionUID = -5706839923710842234L;
    public long size = -1;
    public int unreadMessageCount = 0;
    public int flaggedMessageCount = 0;
    public boolean available = true;
}
