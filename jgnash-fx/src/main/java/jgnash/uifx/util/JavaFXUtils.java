package jgnash.uifx.util;

import de.jensd.fx.glyphs.GlyphsBuilder;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Text;

/**
 * Utility methods for {@code Scene}
 *
 * @author Craig Cavanaugh
 */
public class JavaFXUtils {

    public static final KeyCombination ENTER_KEY = new KeyCodeCombination(KeyCode.ENTER);

    public static final KeyCombination ESCAPE_KEY = new KeyCodeCombination(KeyCode.ESCAPE);

    private JavaFXUtils() {
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

    public static Text createGlyph(final FontAwesomeIcon icon, final String size, final String color) {
        return GlyphsBuilder.create(FontAwesomeIconView.class).glyph(icon)
                .style(String.format("-fx-fill: %s; -fx-font-size: %s;", color, size)).build();
    }

    public static Text createGlyph(final FontAwesomeIcon icon, final String size) {
        return GlyphsBuilder.create(FontAwesomeIconView.class).glyph(icon)
                .style(String.format("-fx-font-size: %s;", size)).build();
    }

    /*public static Text createGlyph(final FontAwesomeIcon icon) {
        return GlyphsBuilder.create(FontAwesomeIconView.class).glyph(icon).build();
    }*/
}
