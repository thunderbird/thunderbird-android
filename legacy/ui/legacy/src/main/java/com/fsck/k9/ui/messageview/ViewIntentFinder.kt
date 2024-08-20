package com.fsck.k9.ui.messageview;


import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.WorkerThread;
import com.fsck.k9.helper.MimeTypeUtil;
import com.fsck.k9.provider.AttachmentTempFileProvider;


/**
 * Tries to find an {@link Intent#ACTION_VIEW} Intent that can be used to view an attachment.
 */
class ViewIntentFinder {
    private final Context context;

    public ViewIntentFinder(Context context) {
        this.context = context;
    }

    @WorkerThread
    public Intent getBestViewIntent(Uri contentUri, String displayName, String mimeType) {
        String inferredMimeType = MimeTypeUtil.getMimeTypeByExtension(displayName);

        IntentAndResolvedActivitiesCount resolvedIntentInfo;
        if (MimeTypeUtil.isDefaultMimeType(mimeType)) {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, inferredMimeType);
        } else {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, mimeType);
            if (!resolvedIntentInfo.hasResolvedActivities() && !inferredMimeType.equals(mimeType)) {
                resolvedIntentInfo = getViewIntentForMimeType(contentUri, inferredMimeType);
            }
        }

        if (!resolvedIntentInfo.hasResolvedActivities()) {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE);
        }

        return resolvedIntentInfo.getIntent();
    }

    private IntentAndResolvedActivitiesCount getViewIntentForMimeType(Uri contentUri, String mimeType) {
        Intent contentUriIntent = createViewIntentForAttachmentProviderUri(contentUri, mimeType);
        int contentUriActivitiesCount = getResolvedIntentActivitiesCount(contentUriIntent);

        return new IntentAndResolvedActivitiesCount(contentUriIntent, contentUriActivitiesCount);
    }

    private Intent createViewIntentForAttachmentProviderUri(Uri contentUri, String mimeType) {
        Uri uri = AttachmentTempFileProvider.getMimeTypeUri(contentUri, mimeType);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        addUiIntentFlags(intent);

        return intent;
    }

    private void addUiIntentFlags(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    }

    private int getResolvedIntentActivitiesCount(Intent intent) {
        PackageManager packageManager = context.getPackageManager();

        List<ResolveInfo> resolveInfos =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return resolveInfos.size();
    }

    private static class IntentAndResolvedActivitiesCount {
        private Intent intent;
        private int activitiesCount;

        IntentAndResolvedActivitiesCount(Intent intent, int activitiesCount) {
            this.intent = intent;
            this.activitiesCount = activitiesCount;
        }

        public Intent getIntent() {
            return intent;
        }

        public boolean hasResolvedActivities() {
            return activitiesCount > 0;
        }
    }
}
