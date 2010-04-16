/**
 * 
 */
package com.fsck.k9;

import java.io.Serializable;

public class AccountStats implements Serializable
{
    public long size = 0;
    public int unreadMessageCount = 0;
    public int flaggedMessageCount = 0;
}