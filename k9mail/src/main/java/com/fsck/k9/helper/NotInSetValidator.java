package com.fsck.k9.helper;
import java.util.Set;



/**
 * Created by ConteDiMonteCristo on 16/07/15.
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
