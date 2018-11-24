package com.fsck.k9.ui;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;
import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.MessageList;
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
import com.mikepenz.materialdrawer.icons.MaterialDrawerFont;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;
import java.util.List;


public class K9Drawer {
    // Bit shift for identifiers of user folders items, to leave space for other items
    private static final short DRAWER_FOLDER_SHIFT = 2;
    private static final short DRAWER_ACCOUNT_SHIFT = 16;

    private static final long DRAWER_ID_UNIFIED_INBOX = 0;
    private static final long DRAWER_ID_PREFERENCES = 1;


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
    private final List<Long> userAccountDrawerIds = new ArrayList<>();
    private boolean unifiedInboxSelected;
    private String openedFolderServerId;

    private final Preferences preferences = DI.get(Preferences.class);

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
                .withAccountHeader(buildAccountHeader())
                .build();

        addFooterItems();

        initializeFolderIcons();
    }

    private AccountHeader buildAccountHeader() {
        AccountHeaderBuilder headerBuilder = new AccountHeaderBuilder()
                .withActivity(parent)
                //.withTextColorRes(R.color.primary_text_secondary_when_activated_material)
                .withTranslucentStatusBar(false);

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

        List <Account> accounts = preferences.getAccounts();
        for (int i = preferences.getAccounts().size() - 1; i >= 0; i--) {
            Account account = accounts.get(i);
            long drawerId = (account.getAccountNumber()+1) << DRAWER_ACCOUNT_SHIFT;

            headerBuilder.addProfiles(new ProfileDrawerItem()
                    .withNameShown(true)
                    .withName(account.getDescription())
                    .withEmail(account.getEmail())
                    .withIcon(new IconicsDrawable(parent, MaterialDrawerFont.Icon.mdf_person)
                            .colorRes(R.color.material_drawer_background).backgroundColor(account.getChipColor())
                            .sizeDp(56).paddingDp(16))
                    .withIdentifier(drawerId)
                    .withSetSelected(false)
                    .withTag(account)
            );
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
                            updateUserAccountsAndFolders(account);
                            Accounts.openRealAccount(account, parent);
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

    public void updateUserAccountsAndFolders(Account account) {
        if (account == null) {
            selectUnifiedInbox();
        } else {
            accountHeader.setActiveProfile((account.getAccountNumber()+1) << DRAWER_ACCOUNT_SHIFT);
            ViewModelProvider viewModelProvider = ViewModelProviders.of(parent, new MessageListViewModelFactory());
            MessageListViewModel viewModel = viewModelProvider.get(MessageListViewModel.class);
            viewModel.getFolders(account).observe(parent, new Observer<List<Folder>>() {
                @Override
                public void onChanged(@Nullable List<Folder> folders) {
                    setUserFolders(folders);
                }
            });
        }
    }

    private OnDrawerItemClickListener createItemClickListener() {
        return new OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                long id = drawerItem.getIdentifier();
                if (id == DRAWER_ID_PREFERENCES) {
                    SettingsActivity.launch(parent);
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
        } else if (unifiedInboxSelected) {
            selectUnifiedInbox();
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
    }

    public void selectUnifiedInbox() {
        unifiedInboxSelected = true;
        openedFolderServerId = null;
        accountHeader.setActiveProfile(DRAWER_ID_UNIFIED_INBOX);
        clearUserFolders();
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
