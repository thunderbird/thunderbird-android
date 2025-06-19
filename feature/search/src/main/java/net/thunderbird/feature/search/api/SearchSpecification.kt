package net.thunderbird.feature.search.api

import android.os.Parcelable
import net.thunderbird.feature.search.ConditionsTreeNode

interface SearchSpecification : Parcelable {
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
    val conditions: ConditionsTreeNode
}
