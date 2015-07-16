package com.fsck.k9.helper;

import android.text.Editable;

import com.fsck.k9.fragment.CreateLocalFolderDialog;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by ConteDiMonteCristo on 16/07/15.
 * Implemnts
 */
public class NotInSetValidator implements CreateLocalFolderDialog.FolderNameValidation{

    private Set<String> set;

    public NotInSetValidator(Set<String> set)
    {
        this.set = set;
    }

    @Override
    public Boolean validateName(String field) {
        if (field.isEmpty()) return false;
        return !set.contains(field);
    }
}
