package com.fsck.k9.helper.jsoup;


import android.support.annotation.NonNull;

import org.jsoup.nodes.Node;


/**
 * Node filter interface. Provide an implementing class to {@link AdvancedNodeTraversor} to iterate through
 * nodes.
 * <p>
 * This interface provides two methods, {@code head} and {@code tail}. The head method is called when the node is first
 * seen, and the tail method when all of the node's children have been visited. As an example, head can be used to
 * create a start tag for a node, and tail to create the end tag.
 * </p>
 * <p>
 * For every node, the filter has to decide in {@link NodeFilter#head(Node, int)}) whether to
 * <ul>
 * <li>continue ({@link HeadFilterDecision#CONTINUE}),</li>
 * <li>skip all children ({@link HeadFilterDecision#SKIP_CHILDREN}),</li>
 * <li>skip node entirely ({@link HeadFilterDecision#SKIP_ENTIRELY}),</li>
 * <li>remove the subtree ({@link HeadFilterDecision#REMOVE}),</li>
 * <li>interrupt the iteration and return ({@link HeadFilterDecision#STOP}).</li>
 * </ul>
 * <p>
 * The difference between {@link HeadFilterDecision#SKIP_CHILDREN} and {@link HeadFilterDecision#SKIP_ENTIRELY} is that
 * the first will invoke {@link NodeFilter#tail(Node, int)} on the node, while the latter will not.
 * </p>
 * <p>
 * When {@link NodeFilter#tail(Node, int)} is called the filter has to decide whether to
 * <ul>
 * <li>continue ({@link TailFilterDecision#CONTINUE}),</li>
 * <li>remove the subtree ({@link TailFilterDecision#REMOVE}),</li>
 * <li>interrupt the iteration and return ({@link TailFilterDecision#STOP}).</li>
 * </ul>
 * </p>
 */
public interface NodeFilter {
    /**
     * Filter decision for {@link NodeFilter#head(Node, int)}.
     */
    enum HeadFilterDecision {
        /**
         * Continue processing the tree.
         */
        CONTINUE,
        /**
         * Skip the child nodes, but do call {@link NodeFilter#tail(Node, int)} next.
         */
        SKIP_CHILDREN,
        /**
         * Skip the subtree, and do not call {@link NodeFilter#tail(Node, int)}.
         */
        SKIP_ENTIRELY,
        /**
         * Remove the node and its children, and do not call {@link NodeFilter#tail(Node, int)}.
         */
        REMOVE,
        /**
         * Stop processing.
         */
        STOP
    }

    /**
     * Filter decision for {@link NodeFilter#tail(Node, int)}.
     */
    enum TailFilterDecision {
        /**
         * Continue processing the tree.
         */
        CONTINUE,
        /**
         * Remove the node and its children.
         */
        REMOVE,
        /**
         * Stop processing.
         */
        STOP
    }

    /**
     * Callback for when a node is first visited.
     *
     * @param node
     *         the node being visited.
     * @param depth
     *         the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     *         of that will have depth 1.
     *
     * @return Filter decision
     */
    @NonNull
    HeadFilterDecision head(Node node, int depth);

    /**
     * Callback for when a node is last visited, after all of its descendants have been visited.
     *
     * @param node
     *         the node being visited.
     * @param depth
     *         the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     *         of that will have depth 1.
     *
     * @return Filter decision
     */
    @NonNull
    TailFilterDecision tail(Node node, int depth);
}
