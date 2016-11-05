package jgnash.uifx.util;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.views.main.MainView;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Utility methods for {@code Scene}.
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
     * Focuses the next node within a {@code Parent}.
     *
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

    public static Optional<ScrollBar> findVerticalScrollBar(final Node table) {
        for (final Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar) {
                if (((ScrollBar) node).getOrientation() == Orientation.VERTICAL) {
                    return Optional.of((ScrollBar) node);
                }
            }
        }

        return Optional.empty();
    }

    /*public static ScrollBar findHorizontalScrollBar(final Node table) {
        for (final Node node : table.lookupAll(".scroll-bar:horizontal")) {
            if (node instanceof ScrollBar) {
                if (((ScrollBar) node).getOrientation() == Orientation.HORIZONTAL) {
                    return (ScrollBar) node;
                }
            }
        }

        throw new RuntimeException("Could not find horizontal scroll bar");
    }*/

    /**
     * Calculates the displayed width of a text string.
     *
     * @param displayString displayed text
     * @param style text style, may be null
     * @return width of the displayed string
     */
    public static double getDisplayedTextWidth(@NotNull final String displayString, @Nullable final String style) {
        double width = 0;

        // Text and Scene construction must be done on the Platform thread
        // Invoke the task on the platform thread and wait until complete

        if (!displayString.isEmpty()) {    // ignore empty strings
            if (Platform.isFxApplicationThread()) {
                width = _getDisplayedTextWidth(displayString, style);
            } else {
                // Text and Scene construction must be done on the Platform thread
                // Invoke the task on the platform thread and wait until complete
                final FutureTask<Double> futureTask =
                        new FutureTask<>(() -> _getDisplayedTextWidth(displayString, style));
                Platform.runLater(futureTask);

                try {
                    width = futureTask.get();
                } catch (final InterruptedException | ExecutionException e) {
                    Logger.getLogger(JavaFXUtils.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        }

        return width;
    }

    private static double _getDisplayedTextWidth(@NotNull final String displayString, @Nullable final String style) {
        final Text text = new Text(displayString);
        new Scene(new Group(text));

        text.setStyle(style);
        text.applyCss();

        return Math.ceil(text.getLayoutBounds().getWidth()) * ThemeManager.fontScaleProperty().doubleValue();
    }

    /**
     * Sends an {@code ImageView} to a printer.
     *
     * @param imageView {@code ImageView} to print
     */
    public static void printImageView(final ImageView imageView) {
        final PrinterJob job = PrinterJob.createPrinterJob();

        if (job != null) {
            // Get the default page layout
            final Printer printer = Printer.getDefaultPrinter();
            PageLayout pageLayout = job.getJobSettings().getPageLayout();

            // Request landscape orientation by default
            pageLayout = printer.createPageLayout(pageLayout.getPaper(), PageOrientation.LANDSCAPE,
                    Printer.MarginType.DEFAULT);

            job.getJobSettings().setPageLayout(pageLayout);

            if (job.showPageSetupDialog(MainView.getPrimaryStage())) {
                pageLayout = job.getJobSettings().getPageLayout();

                // determine the scaling factor to fit the page
                final double scale = Math.min(pageLayout.getPrintableWidth() / imageView.getBoundsInParent().getWidth(),
                        pageLayout.getPrintableHeight() / imageView.getBoundsInParent().getHeight());

                imageView.getTransforms().add(new Scale(scale, scale));

                if (job.printPage(imageView)) {
                    job.endJob();
                }
            } else {
                job.cancelJob();
            }
        }
    }
}
