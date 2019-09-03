/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.uifx.resource.font;

import java.util.Locale;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import jgnash.uifx.skin.ThemeManager;
import jgnash.util.NotNull;

/**
 * Simple implementation of a FontAwesome based icon.  This scales well with font sizeProperty changes and is good for use
 * in table cells
 *
 * @author Craig Cavanaugh
 */
public class FontAwesomeLabel extends Label {

    private static final String TTF_PATH = "/jgnash/fonts/fontawesome-webfont.ttf";

    private static final double DEFAULT_SIZE = 16.0;

    static {
        Font.loadFont(FontAwesomeLabel.class.getResource(TTF_PATH).toExternalForm(),
                ThemeManager.fontScaleProperty().get() * DEFAULT_SIZE);
    }

    private final ObjectProperty<Object> glyphName = new SimpleObjectProperty<>();

    private final DoubleProperty sizeProperty = new SimpleDoubleProperty(DEFAULT_SIZE);

    @SuppressWarnings("unused")
    public FontAwesomeLabel() {
        this(FAIcon.BUG);
    }

    public FontAwesomeLabel(final FAIcon glyphValue) {
        this(glyphValue, ThemeManager.fontScaleProperty().get() * DEFAULT_SIZE, null);
    }

    public FontAwesomeLabel(final FAIcon glyphValue, final Double sizeValue) {
        this(glyphValue, sizeValue, null);
    }

    public FontAwesomeLabel(final FAIcon glyphValue, final Double sizeValue, final Paint paint) {

        sizeProperty.set(sizeValue);

        final StringExpression iconStyleProperty = Bindings.format(Locale.US,
                "-fx-font-family: FontAwesome; -fx-font-size: %1$.6f;",
                ThemeManager.fontScaleProperty().multiply(sizeProperty));

        setGlyphName(glyphValue);

        styleProperty().bind(iconStyleProperty);

        if (paint != null) {
            setTextFill(paint);
        } else {
            textFillProperty().bind(ThemeManager.controlTextFillProperty());
        }

        setCache(true); // enable caching
    }

    /**
     * Unbinds and changes the color of the icon
     *
     * @param value new color
     */
    public void setColor(final Paint value) {
        if (textFillProperty().isBound()) { //unbind if needed
            textFillProperty().unbind();
        }

        setTextFill(value);
    }

    @SuppressWarnings("unused")
    public Object getGlyphName() {
        return glyphName.get();
    }

    /**
     * Set the glyphName to display.
     *
     * @param value This can either be the Glyph Name or a unicode character representing the glyph.
     */
    public void setGlyphName(final Object value) {
        try {
            glyphName.set(value);

            if (value != null) {
                if (value instanceof Character) {
                    setText(String.valueOf((char) value));
                } else {    //  FAIcon is assumed
                    setText(getUnicode(value.toString()));
                }
            }
        } catch (final IllegalArgumentException e) {
            System.err.println(e.toString());
            setText(FAIcon.BUG.getUnicode());
        }
    }

    public Double getSize() {
        return sizeProperty.getValue();
    }

    @SuppressWarnings("unused")
    public void setSize(final Double value) {
        sizeProperty.set(value);
    }

    private static String getUnicode(final String string) {
        return FAIcon.valueOf(string).getUnicode();
    }

    @SuppressWarnings("unused")
    public enum FAIcon {
        ADJUST("\uf042"),
        ARROWS("\uf047"),
        ARROWS_H("\uf07e"),
        ARROWS_V("\uf07d"),
        BANK("\uf19c"),
        BELL("\uf0f3"),
        BOOKMARK("\uf02e"),
        BUG("\uf188"),
        CALENDAR("\uf073"),
        CHEVRON_LEFT("\uf053"),
        CHEVRON_RIGHT("\uf054"),
        CLIPBOARD("\uf0ea"),
        CLOCK("\uf017"),
        CLOSE("\uf00d"),
        CLOUD_DOWNLOAD("\uf0ed"),
        CODE("\uf121"),
        COG("\uf013"),
        COGS("\uf085"),
        COMPRESS("\uf066"),
        COPY("\uf0c5"),
        EDIT("\uf044"),
        ELLIPSIS_H("\uf141"),
        EXCLAMATION("\uf12a"),
        EXCLAMATION_CIRCLE("\uf06a"),
        EXCLAMATION_TRIANGLE("\uf071"),
        EXCHANGE("\uf0ec"),
        EXPAND("\uf065"),
        EXTERNAL_LINK("\uf08e"),
        EXTERNAL_LINK_SQUARE("\uf14c"),
        EYE("\uf06e"),
        FAST_BACKWARD("\uf049"),
        FAST_FORWARD("\uf050"),
        FILE("\uf15b"),
        FILE_CODE_O("\uf1c9"),
        FILE_EXCEL_O("\uf1c3"),
        FILE_IMAGE_O("\uf1c5"),
        FILTER("\uf0b0"),
        FLAG("\uf024"),
        INFO("\uf129"),
        INFO_CIRCLE("\uf05a"),
        KEY("\uf084"),
        LANGUAGE("\uf1ab"),
        LEVEL_DOWN("\uf149"),
        LEVEL_UP("\uf148"),
        LINK("\uf0c1"),
        LIST("\uf03a"),
        LONG_ARROW_RIGHT("\uf178"),
        FOLDER_OPEN("\uf07c"),
        MINUS_CIRCLE("\uf056"),
        MONEY("\uf0d6"),
        PENCIL("\uf040"),
        PLUS("\uf067"),
        PLUS_CIRCLE("\uf055"),
        POWER_OFF("\uf011"),
        PRINT("\uf02f"),
        QUESTION_CIRCLE("\uf059"),
        REFRESH("\uf021"),
        SAVE("\uf0c7"),
        SEARCH_MINUS("\uf010"),
        SEARCH_PLUS("\uf00e"),
        SIGN_OUT("\uf08b"),
        STEP_BACKWARD("\uf048"),
        STEP_FORWARD("\uf051"),
        TABLE("\uf0ce"),
        TERMINAL("\uf120"),
        TEXT_HEIGHT("\uf034"),
        TIMES("\uf00d"),
        TRASH_O("\uf014"),
        UNLINK("\uf127"),
        WRENCH("\uf0ad");

        private final String unicode;

        FAIcon(@NotNull final String unicode) {
            this.unicode = unicode;
        }

        public String getUnicode() {
            return unicode;
        }
    }
}
