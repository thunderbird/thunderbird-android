package com.fsck.k9.ui;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;
import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mailstore.Folder;
import com.fsck.k9.ui.folders.FolderNameFormatter;
import com.fsck.k9.ui.messagelist.MessageListViewModel;
import com.fsck.k9.ui.messagelist.MessageListViewModelFactory;
import com.fsck.k9.ui.settings.SettingsActivity;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.Drawer.OnDrawerItemClickListener;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class K9Drawer {
    // Bit shift for identifiers of user folders items, to leave space for other items
    private static final short DRAWER_FOLDER_SHIFT = 2;
    private static final short DRAWER_ACCOUNT_SHIFT = 16;

    private static final long DRAWER_ID_UNIFIED_INBOX = 0;
    private static final long DRAWER_ID_PREFERENCES = 1;
    private static final long DRAWER_ID_FOLDERS = 2;


    private final FolderNameFormatter folderNameFormatter = DI.get(FolderNameFormatter.class);

    private final Drawer drawer;
    private AccountHeader accountHeader;
    private final MessageList parent;
    private int headerItemCount = 1;

    private int iconFolderInboxResId;
    private int iconFolderOutboxResId;
    private int iconFolderSentResId;
    private int iconFolderTrashResId;
    private int iconFolderDraftsResId;
    private int iconFolderArchiveResId;
    private int iconFolderSpamResId;
    private int iconFolderResId;

    private final List<Long> userFolderDrawerIds = new ArrayList<>();
    private boolean unifiedInboxSelected;
    private String openedFolderServerId;

    private final Preferences preferences = DI.get(Preferences.class);

    public K9Drawer(MessageList parent, Bundle savedInstanceState) {
        this.parent = parent;

        initializeFolderIcons();

        drawer = new DrawerBuilder()
                .withActivity(parent)
                .withOnDrawerItemClickListener(createItemClickListener())
                .withOnDrawerListener(parent.createOnDrawerListener())
                .withSavedInstance(savedInstanceState)
                .withAccountHeader(buildAccountHeader())
                .build();

        addFooterItems();
    }

    private AccountHeader buildAccountHeader() {
        AccountHeaderBuilder headerBuilder = new AccountHeaderBuilder()
                .withActivity(parent)
                .withHeaderBackground(R.drawable.drawer_header_background);

        if (!K9.isHideSpecialAccounts()) {
            headerBuilder.addProfiles(new ProfileDrawerItem()
                    .withNameShown(true)
                    .withName(R.string.integrated_inbox_title)
                    .withEmail(parent.getString(R.string.integrated_inbox_detail))
                    .withIcon(new IconicsDrawable(parent, FontAwesome.Icon.faw_users)
                            .colorRes(R.color.material_drawer_background).backgroundColor(Color.GRAY)
                            .sizeDp(56).paddingDp(8))
                    .withSetSelected(unifiedInboxSelected)
                    .withIdentifier(DRAWER_ID_UNIFIED_INBOX)
            );
        }

        HashSet <Uri> photoUris = new HashSet<Uri>();

        List <Account> accounts = preferences.getAccounts();
        for (int i = 0; i < preferences.getAccounts().size(); i++) {
            Account account = accounts.get(i);
            long drawerId = (account.getAccountNumber()+1) << DRAWER_ACCOUNT_SHIFT;

            ProfileDrawerItem  pdi = new ProfileDrawerItem()
                    .withNameShown(true)
                    .withName(account.getDescription())
                    .withEmail(account.getEmail())
                    .withIdentifier(drawerId)
                    .withSetSelected(false)
                    .withTag(account);

            Uri photoUri = Contacts.getInstance(parent).getPhotoUri(account.getEmail());
            if (photoUri != null && !photoUris.contains(photoUri)) {
                photoUris.add(photoUri);
                pdi.withIcon(photoUri);
            } else {
                pdi.withIcon(new IconicsDrawable(parent, FontAwesome.Icon.faw_user_alt)
                        .colorRes(R.color.material_drawer_background).backgroundColor(account.getChipColor())
                        .sizeDp(56).paddingDp(14));
            }
            headerBuilder.addProfiles(pdi);
        }

        accountHeader = headerBuilder
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        if (profile.getIdentifier() == DRAWER_ID_UNIFIED_INBOX) {
                            parent.openUnifiedInbox();
                            return false;
                        } else {
                            Account account = (Account) ((ProfileDrawerItem) profile).getTag();
                            parent.openRealAccount(account);
                            updateUserAccountsAndFolders(account);
                            return false;
                        }
                    }
                })
                .build();
        return accountHeader;
    }

    private void addFooterItems() {
        drawer.addItems(new DividerDrawerItem(),
                new PrimaryDrawerItem()
                        .withName(R.string.folders_action)
                        .withIcon(iconFolderResId)
                        .withIdentifier(DRAWER_ID_FOLDERS)
                        .withSelectable(false),
                new PrimaryDrawerItem()
                        .withName(R.string.preferences_action)
                        .withIcon(getResId(R.attr.iconActionSettings))
                        .withIdentifier(DRAWER_ID_PREFERENCES)
                        .withSelectable(false)
        );
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

    public void updateUserAccountsAndFolders(Account account) {
        if (account == null) {
            selectUnifiedInbox();
        } else {
            unifiedInboxSelected = false;
            accountHeader.setActiveProfile((account.getAccountNumber()+1) << DRAWER_ACCOUNT_SHIFT);
            accountHeader.getHeaderBackgroundView().setColorFilter(account.getChipColor(), PorterDuff.Mode.MULTIPLY);
            ViewModelProvider viewModelProvider = ViewModelProviders.of(parent, new MessageListViewModelFactory());
            MessageListViewModel viewModel = viewModelProvider.get(MessageListViewModel.class);
            viewModel.getFolders(account).observe(parent, new Observer<List<Folder>>() {
                @Override
                public void onChanged(@Nullable List<Folder> folders) {
                    setUserFolders(folders);
                }
            });
            updateFolderSettingsItem();
        }
    }

    private void updateFolderSettingsItem() {
        IDrawerItem drawerItem = drawer.getDrawerItem(DRAWER_ID_FOLDERS);
        drawerItem.withEnabled(!unifiedInboxSelected);
        drawer.updateItem(drawerItem);
    }

    private OnDrawerItemClickListener createItemClickListener() {
        return new OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                long id = drawerItem.getIdentifier();
                if (id == DRAWER_ID_PREFERENCES) {
                    SettingsActivity.launch(parent);
                    return false;
                } else if (id == DRAWER_ID_FOLDERS) {
                    parent.openFolderSettings();
                    return false;
                } else {
                    Folder folder = (Folder) drawerItem.getTag();
                    parent.openFolder(folder.getServerId());
                    return false;
                }
            }
        };
    }

    public void setUserFolders(@Nullable List<Folder> folders) {
        clearUserFolders();

        if (folders == null) {
            return;
        }

        long openedFolderDrawerId = -1;
        for (int i = folders.size() - 1; i >= 0; i--) {
            Folder folder = folders.get(i);
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
        unifiedInboxSelected = false;
        openedFolderServerId = folderServerId;
        for (long drawerId : userFolderDrawerIds) {
            Folder folder = (Folder) drawer.getDrawerItem(drawerId).getTag();
            if (folder.getServerId().equals(folderServerId)) {
                drawer.setSelection(drawerId, false);
                return;
            }
        }
        updateFolderSettingsItem();
    }

    public void selectUnifiedInbox() {
        unifiedInboxSelected = true;
        openedFolderServerId = null;
        accountHeader.setActiveProfile(DRAWER_ID_UNIFIED_INBOX);
        accountHeader.getHeaderBackgroundView().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
        clearUserFolders();
        updateFolderSettingsItem();
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
