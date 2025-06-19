package net.thunderbird.feature.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import net.thunderbird.feature.search.api.SearchAttribute;
import net.thunderbird.feature.search.api.SearchCondition;
import net.thunderbird.feature.search.api.SearchField;
import net.thunderbird.feature.search.api.MessageSearchSpecification;


/**
 * This class represents a local search.
 *
 * Removing conditions could be done through matching there unique id in the leafset and then
 * removing them from the tree.
 *
 * TODO implement a complete addAllowedFolder method
 * TODO conflicting conditions check on add
 * TODO duplicate condition checking?
 * TODO assign each node a unique id that's used to retrieve it from the leafset and remove.
 *
 */

public class LocalMessageSearch implements MessageSearchSpecification {

    private String id;
    private boolean mManualSearch = false;

    // since the uuid isn't in the message table it's not in the tree neither
    private Set<String> mAccountUuids = new HashSet<>();
    private SearchConditionTreeNode mConditions = null;
    private Set<SearchConditionTreeNode> mLeafSet = new HashSet<>();


    ///////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////
    /**
     * Use this only if the search won't be saved. Saved searches need
     * a name!
     */
    public LocalMessageSearch() {}

    ///////////////////////////////////////////////////////////////
    // Public manipulation methods
    ///////////////////////////////////////////////////////////////
    /**
     * Set the ID of the search. This is used to identify a unified inbox
     * search
     *
     * @param id ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Add a new account to the search. When no accounts are
     * added manually we search all accounts on the device.
     *
     * @param uuid Uuid of the account to be added.
     */
    public void addAccountUuid(String uuid) {
        mAccountUuids.add(uuid);
    }

    /**
     * Adds the provided node as the second argument of an AND
     * clause to this node.
     *
     * @param field Message table field to match against.
     * @param value Value to look for.
     * @param attribute Attribute to use when matching.
     */
    public void and(SearchField field, String value, SearchAttribute attribute) {
        and(new SearchCondition(field, attribute, value));
    }

    /**
     * Adds the provided condition as the second argument of an AND
     * clause to this node.
     *
     * @param condition Condition to 'AND' with.
     * @return New top AND node, new root.
     */
    public SearchConditionTreeNode and(SearchCondition condition) {
        SearchConditionTreeNode tmp = new SearchConditionTreeNode(condition);
        return and(tmp);
    }

    /**
     * Adds the provided node as the second argument of an AND
     * clause to this node.
     *
     * @param node Node to 'AND' with.
     * @return New top AND node, new root.
     */
    public SearchConditionTreeNode and(SearchConditionTreeNode node) {
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
    public SearchConditionTreeNode or(SearchCondition condition) {
        SearchConditionTreeNode tmp = new SearchConditionTreeNode(condition);
        return or(tmp);
    }

    /**
     * Adds the provided node as the second argument of an OR
     * clause to this node.
     *
     * @param node Node to 'OR' with.
     * @return New top OR node, new root.
     */
    public SearchConditionTreeNode or(SearchConditionTreeNode node) {
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
     */
    public void addAllowedFolder(long folderId) {
        /*
         *  TODO find folder sub-tree
         *          - do and on root of it & rest of search
         *          - do or between folder nodes
         */
        mConditions = and(new SearchCondition(SearchField.FOLDER, SearchAttribute.EQUALS, Long.toString(folderId)));
    }

    /*
     * TODO make this more advanced!
     * This is a temporary solution that does NOT WORK for
     * real searches because of possible extra conditions to a folder requirement.
     */
    public List<Long> getFolderIds() {
        List<Long> results = new ArrayList<>();
        for (SearchConditionTreeNode node : mLeafSet) {
            if (node.condition.field == SearchField.FOLDER &&
                    node.condition.attribute == SearchAttribute.EQUALS) {
                results.add(Long.valueOf(node.condition.value));
            }
        }
        return results;
    }

    /**
     * Gets the leafset of the related condition tree.
     *
     * @return All the leaf conditions as a set.
     */
    public Set<SearchConditionTreeNode> getLeafSet() {
        return mLeafSet;
    }

    ///////////////////////////////////////////////////////////////
    // Public accessor methods
    ///////////////////////////////////////////////////////////////
    /**
     * TODO THIS HAS TO GO!!!!
     * very dirty fix for remotesearch support atm
     */
    public String getRemoteSearchArguments() {
        Set<SearchConditionTreeNode> leafSet = getLeafSet();
        if (leafSet == null) {
            return null;
        }

        for (SearchConditionTreeNode node : leafSet) {
            if (node.getCondition().field == SearchField.SUBJECT ||
                    node.getCondition().field == SearchField.SENDER ) {
                return node.getCondition().value;
            }
        }
        return null;
    }

    /**
     * Returns the ID of the search
     *
     * @return The ID of the search
     */
    public String getId() {
        return (id == null) ? "" : id;
    }

    public boolean isManualSearch() {
        return mManualSearch;
    }

    public void setManualSearch(boolean manualSearch) {
        mManualSearch = manualSearch;
    }

    /**
     * Returns all the account uuids that this search will try to match against. Might be an empty array, in which case
     * all accounts should be included in the search.
     */
    @Override
    public Set<String> getAccountUuids() {
        return new HashSet<>(mAccountUuids);
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
    public SearchConditionTreeNode getConditions() {
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
        dest.writeString(id);
        dest.writeByte((byte) (mManualSearch ? 1 : 0));
        dest.writeStringList(new ArrayList<>(mAccountUuids));
        dest.writeParcelable(mConditions, flags);
    }

    public static final Parcelable.Creator<LocalMessageSearch> CREATOR =
            new Parcelable.Creator<LocalMessageSearch>() {

        @Override
        public LocalMessageSearch createFromParcel(Parcel in) {
            return new LocalMessageSearch(in);
        }

        @Override
        public LocalMessageSearch[] newArray(int size) {
            return new LocalMessageSearch[size];
        }
    };

    public LocalMessageSearch(Parcel in) {
        id = in.readString();
        mManualSearch = (in.readByte() == 1);
        mAccountUuids.addAll(in.createStringArrayList());
        mConditions = in.readParcelable(LocalMessageSearch.class.getClassLoader());
        if (mConditions != null) {
            mLeafSet = mConditions.getLeafSet();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "LocalSearch(" +
            "id='" + id + '\'' +
            ", mManualSearch=" + mManualSearch +
            ", mAccountUuids=" + mAccountUuids +
            ", mConditions=" + mConditions +
            ", mLeafSet=" + mLeafSet +
            ')';
    }
}
