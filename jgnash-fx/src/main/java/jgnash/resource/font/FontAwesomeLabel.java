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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.scene.control.Label;
import javafx.scene.text.Font;

import jgnash.uifx.skin.ThemeManager;

/**
 * Simple implementation of a FontAwesome based icon.  This scales well with font size changes and is good for use
 * in table cells
 *
 * @author Craig Cavanaugh
 */
public class FontAwesomeLabel extends Label {

    static {
        Font.loadFont(FontAwesomeImageView.class.getResource(FontAwesomeIconView.TTF_PATH).toExternalForm(),
                ThemeManager.getFontScaleProperty().get() * GlyphImageView.DEFAULT_SIZE);
    }

    public FontAwesomeLabel(final GlyphIcons glyphValue) {
        this(glyphValue, ThemeManager.getFontScaleProperty().get() * GlyphImageView.DEFAULT_SIZE);
    }

    public FontAwesomeLabel(final GlyphIcons glyphValue, final Double sizeValue) {
        setText(String.valueOf(glyphValue.getChar()));
        setStyle("-fx-font-family: FontAwesome; -fx-font-size: " + sizeValue + ";");
    }
}
