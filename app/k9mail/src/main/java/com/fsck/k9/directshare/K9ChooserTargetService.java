package com.fsck.k9.directshare;


import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.compose.RecipientLoader;
import com.fsck.k9.activity.misc.ContactPicture;
import com.fsck.k9.contacts.ContactPictureLoader;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;


@TargetApi(Build.VERSION_CODES.M)
public class K9ChooserTargetService extends ChooserTargetService {
    private static final int MAX_TARGETS = 5;

    private RecipientLoader recipientLoader;
    private ContactPictureLoader contactPictureLoader;

    @Override
    public void onCreate() {
        super.onCreate();

        Context applicationContext = getApplicationContext();
        recipientLoader = RecipientLoader.getMostContactedRecipientLoader(applicationContext, MAX_TARGETS);
        contactPictureLoader = ContactPicture.getContactPictureLoader();
    }

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {
        List<Recipient> recipients = recipientLoader.loadInBackground();

        return createChooserTargets(recipients);
    }

    @NonNull
    private List<ChooserTarget> createChooserTargets(List<Recipient> recipients) {
        float score = 1.0f;

        List<ChooserTarget> targets = new ArrayList<>();
        ComponentName componentName = new ComponentName(this, MessageCompose.class);
        for (Recipient recipient : recipients) {
            Bundle intentExtras = prepareIntentExtras(recipient);
            Icon icon = loadRecipientIcon(recipient);

            ChooserTarget chooserTarget =
                    new ChooserTarget(recipient.getDisplayNameOrAddress(), icon, score, componentName, intentExtras);
            targets.add(chooserTarget);

            score -= 0.1;
        }

        return targets;
    }

    @NonNull
    private Bundle prepareIntentExtras(Recipient recipient) {
        Address address = recipient.address;

        Bundle extras = new Bundle();
        extras.putStringArray(Intent.EXTRA_EMAIL, new String[] { address.toString() });
        extras.putStringArray(Intent.EXTRA_CC, new String[0]);
        extras.putStringArray(Intent.EXTRA_BCC, new String[0]);

        return extras;
    }

    @Nullable
    private Icon loadRecipientIcon(Recipient recipient) {
        Bitmap bitmap = contactPictureLoader.getContactPicture(recipient);
        if (bitmap == null) {
            return null;
        }

        return Icon.createWithBitmap(bitmap);
    }
}
