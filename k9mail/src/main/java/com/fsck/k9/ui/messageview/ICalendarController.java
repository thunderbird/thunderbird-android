package com.fsck.k9.ui.messageview;


import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.cache.TemporaryAttachmentStore;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.ical.ICalParser;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.ICalendarViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalPart;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


public class ICalendarController {
    private final Context context;
    private final MessagingController controller;
    private final MessageViewFragment messageViewFragment;
    private final ICalendarViewInfo iCalendar;
    private final DownloadManager downloadManager;


    ICalendarController(MessagingController controller, DownloadManager downloadManager,
                        MessageViewFragment messageViewFragment, ICalendarViewInfo iCalendar) {
        this.context = messageViewFragment.getApplicationContext();
        this.controller = controller;
        this.downloadManager = downloadManager;
        this.messageViewFragment = messageViewFragment;
        this.iCalendar = iCalendar;
    }

    public void viewICalendar() {
        if (!iCalendar.isContentAvailable) {
            downloadAndViewICalendar((LocalPart) iCalendar.part);
        } else {
            viewLocalICalendar();
        }
    }

    public void saveICalendar() {
        saveICalendarTo(K9.getAttachmentDefaultPath());
    }

    public void saveICalendarTo(String directory) {
        saveICalendarTo(new File(directory));
    }

    private void downloadAndViewICalendar(LocalPart localPart) {
        downloadICalendar(localPart, new Runnable() {
            @Override
            public void run() {
                viewLocalICalendar();
            }
        });
    }

    private void downloadAndSaveICalendarTo(LocalPart localPart, final File directory) {
        downloadICalendar(localPart, new Runnable() {
            @Override
            public void run() {
                saveLocalICalendarTo(directory);
            }
        });
    }

    private void downloadICalendar(LocalPart localPart, final Runnable iCalendarDownloadedCallback) {
        String accountUuid = localPart.getAccountUuid();
        Account account = Preferences.getPreferences(context).getAccount(accountUuid);
        LocalMessage message = localPart.getMessage();

        messageViewFragment.showICalendarLoadingDialog();
        controller.loadAttachment(account, message, iCalendar.part, new MessagingListener() {
            @Override
            public void loadAttachmentFinished(Account account, Message message, Part part) {
                messageViewFragment.hideICalendarLoadingDialogOnMainThread();
                messageViewFragment.runOnMainThread(iCalendarDownloadedCallback);
            }

            @Override
            public void loadAttachmentFailed(Account account, Message message, Part part, String reason) {
                messageViewFragment.hideICalendarLoadingDialogOnMainThread();
            }
        });
    }

    private void viewLocalICalendar() {
        new ViewICalendarAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void saveICalendarTo(File directory) {
        boolean isExternalStorageMounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (!isExternalStorageMounted) {
            String message = context.getString(R.string.message_view_status_calendar_not_saved);
            displayMessageToUser(message);
            return;
        }

        if (!iCalendar.isContentAvailable) {
            downloadAndSaveICalendarTo((LocalPart) iCalendar.part, directory);
        } else {
            saveLocalICalendarTo(directory);
        }
    }

    private void saveLocalICalendarTo(File directory) {
        new SaveICalendarAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, directory);
    }

    private File saveICalendarWithUniqueFileName(File directory) throws IOException {
        String filename = FileHelper.sanitizeFilename(iCalendar.iCalData.getSummary());
        File file = FileHelper.createUniqueFile(directory, filename);

        writeICalendarToStorage(file);

        addSavedICalendarToDownloadsDatabase(file);

        return file;
    }

