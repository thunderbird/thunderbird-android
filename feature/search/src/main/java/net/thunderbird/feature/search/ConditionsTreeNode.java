package net.thunderbird.feature.search;

import java.util.HashSet;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import net.thunderbird.feature.search.api.SearchCondition;


/**
 * This class stores search conditions. It's basically a boolean expression binary tree.
 * The output will be SQL queries ( obtained by traversing inorder ).
 *
 * TODO removing conditions from the tree
 * TODO implement NOT as a node again
 */
public class ConditionsTreeNode implements Parcelable {

    public enum Operator {
        AND, OR, CONDITION
    }

    public ConditionsTreeNode mLeft;
    public ConditionsTreeNode mRight;
    public ConditionsTreeNode mParent;

    /*
     * If mValue isn't CONDITION then mCondition contains a real
     * condition, otherwise it's null.
     */
    public Operator mValue;
    public SearchCondition mCondition;


    ///////////////////////////////////////////////////////////////
    // Static Helpers to restore a tree from a database cursor
    ///////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////
    public ConditionsTreeNode(SearchCondition condition) {
        mParent = null;
        mCondition = condition;
        mValue = Operator.CONDITION;
    }

    public ConditionsTreeNode(ConditionsTreeNode parent, Operator op) {
        mParent = parent;
        mValue = op;
        mCondition = null;
    }


    ///////////////////////////////////////////////////////////////
    // Public modifiers
    ///////////////////////////////////////////////////////////////
    /**
     * Adds the expression as the second argument of an AND
     * clause to this node.
     *
     * @param expr Expression to 'AND' with.
     * @return New top AND node.
     */
    public ConditionsTreeNode and(ConditionsTreeNode expr) {
        return add(expr, Operator.AND);
    }

    /**
     * Convenience method.
     * Adds the provided condition as the second argument of an AND
     * clause to this node.
     *
     * @param condition Condition to 'AND' with.
     * @return New top AND node, new root.
     */
    public ConditionsTreeNode and(SearchCondition condition) {
        ConditionsTreeNode tmp = new ConditionsTreeNode(condition);
        return and(tmp);
    }

    /**
     * Adds the expression as the second argument of an OR
     * clause to this node.
     *
     * @param expr Expression to 'OR' with.
     * @return New top OR node.
     */
    public ConditionsTreeNode or(ConditionsTreeNode expr) {
        return add(expr, Operator.OR);
    }

    /**
     * Convenience method.
     * Adds the provided condition as the second argument of an OR
     * clause to this node.
     *
     * @param condition Condition to 'OR' with.
     * @return New top OR node, new root.
     */
    public ConditionsTreeNode or(SearchCondition condition) {
        ConditionsTreeNode tmp = new ConditionsTreeNode(condition);
        return or(tmp);
    }


    ///////////////////////////////////////////////////////////////
    // Public accessors
    ///////////////////////////////////////////////////////////////
    /**
     * Returns the condition stored in this node.
     * @return Condition stored in the node.
     */
    public SearchCondition getCondition() {
        return mCondition;
    }

    /**
     * Get a set of all the leaves in the tree.
     * @return Set of all the leaves.
     */
    public Set<ConditionsTreeNode> getLeafSet() {
        Set<ConditionsTreeNode> leafSet = new HashSet<>();
        return getLeafSet(leafSet);
    }


    ///////////////////////////////////////////////////////////////
    // Private class logic
    ///////////////////////////////////////////////////////////////
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
     * @return New parent node, containing the operator.
     * @throws IllegalArgumentException Throws when the provided new node does not have a null parent.
     */
    private ConditionsTreeNode add(ConditionsTreeNode node, Operator op) {
        if (node.mParent != null) {
            throw new IllegalArgumentException("Can only add new expressions from root node down.");
        }

        ConditionsTreeNode tmpNode = new ConditionsTreeNode(mParent, op);
        tmpNode.mLeft = this;
        tmpNode.mRight = node;

        if (mParent != null) {
            mParent.updateChild(this, tmpNode);
        }

        this.mParent = tmpNode;
        node.mParent = tmpNode;

        return tmpNode;
    }

    /**
     * Helper method that replaces a child of the current node with a new node.
     * If the provided old child node was the left one, left will be replaced with
     * the new one. Same goes for the right one.
     *
     * @param oldChild Old child node to be replaced.
     * @param newChild New child node.
     */
    private void updateChild(ConditionsTreeNode oldChild, ConditionsTreeNode newChild) {
        // we can compare objects id's because this is the desired behaviour in this case
        if (mLeft == oldChild) {
            mLeft = newChild;
        } else if (mRight == oldChild) {
            mRight = newChild;
        }
    }

    /**
     * Recursive function to gather all the leaves in the subtree of which
     * this node is the root.
     *
     * @param leafSet Leafset that's being built.
     * @return Set of leaves being completed.
     */
    private Set<ConditionsTreeNode> getLeafSet(Set<ConditionsTreeNode> leafSet) {
        if (mLeft == null && mRight == null) {
            // if we ended up in a leaf, add ourself and return
            leafSet.add(this);
            return leafSet;
        }

        // we didn't end up in a leaf
        if (mLeft != null) {
            mLeft.getLeafSet(leafSet);
        }

        if (mRight != null) {
            mRight.getLeafSet(leafSet);
        }
        return leafSet;
    }


    ///////////////////////////////////////////////////////////////
    // Parcelable
    //
    // This whole class has to be parcelable because it's passed
    // on through intents.
    ///////////////////////////////////////////////////////////////
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mValue.ordinal());
        dest.writeParcelable(mCondition, flags);
        dest.writeParcelable(mLeft, flags);
        dest.writeParcelable(mRight, flags);
    }

    public static final Parcelable.Creator<ConditionsTreeNode> CREATOR =
            new Parcelable.Creator<ConditionsTreeNode>() {

        @Override
        public ConditionsTreeNode createFromParcel(Parcel in) {
            return new ConditionsTreeNode(in);
        }

        @Override
        public ConditionsTreeNode[] newArray(int size) {
            return new ConditionsTreeNode[size];
        }
    };

    private ConditionsTreeNode(Parcel in) {
        mValue = Operator.values()[in.readInt()];
        mCondition = in.readParcelable(ConditionsTreeNode.class.getClassLoader());
        mLeft = in.readParcelable(ConditionsTreeNode.class.getClassLoader());
        mRight = in.readParcelable(ConditionsTreeNode.class.getClassLoader());
        mParent = null;

        if (mLeft != null) {
            mLeft.mParent = this;
        }

        if (mRight != null) {
            mRight.mParent = this;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ConditionsTreeNode(" +
            "mLeft=" + mLeft +
            ", mRight=" + mRight +
            ", mParent=" + mParent +
            ", mValue=" + mValue +
            ", mCondition=" + mCondition +
            ')';
    }
}
