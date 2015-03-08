package jgnash.uifx.util;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Utility methods for {@code Scene}
 *
 * @author Craig Cavanaugh
 */
public class SceneUtils {

    private SceneUtils() {
        // utility class
    }

    /**
     * Focuses the next node within a {@code Parent}
     * @param node {@code Node} predecessor node
     */
    public static void focusNext(final Node node) {

        final Parent parent = node.getParent();

        if (parent != null) {
            final ObservableList<Node> children = parent.getChildrenUnmodifiable();
            final int index = children.indexOf(node);

            if (index >= 0) {
                // step through children after this node
                for (int i = index + 1; i < children.size(); i++) {
                    if (children.get(i).isFocusTraversable()) {
                        children.get(i).requestFocus();
                        break;
                    }
                }

                // wrap to the start
                for (int i = 0; i < index; i++) {
                    if (children.get(i).isFocusTraversable()) {
                        children.get(i).requestFocus();
                        break;
                    }
                }
            }
        }
    }
}
