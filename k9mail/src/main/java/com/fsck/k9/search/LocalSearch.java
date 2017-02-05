package com.fsck.k9.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents a local search.
 *
 * Removing conditions could be done through matching there unique id in the leafset and then
 * removing them from the tree.
 *
 * TODO implement a complete addAllowedFolder method
 * TODO conflicting conditions check on add
 * TODO duplicate condition checking?
 * TODO assign each node a unique id that's used to retrieve it from the leaveset and remove.
 *
 */

public class LocalSearch implements SearchSpecification {

    private String mName;
    private boolean mPredefined;
    private boolean mManualSearch = false;

    // since the uuid isn't in the message table it's not in the tree neither
    private Set<String> mAccountUuids = new HashSet<String>();
    private ConditionsTreeNode mConditions = null;
    private Set<ConditionsTreeNode> mLeafSet = new HashSet<ConditionsTreeNode>();


    ///////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////
    /**
     * Use this only if the search won't be saved. Saved searches need
     * a name!
     */
    public LocalSearch() {}

    /**
     *
     * @param name
     */
    public LocalSearch(String name) {
        this.mName = name;
    }

    /**
     * Use this constructor when you know what you'r doing. Normally it's only used
     * when restoring these search objects from the database.
     *
     * @param name Name of the search
     * @param searchConditions SearchConditions, may contains flags and folders
     * @param accounts Relative Account's uuid's
     * @param predefined Is this a predefined search or a user created one?
     */
    protected LocalSearch(String name, ConditionsTreeNode searchConditions,
            String accounts, boolean predefined) {
        this(name);
        mConditions = searchConditions;
        mPredefined = predefined;
        mLeafSet = new HashSet<ConditionsTreeNode>();
        if (mConditions != null) {
            mLeafSet.addAll(mConditions.getLeafSet());
        }

        // initialize accounts
        if (accounts != null) {
            for (String account : accounts.split(",")) {
                mAccountUuids.add(account);
            }
        } else {
            // impossible but still not unrecoverable
        }
    }

    @Override
    public LocalSearch clone() {
        ConditionsTreeNode conditions = (mConditions == null) ? null : mConditions.cloneTree();

        LocalSearch copy = new LocalSearch(mName, conditions, null, mPredefined);
        copy.mManualSearch = mManualSearch;
        copy.mAccountUuids = new HashSet<String>(mAccountUuids);

        return copy;
    }

    ///////////////////////////////////////////////////////////////
    // Public manipulation methods
    ///////////////////////////////////////////////////////////////
    /**
     * Sets the name of the saved search. If one existed it will
     * be overwritten.
     *
     * @param name Name to be set.
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Add a new account to the search. When no accounts are
     * added manually we search all accounts on the device.
     *
     * @param uuid Uuid of the account to be added.
     */
    public void addAccountUuid(String uuid) {
        if (uuid.equals(ALL_ACCOUNTS)) {
            mAccountUuids.clear();
            return;
        }
        mAccountUuids.add(uuid);
    }

    /**
     * Adds all the account uuids in the provided array to
     * be matched by the seach.
     *
     * @param accountUuids
     */
    public void addAccountUuids(String[] accountUuids) {
        for (String acc : accountUuids) {
            addAccountUuid(acc);
        }
    }

    /**
     * Removes an account UUID from the current search.
     *
     * @param uuid Account UUID to remove.
     * @return True if removed, false otherwise.
     */
    public boolean removeAccountUuid(String uuid) {
        return mAccountUuids.remove(uuid);
    }

    /**
     * Adds the provided node as the second argument of an AND
     * clause to this node.
     *
     * @param field Message table field to match against.
     * @param value Value to look for.
     * @param attribute Attribute to use when matching.
     */
    public void and(SearchField field, String value, Attribute attribute) {
        and(new SearchCondition(field, attribute, value));
    }

    /**
     * Adds the provided condition as the second argument of an AND
     * clause to this node.
     *
     * @param condition Condition to 'AND' with.
     * @return New top AND node, new root.
     */
    public ConditionsTreeNode and(SearchCondition condition) {
        try {
            ConditionsTreeNode tmp = new ConditionsTreeNode(condition);
            return and(tmp);
        } catch (Exception e) {
            // impossible
            return null;
        }
    }

    /**
     * Adds the provided node as the second argument of an AND
     * clause to this node.
     *
     * @param node Node to 'AND' with.
     * @return New top AND node, new root.
     * @throws Exception
     */
    public ConditionsTreeNode and(ConditionsTreeNode node) throws Exception {
        mLeafSet.addAll(node.getLeafSet());

        if (mConditions == null) {
            mConditions = node;
            return node;
        }

        mConditions = mConditions.and(node);
        return mConditions;
    }

    /**
     * Adds the provided condition as the second argument of an OR
     * clause to this node.
     *
     * @param condition Condition to 'OR' with.
     * @return New top OR node, new root.
     */
    public ConditionsTreeNode or(SearchCondition condition) {
        try {
            ConditionsTreeNode tmp = new ConditionsTreeNode(condition);
            return or(tmp);
        } catch (Exception e) {
            // impossible
            return null;
        }
    }

