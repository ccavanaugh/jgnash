/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.uifx.report;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;

import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.FileUtils;
import jgnash.resource.util.ResourceUtils;

/**
 * JavaFX Chart utilities.
 *
 * @author Craig Cavanaugh
 */
class ChartUtilities {

    private ChartUtilities() {
        // Utility class
    }

    /**
     * Increases scale/resolution of captured image.  This could be made user configurable
     */
    private static final int SNAPSHOT_SCALE_FACTOR = 4;

    private static WritableImage takeSnapshot(final Pane pane) {

        Map<Chart, Boolean> animationMap = new HashMap<>();

        // Need to disable chart animations for printing
        pane.getChildren().stream().filter(node -> node instanceof Chart).forEach(node -> {
            animationMap.put((Chart) node, ((Chart) node).getAnimated());

            // Need to disable animation for printing
            ((Chart) node).setAnimated(false);
        });

        final SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setTransform(new Scale(SNAPSHOT_SCALE_FACTOR, SNAPSHOT_SCALE_FACTOR));

        final WritableImage image = pane.snapshot(snapshotParameters, null);

        // Restore animation
        for (Map.Entry<Chart, Boolean> entry : animationMap.entrySet()) {
            entry.getKey().setAnimated(entry.getValue());
        }

        return image;
    }

    static void saveChart(final Pane pane) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(ResourceUtils.getString("Title.SaveFile"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (file != null) {
            final WritableImage image = takeSnapshot(pane);

            try {
                final String type = FileUtils.getFileExtension(file.toString().toLowerCase(Locale.ROOT));

                ImageIO.write(SwingFXUtils.fromFXImage(image, null), type, file);
            } catch (final IOException e) {
                StaticUIMethods.displayException(e);
            }
        }
    }

    static void copyToClipboard(final Pane pane) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        content.putImage(takeSnapshot(pane));

        clipboard.setContent(content);
    }

    static void printChart(final Pane pane) {
        // Manipulate a snapshot of the pane instead of the pane itself to avoid visual artifacts when scaling
        JavaFXUtils.printImageView(new ImageView(takeSnapshot(pane)));
    }
}