    private void writeICalendarToStorage(File file) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(iCalendar.uri);
        try {
            OutputStream out = new FileOutputStream(file);
            try {
                IOUtils.copy(in, out);
                out.flush();
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private void addSavedICalendarToDownloadsDatabase(File file) {
        String fileName = file.getName();
        String path = file.getAbsolutePath();
        long fileLength = file.length();
        String mimeType = ICalParser.MIME_TYPE;

        downloadManager.addCompletedDownload(fileName, fileName, true, mimeType, path, fileLength, true);
    }

    private Intent getBestViewIntentAndSaveFileIfNecessary() {
        String displayName = iCalendar.iCalData.getSummary();
        String inferredMimeType = MimeUtility.getMimeTypeByExtension(displayName);

        IntentAndResolvedActivitiesCount resolvedIntentInfo;
        String mimeType = ICalParser.MIME_TYPE;
        resolvedIntentInfo = getBestViewIntentForMimeType(mimeType);
        if (!resolvedIntentInfo.hasResolvedActivities() && !inferredMimeType.equals(mimeType)) {
            resolvedIntentInfo = getBestViewIntentForMimeType(inferredMimeType);
        }

        if (!resolvedIntentInfo.hasResolvedActivities()) {
            resolvedIntentInfo = getBestViewIntentForMimeType(MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE);
        }

        Intent viewIntent;
        if (resolvedIntentInfo.hasResolvedActivities() && resolvedIntentInfo.containsFileUri()) {
            try {
                File tempFile = TemporaryAttachmentStore.getFileForWriting(context, displayName);
                writeICalendarToStorage(tempFile);
                viewIntent = createViewIntentForFileUri(resolvedIntentInfo.getMimeType(), Uri.fromFile(tempFile));
            } catch (IOException e) {
                if (K9.DEBUG) {
                    Log.e(K9.LOG_TAG, "Error while saving calendar to use file:// URI with ACTION_VIEW Intent", e);
                }
                viewIntent = createViewIntentForICalendarProviderUri(MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE);
            }
        } else {
            viewIntent = resolvedIntentInfo.getIntent();
        }

        return viewIntent;
    }

    private IntentAndResolvedActivitiesCount getBestViewIntentForMimeType(String mimeType) {
        Intent contentUriIntent = createViewIntentForICalendarProviderUri(mimeType);
        int contentUriActivitiesCount = getResolvedIntentActivitiesCount(contentUriIntent);

        if (contentUriActivitiesCount > 0) {
            return new IntentAndResolvedActivitiesCount(contentUriIntent, contentUriActivitiesCount);
        }

        File tempFile = TemporaryAttachmentStore.getFile(context, iCalendar.iCalData.getSummary());
        Uri tempFileUri = Uri.fromFile(tempFile);
        Intent fileUriIntent = createViewIntentForFileUri(mimeType, tempFileUri);
        int fileUriActivitiesCount = getResolvedIntentActivitiesCount(fileUriIntent);

        if (fileUriActivitiesCount > 0) {
            return new IntentAndResolvedActivitiesCount(fileUriIntent, fileUriActivitiesCount);
        }

        return new IntentAndResolvedActivitiesCount(contentUriIntent, contentUriActivitiesCount);
    }

    private Intent createViewIntentForICalendarProviderUri(String mimeType) {
        Uri uri = getICalendarUriForMimeType(iCalendar);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        addUiIntentFlags(intent);

        return intent;
    }

    private Uri getICalendarUriForMimeType(ICalendarViewInfo iCalendar) {
        return iCalendar.uri.buildUpon()
                .appendPath(ICalParser.MIME_TYPE)
                .build();
    }

    private Intent createViewIntentForFileUri(String mimeType, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
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

    private void displayICalendarNotSavedMessage() {
        String message = context.getString(R.string.message_view_status_calendar_not_saved);
        displayMessageToUser(message);
    }

    private void displayMessageToUser(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
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

        public String getMimeType() {
            return intent.getType();
        }

        public boolean containsFileUri() {
            return "file".equals(intent.getData().getScheme());
        }
    }

    private class ViewICalendarAsyncTask extends AsyncTask<Void, Void, Intent> {

        @Override
        protected void onPreExecute() {
            messageViewFragment.disableICalendarButtons(iCalendar);
        }

        @Override
        protected Intent doInBackground(Void... params) {
            return getBestViewIntentAndSaveFileIfNecessary();
        }

        @Override
        protected void onPostExecute(Intent intent) {
            viewAttachment(intent);
            messageViewFragment.enableICalendarButtons(iCalendar);
        }

        private void viewAttachment(Intent intent) {
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(K9.LOG_TAG, "Could not display calendar", e);

                String message = context.getString(R.string.message_view_no_viewer, ICalParser.MIME_TYPE);
                displayMessageToUser(message);
            }
        }
    }

    private class SaveICalendarAsyncTask extends AsyncTask<File, Void, File> {

        @Override
        protected void onPreExecute() {
            messageViewFragment.disableICalendarButtons(iCalendar);
        }

        @Override
        protected File doInBackground(File... params) {
            try {
                File directory = params[0];
                return saveICalendarWithUniqueFileName(directory);
            } catch (IOException e) {
                if (K9.DEBUG) {
                    Log.e(K9.LOG_TAG, "Error saving attachment", e);
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(File file) {
            messageViewFragment.enableICalendarButtons(iCalendar);
            if (file == null) {
                displayICalendarNotSavedMessage();
            }
        }
    }
}
