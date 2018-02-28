package com.fsck.k9.service;


import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.compose.RecipientLoader;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.view.RecipientSelectView;

import java.util.ArrayList;
import java.util.List;


@TargetApi(Build.VERSION_CODES.M)
public class K9ChooserTargetService extends ChooserTargetService {

    private final int MAX_TARGETS = 5;

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {
        final List<ChooserTarget> targets = new ArrayList<>();
        final ComponentName componentName = new ComponentName(this, MessageCompose.class);

        String cryptoProvider = null;//TODO please check, cryptoProvider necessary?
        RecipientLoader recipientLoader = RecipientLoader
                .getMostContactedRecipientLoader(this.getApplicationContext(), cryptoProvider, MAX_TARGETS);
        List<RecipientSelectView.Recipient> recipients = recipientLoader.loadInBackground();

        // Ranking score for target between 0.0f and 1.0f
        float score = 1.0f;

        for (RecipientSelectView.Recipient recipient : recipients) {


            final Bundle extras = new Bundle();
            extras.putString("uuid", recipient.getDisplayNameOrAddress());
            String address = recipient.address.getAddress();
            extras.putStringArray(Intent.EXTRA_EMAIL,
                    new String[] { recipient.getDisplayNameOrAddress() + "<" + address + ">" });

            Icon icon = null;
            Bitmap bitmap = ContactPicture.getContactPictureLoader(getApplicationContext())
                    .loadContactPictureIcon(getApplicationContext(), recipient);
            if (bitmap != null) {
                icon = Icon.createWithBitmap(bitmap);
            }

            targets.add(new ChooserTarget(recipient.getDisplayNameOrAddress(), icon, score, componentName, extras));
            score -= 0.1;
        }

        return targets;
    }

}