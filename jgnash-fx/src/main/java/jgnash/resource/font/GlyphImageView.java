/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.resource.font;

import de.jensd.fx.glyphs.GlyphIcons;

import java.util.HashMap;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Utility class to convert font glyphs into ImageViews
 *
 * @author Craig Cavanaugh
 */
public abstract class GlyphImageView extends ImageView {

    static final double DEFAULT_SIZE = 14.0;

    private final ObjectProperty<Object> glyphName = new SimpleObjectProperty<>();

    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> size = new SimpleObjectProperty<>();

    private final static HashMap<Long, Image> imageCache = new HashMap<>();

    public GlyphImageView(final GlyphIcons glyphValue, final Double sizeValue, final Color colorValue) {
        color.setValue(colorValue);
        size.setValue(sizeValue);
        glyphName.setValue(glyphValue);

        updateIcon();

        glyphName.addListener(o -> updateIcon());
        color.addListener(o -> updateIcon());
        size.addListener(o -> updateIcon());
    }

    abstract String getFontName();

    abstract Character getGlyphChar(String string);

    /**
     * Set the glyphName to display.
     *
     * @param value This can either be the Glyph Name or a unicode character representing the glyph.
     */
    @FXML
    public void setGlyphName(final Object value) {
        glyphName.set(value);
    }

    @FXML
    public Object getGlyphName() {
        return glyphName.get();
    }

    @FXML
    public void setColor(final Color value) {
        color.setValue(value);
    }

    @FXML
    public Color getColor() {
        return color.get();
    }

    @FXML
    public void setSize(final Double value) {
        size.setValue(value);
    }

    @FXML
    public Double getSize() {
        return size.getValue();
    }

    /**
     * This creates and updates the image
     */
    private void updateIcon() {
        final Object iconValue = getGlyphName();

        if (iconValue != null) {
            if (iconValue instanceof Character) {
                setImage(getImage((Character) iconValue, getColor(), getSize()));
            } else {
                setImage(getImage(getGlyphChar(iconValue.toString()), getColor(), getSize()));
            }
        }
    }

    private Image getImage(final Character character, final Color color, final Double size) {
        final long key = getFontName().hashCode() + color.hashCode() + size.hashCode() + character.hashCode();

        if (imageCache.containsKey(key)) {
            return imageCache.get(key);
        } else {
            final double width = Math.max(DEFAULT_SIZE, getWidth(character, size));

            final Canvas canvas = new Canvas(width, size);
            final GraphicsContext gc = canvas.getGraphicsContext2D();

            final Font font = new Font(getFontName(), size);
            gc.setFont(font);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFill(color);
            gc.fillText(String.valueOf(character), width / 2.0, size / 2.0);

            final SnapshotParameters snapshotParameters = new SnapshotParameters();
            snapshotParameters.setFill(Color.TRANSPARENT);

            final Image image = canvas.snapshot(snapshotParameters, null);

            imageCache.put(key, image);
            return image;
        }
    }

    private double getWidth(final char character, final double size) {
        final Text text = new Text(String.valueOf(character));
        text.fontProperty().setValue(new Font(getFontName(), size));
        new Scene(new Group(text));

        text.applyCss();

        return text.getLayoutBounds().getWidth();
    }
}
