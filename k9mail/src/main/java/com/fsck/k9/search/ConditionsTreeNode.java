package com.fsck.k9.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.Set;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchCondition;
import com.fsck.k9.search.SearchSpecification.SearchField;


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

    public ConditionsTreeNode left;
    public ConditionsTreeNode right;
    public ConditionsTreeNode parent;

    /*
     * If value isn't CONDITION then condition contains a real
     * condition, otherwise it's null.
     */
    public Operator value;
    public SearchCondition condition;

    /*
     * Used for storing and retrieving the tree to/from the database.
     * The algorithm is called "modified preorder tree traversal".
     */
    private int leftMPTTMarker;
    private int rightMPTTMarker;


    ///////////////////////////////////////////////////////////////
    // Static Helpers to restore a tree from a database cursor
    ///////////////////////////////////////////////////////////////
    /**
     * Builds a condition tree starting from a database cursor. The cursor
     * should point to rows representing the nodes of the tree.
     *
     * @param cursor Cursor pointing to the first of a bunch or rows. Each rows
     *  should contains 1 tree node.
     * @return A condition tree.
     */
    public static ConditionsTreeNode buildTreeFromDB(Cursor cursor) {
        Stack<ConditionsTreeNode> stack = new Stack<ConditionsTreeNode>();
        ConditionsTreeNode tmp = null;

        // root node
        if (cursor.moveToFirst()) {
            tmp = buildNodeFromRow(cursor);
            stack.push(tmp);
        }

        // other nodes
        while (cursor.moveToNext()) {
            tmp = buildNodeFromRow(cursor);
            if (tmp.rightMPTTMarker < stack.peek().rightMPTTMarker) {
                stack.peek().left = tmp;
                stack.push(tmp);
            } else {
                while (stack.peek().rightMPTTMarker < tmp.rightMPTTMarker) {
                    stack.pop();
                }
                stack.peek().right = tmp;
            }
        }
        return tmp;
    }

    /**
     * Converts a single database row to a single condition node.
     *
     * @param cursor Cursor pointing to the row we want to convert.
     * @return A single ConditionsTreeNode
     */
    private static ConditionsTreeNode buildNodeFromRow(Cursor cursor) {
        ConditionsTreeNode result = null;
        SearchCondition condition = null;

        Operator tmpValue = ConditionsTreeNode.Operator.valueOf(cursor.getString(5));

        if (tmpValue == Operator.CONDITION) {
            condition = new SearchCondition(SearchField.valueOf(cursor.getString(0)),
                    Attribute.valueOf(cursor.getString(2)), cursor.getString(1));
        }

        result = new ConditionsTreeNode(condition);
        result.value = tmpValue;
        result.leftMPTTMarker = cursor.getInt(3);
        result.rightMPTTMarker = cursor.getInt(4);

        return result;
    }


    ///////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////
    public ConditionsTreeNode(SearchCondition condition) {
        parent = null;
        this.condition = condition;
        value = Operator.CONDITION;
    }

    private ConditionsTreeNode(ConditionsTreeNode parent, Operator op) {
        this.parent = parent;
        value = op;
        condition = null;
    }


    /* package */ ConditionsTreeNode cloneTree() {
        if (parent != null) {
            throw new IllegalStateException("Can't call cloneTree() for a non-root node");
        }

        ConditionsTreeNode copy = new ConditionsTreeNode(condition.clone());

        copy.leftMPTTMarker = leftMPTTMarker;
        copy.rightMPTTMarker = rightMPTTMarker;

        copy.left = (left == null) ? null : left.cloneNode(copy);
        copy.right = (right == null) ? null : right.cloneNode(copy);

        return copy;
    }

    private ConditionsTreeNode cloneNode(ConditionsTreeNode parent) {
        ConditionsTreeNode copy = new ConditionsTreeNode(parent, value);

        copy.condition = condition.clone();
        copy.leftMPTTMarker = leftMPTTMarker;
        copy.rightMPTTMarker = rightMPTTMarker;

        copy.left = (left == null) ? null : left.cloneNode(copy);
        copy.right = (right == null) ? null : right.cloneNode(copy);

        return copy;
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
     * @throws Exception
     */
    public ConditionsTreeNode and(ConditionsTreeNode expr) throws Exception {
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
        try {
            ConditionsTreeNode tmp = new ConditionsTreeNode(condition);
            return and(tmp);
        } catch (Exception e) {
            // impossible
            return null;
        }
    }

    /**
     * Adds the expression as the second argument of an OR
     * clause to this node.
     *
     * @param expr Expression to 'OR' with.
     * @return New top OR node.
     * @throws Exception
     */
    public ConditionsTreeNode or(ConditionsTreeNode expr) throws Exception {
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
        try {
            ConditionsTreeNode tmp = new ConditionsTreeNode(condition);
            return or(tmp);
        } catch (Exception e) {
            // impossible
            return null;
        }
    }

    /**
     * This applies the MPTT labeling to the subtree of which this node
     * is the root node.
     *
     * For a description on MPTT see:
     * http://www.sitepoint.com/hierarchical-data-database-2/
     */
    public void applyMPTTLabel() {
        applyMPTTLabel(1);
    }


    ///////////////////////////////////////////////////////////////
    // Public accessors
    ///////////////////////////////////////////////////////////////
    /**
     * Returns the condition stored in this node.
     * @return Condition stored in the node.
     */
    public SearchCondition getCondition() {
        return condition;
    }

    /**
     * Get a set of all the leaves in the tree.
     * @return Set of all the leaves.
     */
    public Set<ConditionsTreeNode> getLeafSet() {
        Set<ConditionsTreeNode> leafSet = new HashSet<ConditionsTreeNode>();
        return getLeafSet(leafSet);
    }

    /**
     * Returns a list of all the nodes in the subtree of which this node
     * is the root. The list contains the nodes in a pre traversal order.
     *
     * @return List of all nodes in subtree in preorder.
     */
    public List<ConditionsTreeNode> preorder() {
        List<ConditionsTreeNode> result = new ArrayList<ConditionsTreeNode>();
        Stack<ConditionsTreeNode> stack = new Stack<ConditionsTreeNode>();
        stack.push(this);

        while (!stack.isEmpty()) {
            ConditionsTreeNode current = stack.pop();

            if (current.left != null) {
                stack.push(current.left);
            }

            if (current.right != null) {
                stack.push(current.right);
            }

            result.add(current);
        }

        return result;
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
     * @param Node to add.
     * @param Operator that will connect the new node with this one.
     * @return New parent node, containing the operator.
     * @throws Exception Throws when the provided new node does not have a null parent.
     */
    private ConditionsTreeNode add(ConditionsTreeNode node, Operator op) throws Exception {
        if (node.parent != null) {
            throw new Exception("Can only add new expressions from root node down.");
        }

        ConditionsTreeNode tmpNode = new ConditionsTreeNode(parent, op);
        tmpNode.left = this;
        tmpNode.right = node;

        if (parent != null) {
            parent.updateChild(this, tmpNode);
        }

        this.parent = tmpNode;
        node.parent = tmpNode;

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
        if (left == oldChild) {
            left = newChild;
        } else if (right == oldChild) {
            right = newChild;
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
        if (left == null && right == null) {
            // if we ended up in a leaf, add ourself and return
            leafSet.add(this);
            return leafSet;
        }

        // we didn't end up in a leaf
        if (left != null) {
            left.getLeafSet(leafSet);
        }

        if (right != null) {
            right.getLeafSet(leafSet);
        }
        return leafSet;
    }

    /**
     * This applies the MPTT labeling to the subtree of which this node
     * is the root node.
     *
     * For a description on MPTT see:
     * http://www.sitepoint.com/hierarchical-data-database-2/
     */
    private int applyMPTTLabel(int label) {
        leftMPTTMarker = label;

        if (left != null) {
            label = left.applyMPTTLabel(label += 1);
        }

        if (right != null) {
            label = right.applyMPTTLabel(label += 1);
        }

        ++label;
        rightMPTTMarker = label;
        return label;
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
        dest.writeInt(value.ordinal());
        dest.writeParcelable(condition, flags);
        dest.writeParcelable(left, flags);
        dest.writeParcelable(right, flags);
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
        value = Operator.values()[in.readInt()];
        condition = in.readParcelable(ConditionsTreeNode.class.getClassLoader());
        left = in.readParcelable(ConditionsTreeNode.class.getClassLoader());
        right = in.readParcelable(ConditionsTreeNode.class.getClassLoader());
        parent = null;

        if (left != null) {
            left.parent = this;
        }

        if (right != null) {
            right.parent = this;
        }
    }
}
