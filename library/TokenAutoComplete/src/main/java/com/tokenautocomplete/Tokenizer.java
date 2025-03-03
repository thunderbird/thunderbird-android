package com.tokenautocomplete;

import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.List;

public interface Tokenizer extends Parcelable {
    /**
     * Find all ranges that can be tokenized. This system should detect possible tokens
     * both with and without having had wrapTokenValue called on the token string representation
     *
     * @param charSequence the string to search in
     * @param start where the tokenizer should start looking for tokens
     * @param end where the tokenizer should stop looking for tokens
     * @return all ranges of characters that are valid tokens
     */
    @NonNull
    List<Range> findTokenRanges(CharSequence charSequence, int start, int end);

    /**
     * Return a complete string representation of the token. Often used to add commas after email
     * addresses when creating tokens
     *
     * This value must NOT include any leading or trailing whitespace
     *
     * @param unwrappedTokenValue the value to wrap
     * @return the token value with any expected delimiter characters
     */
    @NonNull
    CharSequence wrapTokenValue(CharSequence unwrappedTokenValue);

    /**
     * Return true if there is a character in the charSequence that should trigger token detection
     * @param charSequence source text to look at
     * @return true if charSequence contains a value that should end a token
     */
    boolean containsTokenTerminator(CharSequence charSequence);
}
