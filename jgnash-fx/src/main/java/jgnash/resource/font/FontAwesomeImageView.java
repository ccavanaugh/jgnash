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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import jgnash.uifx.skin.ThemeManager;

/**
 * Utility class to convert font glyphs into ImageViews
 *
 * @author Craig Cavanaugh
 */
public class FontAwesomeImageView extends GlyphImageView {

    private static final String FONT_NAME = "FontAwesome"; //$NON-NLS-1$

    static {
        Font.loadFont(FontAwesomeImageView.class.getResource(FontAwesomeIconView.TTF_PATH).toExternalForm(),
                ThemeManager.getFontScaleProperty().get() * DEFAULT_SIZE);
    }

    @SuppressWarnings("unused")
    public FontAwesomeImageView() {
        super(FontAwesomeIcon.BUG, ThemeManager.getFontScaleProperty().get() * DEFAULT_SIZE, ThemeManager.getBaseTextColor());
    }

    public FontAwesomeImageView(final GlyphIcons glyphValue) {
        super(glyphValue, ThemeManager.getFontScaleProperty().get() * DEFAULT_SIZE, ThemeManager.getBaseTextColor());
    }

    public FontAwesomeImageView(final GlyphIcons glyphValue, final Double sizeValue) {
        super(glyphValue, sizeValue, ThemeManager.getBaseTextColor());
    }

    public FontAwesomeImageView(final GlyphIcons glyphValue, final Double sizeValue, final Paint colorValue) {
        super(glyphValue, sizeValue, colorValue);
    }

    @Override
    String getFontName() {
        return FONT_NAME;
    }

    @Override
    Character getGlyphChar(final String string) {
        return FontAwesomeIcon.valueOf(string).getChar();
    }
}
