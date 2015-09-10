package com.fsck.k9.helper;

/**
 * Interface to perform validation os a field value
 */
public interface RangeValidator {

    /**
     *  Validate a text field
     * @param field a text field
     * @return a boolean to flag success or failure
     */
    Boolean validateField(String field);
}