    /**
     * Adds the provided node as the second argument of an OR
     * clause to this node.
     *
     * @param node Node to 'OR' with.
     * @return New top OR node, new root.
     * @throws Exception
     */
    public ConditionsTreeNode or(ConditionsTreeNode node) throws Exception {
        mLeafSet.addAll(node.getLeafSet());

        if (mConditions == null) {
            mConditions = node;
            return node;
        }

        mConditions = mConditions.or(node);
        return mConditions;
    }

    /**
     * TODO
     * FOR NOW: And the folder with the root.
     *
     * Add the folder as another folder to search in. The folder
     * will be added AND to the root if no 'folder subtree' was found.
     * Otherwise the folder will be added OR to that tree.
     *
     * @param name Name of the folder to add.
     */
    public void addAllowedFolder(String name) {
        /*
         *  TODO find folder sub-tree
         *          - do and on root of it & rest of search
         *          - do or between folder nodes
         */
        mConditions = and(new SearchCondition(SearchField.FOLDER, Attribute.EQUALS, name));
    }

    /*
     * TODO make this more advanced!
     * This is a temporarely solution that does NOT WORK for
     * real searches because of possible extra conditions to a folder requirement.
     */
    public List<String> getFolderNames() {
        List<String> results = new ArrayList<String>();
        for (ConditionsTreeNode node : mLeafSet) {
            if (node.mCondition.field == SearchField.FOLDER &&
                    node.mCondition.attribute == Attribute.EQUALS) {
                results.add(node.mCondition.value);
            }
        }
        return results;
    }

    /**
     * Gets the leafset of the related condition tree.
     *
     * @return All the leaf conditions as a set.
     */
    public Set<ConditionsTreeNode> getLeafSet() {
        return mLeafSet;
    }

    ///////////////////////////////////////////////////////////////
    // Public accesor methods
    ///////////////////////////////////////////////////////////////
    /**
     * TODO THIS HAS TO GO!!!!
     * very dirty fix for remotesearch support atm
     */
    public String getRemoteSearchArguments() {
        Set<ConditionsTreeNode> leafSet = getLeafSet();
        if (leafSet == null) {
            return null;
        }

        for (ConditionsTreeNode node : leafSet) {
            if (node.getCondition().field == SearchField.SUBJECT ||
                    node.getCondition().field == SearchField.SENDER ) {
                return node.getCondition().value;
            }
        }
        return null;
    }

    /**
     * Returns the name of the saved search.
     *
     * @return Name of the search.
     */
    @Override
    public String getName() {
        return (mName == null) ? "" : mName;
    }

    /**
     * Checks if this search was hard coded and shipped with K-9
     *
     * @return True is search was shipped with K-9
     */
    public boolean isPredefined() {
        return mPredefined;
    }

    public boolean isManualSearch() {
        return mManualSearch;
    }

    public void setManualSearch(boolean manualSearch) {
        mManualSearch = manualSearch;
    }

    /**
     * Returns all the account uuids that this search will try to
     * match against.
     *
     * @return Array of account uuids.
     */
    @Override
    public String[] getAccountUuids() {
        if (mAccountUuids.isEmpty()) {
            return new String[] { SearchSpecification.ALL_ACCOUNTS };
        }

        String[] tmp = new String[mAccountUuids.size()];
        mAccountUuids.toArray(tmp);
        return tmp;
    }

    /**
     * Returns whether or not to search all accounts.
     *
     * @return {@code true} if all accounts should be searched.
     */
    public boolean searchAllAccounts() {
        return (mAccountUuids.isEmpty());
    }

    /**
     * Get the condition tree.
     *
     * @return The root node of the related conditions tree.
     */
    @Override
    public ConditionsTreeNode getConditions() {
        return mConditions;
    }

    ///////////////////////////////////////////////////////////////
    // Parcelable
    ///////////////////////////////////////////////////////////////
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeByte((byte) (mPredefined ? 1 : 0));
        dest.writeByte((byte) (mManualSearch ? 1 : 0));
        dest.writeStringList(new ArrayList<String>(mAccountUuids));
        dest.writeParcelable(mConditions, flags);
    }

    public static final Parcelable.Creator<LocalSearch> CREATOR =
            new Parcelable.Creator<LocalSearch>() {

        @Override
        public LocalSearch createFromParcel(Parcel in) {
            return new LocalSearch(in);
        }

        @Override
        public LocalSearch[] newArray(int size) {
            return new LocalSearch[size];
        }
    };

    public LocalSearch(Parcel in) {
        mName = in.readString();
        mPredefined = (in.readByte() == 1);
        mManualSearch = (in.readByte() == 1);
        mAccountUuids.addAll(in.createStringArrayList());
        mConditions = in.readParcelable(LocalSearch.class.getClassLoader());
        mLeafSet = (mConditions == null) ? null : mConditions.getLeafSet();
    }
}