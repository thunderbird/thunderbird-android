package com.fsck.k9.mail.store;

import com.fsck.k9.Account;

/**
 * An {@link Account} is not {@link Account#isAvalaible(android.content.Context)}.<br/>
 * The operation may be retried later.
 */
public class AccountUnavaliableException extends RuntimeException {

	public AccountUnavaliableException() {
		super("please try again later");
	}
}
