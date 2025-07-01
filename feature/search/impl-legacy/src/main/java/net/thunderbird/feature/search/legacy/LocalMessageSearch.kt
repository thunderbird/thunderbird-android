package net.thunderbird.feature.search.legacy

import android.os.Parcel
import android.os.Parcelable
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.MessageSearchSpecification
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition

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
@Suppress("TooManyFunctions")
class LocalMessageSearch : MessageSearchSpecification {
    var id: String = ""

    var isManualSearch: Boolean = false

    // since the uuid isn't in the message table it's not in the tree neither
    private val mAccountUuids: MutableSet<String> = HashSet()
    private var mConditions: SearchConditionTreeNode? = null

    /**
     * Gets the leafset of the related condition tree.
     *
     * @return All the leaf conditions as a set.
     */
    var leafSet: MutableSet<SearchConditionTreeNode> = HashSet()
        private set

    /**
     * Use this only if the search won't be saved. Saved searches need
     * a name!
     */
    constructor()

    /**
     * Add a new account to the search. When no accounts are
     * added manually we search all accounts on the device.
     *
     * @param uuid Uuid of the account to be added.
     */
    fun addAccountUuid(uuid: String) {
        mAccountUuids.add(uuid)
    }

    /**
     * Adds the provided node as the second argument of an AND
     * clause to this node.
     *
     * @param field Message table field to match against.
     * @param value Value to look for.
     * @param attribute Attribute to use when matching.
     */
    fun and(field: MessageSearchField, value: String, attribute: SearchAttribute) {
        and(SearchCondition(field, attribute, value))
    }

    /**
     * Adds the provided condition as the second argument of an AND
     * clause to this node.
     *
     * @param condition Condition to 'AND' with.
     * @return New top AND node, new root.
     */
    fun and(condition: SearchCondition): SearchConditionTreeNode {
        val tmp = SearchConditionTreeNode.Builder(condition).build()
        return and(tmp)
    }

    /**
     * Adds the provided node as the second argument of an AND
     * clause to this node.
     *
     * @param node Node to 'AND' with.
     * @return New top AND node, new root.
     */
    fun and(node: SearchConditionTreeNode): SearchConditionTreeNode {
        leafSet.addAll(node.getLeafSet())

        if (mConditions == null) {
            mConditions = node
            return node
        }

        mConditions = SearchConditionTreeNode.Builder(mConditions!!)
            .and(node)
            .build()

        return mConditions!!
    }

    /**
     * Adds the provided condition as the second argument of an OR
     * clause to this node.
     *
     * @param condition Condition to 'OR' with.
     * @return New top OR node, new root.
     */
    fun or(condition: SearchCondition): SearchConditionTreeNode {
        val tmp = SearchConditionTreeNode.Builder(condition).build()
        return or(tmp)
    }

    /**
     * Adds the provided node as the second argument of an OR
     * clause to this node.
     *
     * @param node Node to 'OR' with.
     * @return New top OR node, new root.
     */
    fun or(node: SearchConditionTreeNode): SearchConditionTreeNode {
        leafSet.addAll(node.getLeafSet())

        if (mConditions == null) {
            mConditions = node
            return node
        }

        mConditions = SearchConditionTreeNode.Builder(mConditions!!)
            .or(node)
            .build()

        return mConditions!!
    }

    /**
     * TODO
     * FOR NOW: And the folder with the root.
     *
     * Add the folder as another folder to search in. The folder
     * will be added AND to the root if no 'folder subtree' was found.
     * Otherwise the folder will be added OR to that tree.
     */
    fun addAllowedFolder(folderId: Long) {
        /*
         *  TODO find folder sub-tree
         *          - do and on root of it & rest of search
         *          - do or between folder nodes
         */
        mConditions = and(SearchCondition(MessageSearchField.FOLDER, SearchAttribute.EQUALS, folderId.toString()))
    }

    val folderIds: MutableList<Long>
        /*
         * TODO make this more advanced!
         * This is a temporary solution that does NOT WORK for
         * real searches because of possible extra conditions to a folder requirement.
         */
        get() {
            val results: MutableList<Long> = ArrayList()
            for (node in this.leafSet) {
                if (node.condition!!.field === MessageSearchField.FOLDER &&
                    node.condition.attribute == SearchAttribute.EQUALS
                ) {
                    results.add(node.condition.value.toLong())
                }
            }
            return results
        }

    /**
     * TODO THIS HAS TO GO!!!!
     * very dirty fix for remotesearch support atm
     */
    val remoteSearchArguments: String?
        get() {
            val leafSet = this.leafSet

            for (node in leafSet) {
                if (node.condition!!.field === MessageSearchField.SUBJECT ||
                    node.condition.field === MessageSearchField.SENDER
                ) {
                    return node.condition.value
                }
            }
            return null
        }

    override val accountUuids: Set<String>
        /**
         * Returns all the account uuids that this search will try to match against. Might be an empty array, in which
         * case all accounts should be included in the search.
         */
        get() = HashSet<String>(mAccountUuids)

    /**
     * Returns whether or not to search all accounts.
     *
     * @return `true` if all accounts should be searched.
     */
    fun searchAllAccounts(): Boolean {
        return (mAccountUuids.isEmpty())
    }

    override val conditions: SearchConditionTreeNode
        /**
         * Get the condition tree.
         *
         * @return The root node of the related conditions tree.
         */
        get() = mConditions ?: SearchConditionTreeNode.Builder(
            SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, ""),
        ).build()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeByte((if (this.isManualSearch) 1 else 0).toByte())
        dest.writeStringList(ArrayList<String?>(mAccountUuids))
        dest.writeParcelable(mConditions, flags)
    }

    constructor(input: Parcel) {
        id = input.readString() ?: ""
        this.isManualSearch = (input.readByte().toInt() == 1)
        mAccountUuids.addAll(input.createStringArrayList()!!)
        mConditions = input.readParcelable<SearchConditionTreeNode?>(LocalMessageSearch::class.java.getClassLoader())
        if (mConditions != null) {
            this.leafSet = mConditions!!.getLeafSet() as MutableSet<SearchConditionTreeNode>
        }
    }

    override fun toString(): String {
        return "LocalSearch(" +
            "id='" + id + '\'' +
            ", mManualSearch=" + this.isManualSearch +
            ", mAccountUuids=" + mAccountUuids +
            ", mConditions=" + mConditions +
            ", mLeafSet=" + this.leafSet +
            ')'
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LocalMessageSearch> = object : Parcelable.Creator<LocalMessageSearch> {
            override fun createFromParcel(`in`: Parcel): LocalMessageSearch {
                return LocalMessageSearch(`in`)
            }

            override fun newArray(size: Int): Array<LocalMessageSearch?> {
                return arrayOfNulls(size)
            }
        }
    }
}
