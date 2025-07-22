package net.thunderbird.feature.search.legacy

import kotlinx.serialization.Serializable
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
 */
@Serializable
@Suppress("TooManyFunctions")
class LocalMessageSearch : MessageSearchSpecification {
    var id: String = ""
    var isManualSearch: Boolean = false

    // since the uuid isn't in the message table it's not in the tree neither
    private val accountUuidSet: MutableSet<String> = HashSet()
    private var conditionsRoot: SearchConditionTreeNode? = null

    /**
     * Gets the leafset of the related condition tree.
     *
     * @return All the leaf conditions as a set.
     */
    var leafSet: MutableSet<SearchConditionTreeNode> = HashSet()
        private set

    /**
     * Add a new account to the search. When no accounts are
     * added manually we search all accounts on the device.
     *
     * @param uuid Uuid of the account to be added.
     */
    fun addAccountUuid(uuid: String) {
        accountUuidSet.add(uuid)
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
        val node = SearchConditionTreeNode.Builder(condition).build()
        return and(node)
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

        conditionsRoot = conditionsRoot?.let {
            SearchConditionTreeNode.Builder(it)
                .and(node)
                .build()
        } ?: node

        return conditionsRoot!!
    }

    /**
     * Adds the provided condition as the second argument of an OR
     * clause to this node.
     *
     * @param condition Condition to 'OR' with.
     * @return New top OR node, new root.
     */
    fun or(condition: SearchCondition): SearchConditionTreeNode {
        val node = SearchConditionTreeNode.Builder(condition).build()
        return or(node)
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

        conditionsRoot = conditionsRoot?.let {
            SearchConditionTreeNode.Builder(it)
                .or(node)
                .build()
        } ?: node

        return conditionsRoot!!
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
        conditionsRoot = and(SearchCondition(MessageSearchField.FOLDER, SearchAttribute.EQUALS, folderId.toString()))
    }

    /**
     * TODO make this more advanced!
     * This is a temporary solution that does NOT WORK for
     * real searches because of possible extra conditions to a folder requirement.
     */
    val folderIds: List<Long>
        get() = leafSet
            .mapNotNull { node ->
                node.condition?.takeIf {
                    it.field == MessageSearchField.FOLDER && it.attribute == SearchAttribute.EQUALS
                }?.value?.toLong()
            }

    /**
     * Safely gets a folder ID at the specified index, returning null if the index is out of bounds.
     * This helps prevent IndexOutOfBoundsException when accessing folder IDs.
     *
     * @param index The index of the folder ID to get
     * @return The folder ID at the specified index, or null if the index is out of bounds
     */
    fun getFolderIdAtIndexOrNull(index: Int): Long? {
        return folderIds.getOrNull(index)
    }

    /**
     * TODO THIS HAS TO GO!!!!
     * very dirty fix for remotesearch support atm
     */
    val remoteSearchArguments: String?
        get() = leafSet.firstNotNullOfOrNull { node ->
            node.condition?.takeIf {
                it.field == MessageSearchField.SUBJECT || it.field == MessageSearchField.SENDER
            }?.value
        }

    /**
     * Returns all the account uuids that this search will try to match against. Might be an empty array, in which
     * case all accounts should be included in the search.
     */
    override val accountUuids: Set<String>
        get() = accountUuidSet.toSet()

    /**
     * Returns whether or not to search all accounts.
     *
     * @return `true` if all accounts should be searched.
     */
    fun searchAllAccounts(): Boolean = accountUuidSet.isEmpty()

    /**
     * Get the condition tree.
     *
     * @return The root node of the related conditions tree.
     */
    override val conditions: SearchConditionTreeNode
        get() = conditionsRoot ?: SearchConditionTreeNode.Builder(
            SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, ""),
        ).build()

    override fun toString(): String = buildString {
        append("LocalSearch(")
        append("id='").append(id).append("', ")
        append("isManualSearch=").append(isManualSearch).append(", ")
        append("accountUuidSet=").append(accountUuidSet).append(", ")
        append("conditionsRoot=").append(conditionsRoot).append(", ")
        append("leafSet=").append(leafSet)
        append(")")
    }
}
