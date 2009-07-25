
package com.android.email.mail;

public class MessagingException extends Exception {
    public static final long serialVersionUID = -1;
    
    boolean permanentFailure = false;
    
    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public boolean isPermanentFailure()
    {
      return permanentFailure;
    }

    public void setPermanentFailure(boolean permanentFailure)
    {
      this.permanentFailure = permanentFailure;
    }
    
    
}
