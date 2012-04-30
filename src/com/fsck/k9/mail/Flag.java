
package com.fsck.k9.mail;

/**
 * Flags that can be applied to Messages.
 * 
 * Update: to support IMAP keywords ( custom flags ) this enum became
 * a class. This class was constructed to resemble enums. Since Java
 * enums are objects internally anyway ( implemented similar to these ) 
 * this will not be noticably slower.
 * 
 * The extra field bCustom denotes custom flags. These get an prefix
 * attached internally. When using the flags with external servers,..
 * one should use the realName() method.
 */
public class Flag {
	
	/*
	 * IMAP Spec flags.
	 */
    public static final Flag DELETED = new Flag("DELETED", "\\Deleted");
    public static final Flag SEEN = new Flag("SEEN", "\\Seen");
    public static final Flag ANSWERED = new Flag("ANSWERED", "\\Answered");
    public static final Flag FLAGGED = new Flag("FLAGGED", "\\Flagged");
    
    public static final Flag DRAFT = new Flag("DRAFT", "\\Draft");
    public static final Flag RECENT = new Flag("RECENT", "\\Recent");
    
    private static final Flag[] IMAP_FLAGS = new Flag[] {DELETED, SEEN, ANSWERED, FLAGGED, DRAFT, RECENT};

    /*
     * The following flags are for internal library use only.
     */
    /**
     * Delete and remove from the LocalStore immediately.
     */
    public static final Flag X_DESTROYED = new Flag("X_DESTROYED");

    /**
     * Sending of an unsent message failed. It will be retried. Used to show status.
     */
    public static final Flag X_SEND_FAILED = new Flag("X_SEND_FAILED");

    /**
     * Sending of an unsent message is in progress.
     */
    public static final Flag X_SEND_IN_PROGRESS = new Flag("X_SEND_IN_PROGRESS");

    /**
     * Indicates that a message is fully downloaded from the server and can be viewed normally.
     * This does not include attachments, which are never downloaded fully.
     */
    public static final Flag X_DOWNLOADED_FULL = new Flag("X_DOWNLOADED_FULL");

    /**
     * Indicates that a message is partially downloaded from the server and can be viewed but
     * more content is available on the server.
     * This does not include attachments, which are never downloaded fully.
     */
    public static final Flag X_DOWNLOADED_PARTIAL = new Flag("X_DOWNLOADED_PARTIAL");

    /**
     * Indicates that the copy of a message to the Sent folder has started.
     */
    public static final Flag X_REMOTE_COPY_STARTED = new Flag("X_REMOTE_COPY_STARTED");

    /**
     * Indicates that all headers of the message have been stored in the
     * database. If this is false, additional headers might be retrieved from
     * the server (if the message is still there).
     */
    public static final Flag X_GOT_ALL_HEADERS = new Flag("X_GOT_ALL_HEADERS");
    
    /*
     * Predefined Prefixes
     */
    private static final String USER_PREFIX = "USER_";
    
    private final String name;
    private final String internal_name;
    protected boolean bCustom;
    
    /**
     * When a Flag is created dynamically we know it's a custom flag.
     * @param name Name of the flag.
     * @return Newly created Flag object.
     */
    public static Flag CreateFlag(String name) {
    	Flag tmpFlag = new Flag(USER_PREFIX+name, name);
    	tmpFlag.bCustom = true;
    	return tmpFlag;
	}
    
    private Flag(String name){
    	this(name, name);
    }
    
    private Flag(String internal_name, String name){
    	this.name = name;
    	this.bCustom = true;
    	this.internal_name = internal_name;
    }
    
    /**
     * Returns the predefined static flag object if any. Otherwise a new
     * custom flag is created and returned.
     * 
     * When the name starts with the USER_PREFIX we don't add it again to
     * create a new one. This is the case for example when this is called with
     * strings retrieved from the database.
     * 
     * TODO We might not want to create a custom flag by default and just
     * throw an exception. Only 10 uses of this method so refactoring possible.
     * Catching the exception should then proceed with a CreateFlag(name) call.
     * 
     * @param name Name of Flag wanted.
     * @return	Predefined Flag object if any otherwise new custom Flag.
     * @throws IllegalArgumentException Thrown when the field is not accessible.
     */
    public static Flag valueOf(String name) throws IllegalArgumentException{
    	try {
			return (Flag)(Flag.class.getField(name).get(null));
		} catch (NoSuchFieldException e) {
			// not a predefined flag
			if(name.startsWith(USER_PREFIX))
				return Flag.CreateFlag(name.substring(USER_PREFIX.length()));
			else return Flag.CreateFlag(name);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
    }
    
    /**
     * Returns the predefined flag matching the given "real name". For 
     * example "\\Deleted" will return the DELETED flag. When no such
     * flag exists we assume it's a custom flag and create it.
     * 
     * TODO this could be faster this way: 
     * http://java.dzone.com/articles/enum-tricks-customized-valueof
     * 
     * Since it's only used once I don't see the point.
     * 
     * @param real_name Real name to look for.
     * @return The flag that was found or created.
     */
    public static Flag valueOfByRealName(String internal_name){
    	for( Flag f : IMAP_FLAGS )
    		if(f.internal_name.equalsIgnoreCase(internal_name))
    			return f;
    	return Flag.CreateFlag(internal_name);
    }
    
    @Override
    public String toString() { return name; }
    
    public String name() { return name; }
    
    /**
     * Returns the real keyword name without user prefix. This is
     * for non-internal use ( syncing with IMAP for example ).
     * 
     * @return Real keyword string.
     */
    public String realName(){
    	return internal_name;
    }
    
}
