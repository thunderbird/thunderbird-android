package net.thunderbird.feature.search.api

import android.os.Parcelable
import net.thunderbird.feature.search.SearchConditionTreeNode

/**
 * Represents a search specification that defines the accounts and conditions
 * for searching messages.
 *
 * This interface is used to encapsulate the details of a search operation,
 * including which accounts to search and the conditions that must be met
 * for messages to be included in the search results.
 */
interface MessageSearchSpecification : Parcelable {
    /**
     * Get all the uuids of accounts this search acts on.
     * @return Set of uuids.
     */
    val accountUuids: Set<String>

    /**
     * Returns the root node of the condition tree accompanying
     * the search.
     *
     * @return Root node of conditions tree.
     */
    val conditions: SearchConditionTreeNode
}
