package app.k9mail.legacy.search.api;

import android.os.Parcelable;

import app.k9mail.legacy.search.ConditionsTreeNode;


public interface SearchSpecification extends Parcelable {

    /**
     * Get all the uuids of accounts this search acts on.
     * @return Array of uuids.
     */
    String[] getAccountUuids();

    /**
     * Returns the root node of the condition tree accompanying
     * the search.
     *
     * @return Root node of conditions tree.
     */
    ConditionsTreeNode getConditions();
}
