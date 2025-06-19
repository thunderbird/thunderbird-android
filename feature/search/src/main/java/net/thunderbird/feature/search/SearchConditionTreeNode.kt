package net.thunderbird.feature.search

import android.os.Parcel
import android.os.Parcelable
import net.thunderbird.feature.search.api.SearchCondition

/**
 * This class stores search conditions. It's basically a boolean expression binary tree.
 * The output will be SQL queries ( obtained by traversing inorder ).
 *
 * TODO removing conditions from the tree
 * TODO implement NOT as a node again
 */
class SearchConditionTreeNode : Parcelable {
    enum class Operator {
        AND,
        OR,
        CONDITION,
    }

    @JvmField
    var mLeft: SearchConditionTreeNode? = null

    @JvmField
    var mRight: SearchConditionTreeNode? = null
    var mParent: SearchConditionTreeNode?

    /*
     * If mValue isn't CONDITION then mCondition contains a real
     * condition, otherwise it's null.
     */
    @JvmField
    var mValue: Operator
    var condition: SearchCondition?

    constructor(condition: SearchCondition?) {
        mParent = null
        this.condition = condition
        mValue = Operator.CONDITION
    }

    constructor(parent: SearchConditionTreeNode?, op: Operator) {
        mParent = parent
        mValue = op
        this.condition = null
    }

    /**
     * Adds the expression as the second argument of an AND
     * clause to this node.
     *
     * @param expr Expression to 'AND' with.
     * @ return New top AND node.
     */
    fun and(expr: SearchConditionTreeNode): SearchConditionTreeNode {
        return add(expr, Operator.AND)
    }

    /**
     * Convenience method.
     * Adds the provided condition as the second argument of an AND
     * clause to this node.
     *
     * @param condition Condition to 'AND' with.
     * @return New top AND node, new root.
     */
    fun and(condition: SearchCondition?): SearchConditionTreeNode {
        val tmp = SearchConditionTreeNode(condition)
        return and(tmp)
    }

    /**
     * Adds the expression as the second argument of an OR
     * clause to this node.
     *
     * @param expr Expression to 'OR' with.
     * @return New top OR node.
     */
    fun or(expr: SearchConditionTreeNode): SearchConditionTreeNode {
        return add(expr, Operator.OR)
    }

    /**
     * Convenience method.
     * Adds the provided condition as the second argument of an OR
     * clause to this node.
     *
     * @param condition Condition to 'OR' with.
     * @return New top OR node, new root.
     */
    fun or(condition: SearchCondition?): SearchConditionTreeNode {
        val tmp = SearchConditionTreeNode(condition)
        return or(tmp)
    }

    /**
     * Returns the condition stored in this node.
     * @ return Condition stored in the node.
     */
    val leafSet: MutableSet<SearchConditionTreeNode?>
        /**
         * Get a set of all the leaves in the tree.
         * @return Set of all the leaves.
         */
        get() {
            val leafSet: MutableSet<SearchConditionTreeNode?> =
                HashSet<SearchConditionTreeNode?>()
            return getLeafSet(leafSet)
        }

    /**
     * Adds two new ConditionTreeNodes, one for the operator and one for the
     * new condition. The current node will end up on the same level as the
     * one provided in the arguments, they will be siblings. Their common
     * parent node will be one containing the operator provided in the arguments.
     * The method will update all the required references so the tree ends up in
     * a valid state.
     *
     * This method only supports node arguments with a null parent node.
     *
     * @param node Node to add.
     * @param op Operator that will connect the new node with this one.
     * @ return New parent node, containing the operator .
     * @throws IllegalArgumentException Throws when the provided new node does not have a null parent.
     */
    private fun add(node: SearchConditionTreeNode, op: Operator): SearchConditionTreeNode {
        require(node.mParent == null) { "Can only add new expressions from root node down." }

        val tmpNode = SearchConditionTreeNode(mParent, op)
        tmpNode.mLeft = this
        tmpNode.mRight = node

        if (mParent != null) {
            mParent!!.updateChild(this, tmpNode)
        }

        this.mParent = tmpNode
        node.mParent = tmpNode

        return tmpNode
    }

    /**
     * Helper method that replaces a child of the current node with a new node.
     * If the provided old child node was the left one, left will be replaced with
     * the new one. Same goes for the right one.
     *
     * @param oldChild Old child node to be replaced.
     * @param newChild New child node.
     */
    private fun updateChild(oldChild: SearchConditionTreeNode?, newChild: SearchConditionTreeNode?) {
        // we can compare objects id's because this is the desired behaviour in this case
        if (mLeft === oldChild) {
            mLeft = newChild
        } else if (mRight === oldChild) {
            mRight = newChild
        }
    }

    /**
     * Recursive function to gather all the leaves in the subtree of which
     * this node is the root.
     *
     * @param leafSet Leafset that's being built.
     * @return Set of leaves being completed.
     */
    private fun getLeafSet(leafSet: MutableSet<SearchConditionTreeNode?>): MutableSet<SearchConditionTreeNode?> {
        if (mLeft == null && mRight == null) {
            // if we ended up in a leaf, add ourself and return
            leafSet.add(this)
            return leafSet
        }

        // we didn't end up in a leaf
        if (mLeft != null) {
            mLeft!!.getLeafSet(leafSet)
        }

        if (mRight != null) {
            mRight!!.getLeafSet(leafSet)
        }
        return leafSet
    }

    /**/
    // ///////////////////////////////////////////////////////// */ // Parcelable
    //
    // This whole class has to be parcelable because it's passed
    // on through intents.
    /**/
    // ///////////////////////////////////////////////////////// */
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mValue.ordinal)
        dest.writeParcelable(this.condition, flags)
        dest.writeParcelable(mLeft, flags)
        dest.writeParcelable(mRight, flags)
    }

    private constructor(`in`: Parcel) {
        mValue = Operator.entries[`in`.readInt()]
        this.condition = `in`.readParcelable<SearchCondition?>(SearchConditionTreeNode::class.java.getClassLoader())
        mLeft = `in`.readParcelable<SearchConditionTreeNode?>(SearchConditionTreeNode::class.java.getClassLoader())
        mRight = `in`.readParcelable<SearchConditionTreeNode?>(SearchConditionTreeNode::class.java.getClassLoader())
        mParent = null

        if (mLeft != null) {
            mLeft!!.mParent = this
        }

        if (mRight != null) {
            mRight!!.mParent = this
        }
    }

    override fun toString(): String {
        return "ConditionsTreeNode(" +
            "mLeft=" + mLeft +
            ", mRight=" + mRight +
            ", mParent=" + mParent +
            ", mValue=" + mValue +
            ", mCondition=" + this.condition +
            ')'
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SearchConditionTreeNode?> =
            object : Parcelable.Creator<SearchConditionTreeNode?> {
                override fun createFromParcel(`in`: Parcel): SearchConditionTreeNode {
                    return SearchConditionTreeNode(`in`)
                }

                override fun newArray(size: Int): Array<SearchConditionTreeNode?> {
                    return arrayOfNulls<SearchConditionTreeNode>(size)
                }
            }
    }
}
