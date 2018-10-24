package com.fsck.k9.ui;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;

import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.mailstore.Folder;
import com.fsck.k9.ui.folders.FolderNameFormatter;
import com.fsck.k9.ui.settings.SettingsActivity;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.Drawer.OnDrawerItemClickListener;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

public class K9Drawer {
    // Bit shift for identifiers of user folders items, to leave space for other items
    private static final short DRAWER_FOLDER_SHIFT = 2;

    private static final long DRAWER_ID_UNIFIED_INBOX = 0;
    private static final long DRAWER_ID_PREFERENCES = 1;


    private final FolderNameFormatter folderNameFormatter = DI.get(FolderNameFormatter.class);

    private final Drawer drawer;
    private final MessageList parent;
    private int headerItemCount = 0;

    private int iconFolderInboxResId;
    private int iconFolderOutboxResId;
    private int iconFolderSentResId;
    private int iconFolderTrashResId;
    private int iconFolderDraftsResId;
    private int iconFolderArchiveResId;
    private int iconFolderSpamResId;
    private int iconFolderResId;

    private final List<Long> userFolderDrawerIds = new ArrayList<>();
    private String openedFolderServerId;


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

        addHeaderItems();
        addFooterItems();

        initializeFolderIcons();
    }

    private void addHeaderItems() {
        if (!K9.isHideSpecialAccounts()) {
            drawer.addItems(new PrimaryDrawerItem()
                            .withName(R.string.integrated_inbox_title)
                            .withIcon(getResId(R.attr.iconUnifiedInbox))
                            .withIdentifier(DRAWER_ID_UNIFIED_INBOX),
                    new DividerDrawerItem());

            headerItemCount += 2;
        }
    }

    private void addFooterItems() {
        drawer.addItems(new DividerDrawerItem(),
                new PrimaryDrawerItem()
                .withName(R.string.preferences_action)
                .withIcon(getResId(R.attr.iconActionSettings))
                .withIdentifier(DRAWER_ID_PREFERENCES)
                .withSelectable(false));
    }

    private void initializeFolderIcons() {
        iconFolderInboxResId = getResId(R.attr.iconFolderInbox);
        iconFolderOutboxResId = getResId(R.attr.iconFolderOutbox);
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
            case OUTBOX: return iconFolderOutboxResId;
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
                long id = drawerItem.getIdentifier();
                if (id == DRAWER_ID_UNIFIED_INBOX) {
                    parent.openUnifiedInbox();
                    return false;
                } else if (id == DRAWER_ID_PREFERENCES) {
                    SettingsActivity.launch(parent);
                    return false;
                }

                Folder folder = (Folder) drawerItem.getTag();
                parent.openFolder(folder.getServerId());
                return false;
            }
        };
    }

    public void setUserFolders(@Nullable List<Folder> folders) {
        clearUserFolders();

        if (folders == null) {
            return;
        }

        Collections.reverse(folders);

        long openedFolderDrawerId = -1;
        for (Folder folder : folders) {
            long drawerId = folder.getId() << DRAWER_FOLDER_SHIFT;
            drawer.addItemAtPosition(new PrimaryDrawerItem()
                    .withIcon(getFolderIcon(folder))
                    .withIdentifier(drawerId)
                    .withTag(folder)
                    .withName(getFolderDisplayName(folder)),
                    headerItemCount);

            userFolderDrawerIds.add(drawerId);

            if (folder.getServerId().equals(openedFolderServerId)) {
                openedFolderDrawerId = drawerId;
            }
        }

        if (openedFolderDrawerId != -1) {
            drawer.setSelection(openedFolderDrawerId, false);
        }
    }

    private void clearUserFolders() {
        for (long drawerId : userFolderDrawerIds) {
            drawer.removeItem(drawerId);
        }
        userFolderDrawerIds.clear();
    }

    public void selectFolder(String folderServerId) {
        openedFolderServerId = folderServerId;
        for (long drawerId : userFolderDrawerIds) {
            Folder folder = (Folder) drawer.getDrawerItem(drawerId).getTag();
            if (folder.getServerId().equals(folderServerId)) {
                drawer.setSelection(drawerId, false);
                return;
            }
        }
    }

    public void selectUnifiedInbox() {
        drawer.setSelection(DRAWER_ID_UNIFIED_INBOX, false);
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
