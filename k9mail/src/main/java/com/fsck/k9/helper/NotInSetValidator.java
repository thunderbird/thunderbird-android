package com.fsck.k9.helper;
import java.util.Set;



/**
 * Class to validate a value based on the predicate that it does not
 * exists in a collection.
 */
public class NotInSetValidator implements RangeValidator{

    private final Set<String> set;

    public NotInSetValidator(Set<String> set)
    {
        this.set = set;
    }

    @Override
    public Boolean validateField(String field) {
        return !field.isEmpty() && !set.contains(field);
    }
}
