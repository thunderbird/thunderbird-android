package com.fsck.k9.endtoend.framework;

/**
 * Credentials for the stub IMAP/SMTP server
 */
public class UserForImap {

    public static final UserForImap TEST_USER = new UserForImap("test-username", "test-password", "test-email@example.com");

    public final String loginUsername;
    public final String password;
    public final String emailAddress;

    private UserForImap(String loginUsername, String password, String emailAddress) {
        this.loginUsername = loginUsername;
        this.password = password;
        this.emailAddress = emailAddress;
    }
}
