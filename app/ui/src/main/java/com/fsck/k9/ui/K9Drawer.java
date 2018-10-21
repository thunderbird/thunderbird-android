package com.fsck.k9.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;

import com.fsck.k9.DI;
import com.fsck.k9.ui.R;
import com.fsck.k9.mailstore.Folder;
import com.fsck.k9.ui.folders.FolderNameFormatter;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.ui.settings.SettingsActivity;

import com.mikepenz.materialdrawer.Drawer.OnDrawerItemClickListener;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.BaseDrawerItem;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import timber.log.Timber;

public class K9Drawer {
    // Resources
    private int iconFolderInboxResId;
    private int iconFolderOutbotResId;
    private int iconFolderSentResId;
    private int iconFolderTrashResId;
    private int iconFolderDraftsResId;
    private int iconFolderArchiveResId;
    private int iconFolderSpamResId;
    private int iconFolderResId;

    private final List<Long> userFolderIds = new ArrayList<>();

    private final FolderNameFormatter folderNameFormatter = DI.get(FolderNameFormatter.class);

    private com.mikepenz.materialdrawer.Drawer drawer;
    private MessageList parent;
    private String openedFolder;

    public K9Drawer(MessageList parent, Bundle savedInstanceState) {
        this.parent = parent;

        drawer = new DrawerBuilder()
                .withActivity(parent)
                .withDisplayBelowStatusBar(false)
                .withTranslucentStatusBar(false)
                .withDrawerLayout(R.layout.material_drawer_fits_not)
                .withActionBarDrawerToggle(true)
                .withOnDrawerItemClickListener(createItemClickListener())
                .withOnDrawerListener(parent.createOnDrawerListener())
                .withSavedInstance(savedInstanceState)
                .build();

        initializeFolderIcons();
    }

    private void initializeFolderIcons() {
        iconFolderInboxResId = getResId(R.attr.iconFolderInbox);
        iconFolderOutbotResId = getResId(R.attr.iconFolderOutbox);
        iconFolderSentResId = getResId(R.attr.iconFolderSent);
        iconFolderTrashResId = getResId(R.attr.iconFolderTrash);
        iconFolderDraftsResId = getResId(R.attr.iconFolderDrafts);
        iconFolderArchiveResId = getResId(R.attr.iconFolderArchive);
        iconFolderSpamResId = getResId(R.attr.iconFolderSpam);
        iconFolderResId = getResId(R.attr.iconFolder);
    }

    private int getResId(int resAttribute) {
        TypedValue typedValue = new TypedValue();
        boolean found = parent.getTheme().resolveAttribute(resAttribute, typedValue, true);
        if (!found) {
            throw new AssertionError("Couldn't find resource with attribute " + resAttribute);
        }
        return typedValue.resourceId;
    }

    private int getFolderIcon(Folder folder) {
        switch (folder.getType()) {
            case INBOX: return iconFolderInboxResId;
            case OUTBOX: return iconFolderOutbotResId;
            case SENT: return iconFolderSentResId;
            case TRASH: return iconFolderTrashResId;
            case DRAFTS: return iconFolderDraftsResId;
            case ARCHIVE: return iconFolderArchiveResId;
            case SPAM: return iconFolderSpamResId;
            default: return iconFolderResId;
        }
    }

    private String getFolderDisplayName(Folder folder) {
        return folderNameFormatter.displayName(folder);
    }

    private OnDrawerItemClickListener createItemClickListener() {
        return new OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                Folder folder = (Folder) drawerItem.getTag();
                parent.openFolder(folder.getServerId());
                return false;
            }
        };
    }

    /**
     * Set the user folders to display in the drawer
     *
     * @param folders
     *         Folder objects to use
     */
    public void setUserFolders(@Nullable List<Folder> folders) {
        if (folders != null) {
            List<IDrawerItem> drawerItems = new ArrayList<>(folders.size());
            long openedFolderId = -1;
            for (Folder folder : folders) {
                PrimaryDrawerItem item = new PrimaryDrawerItem()
                        .withIcon(getFolderIcon(folder))
                        .withIdentifier(folder.getId())
                        .withTag(folder)
                        .withName(getFolderDisplayName(folder));
                drawerItems.add(item);
                userFolderIds.add(folder.getId());

                if (folder.getServerId().equals(openedFolder)) {
                    openedFolderId = folder.getId();
                }
            }
            drawer.setItems(drawerItems);

            if (openedFolderId != -1) {
                drawer.setSelection(openedFolderId, false);
            }
        } else {
            Timber.d("clearing folders");
            drawer.removeAllItems();
            userFolderIds.clear();
        }
    }

    public void selectFolderId(String folderId) {
        openedFolder = folderId;
        for (long id : userFolderIds) {
            Folder folder = (Folder) drawer.getDrawerItem(id).getTag();
            if (folder.getServerId().equals(folderId)) {
                drawer.setSelection(id, false);
                return;
            }
        }
    }

    public DrawerLayout getLayout() {
        return drawer.getDrawerLayout();
    }

    public boolean isOpen() {
        return drawer.isDrawerOpen();
    }

    public void open() {
        drawer.openDrawer();
    }

    public void close() {
        drawer.closeDrawer();
    }

    public void lock() {
        drawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void unlock() {
        drawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }
}
