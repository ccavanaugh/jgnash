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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import jgnash.uifx.skin.ThemeManager;
import jgnash.util.EncodeDecode;
import jgnash.util.NotNull;

/**
 * Simple implementation of a FontAwesome based icon.  This scales well with font sizeProperty changes and is good for use
 * in table cells
 *
 * @author Craig Cavanaugh
 */
public class FontAwesomeLabel extends Label {

    private static final String TTF_PATH = "/jgnash/fonts/fa-solid-900.ttf";

    public static final double DEFAULT_SIZE = 16.0;

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

    public static FontAwesomeLabel fromInteger(final int value, final double size, final long color) {
        final String unicode = Character.toString(value);

        final Color c = Color.web(EncodeDecode.longToColorString(color));

        for (final FAIcon faIcon : FAIcon.values()) {
            if (faIcon.unicode.equals(unicode)) {
                return new FontAwesomeLabel(faIcon, size, c);
            }
        }

        return new FontAwesomeLabel(FAIcon.BUG);
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
                "-fx-font-family: Font Awesome 5 Free; -fx-font-weight: 900; -fx-font-size: %1$.6f;",
                ThemeManager.fontScaleProperty().multiply(sizeProperty));

        setGlyphName(glyphValue);

        styleProperty().bind(iconStyleProperty);

        if (paint != null) {
            setTextFill(paint);
        } else {
            textFillProperty().bind(ThemeManager.controlTextFillProperty());
        }

        setUserData(glyphValue);

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
        ARROWS("\uf0b2"),
        ARROWS_H("\uf337"),
        ARROWS_V("\uf338"),
        BELL("\uf0f3"),
        BOLT("\uf0e7"),
        BUG("\uf188"),
        CALENDAR("\uf073"),
        CALENDAR_CHECK("\uf274"),
        CHEVRON_LEFT("\uf053"),
        CHEVRON_RIGHT("\uf054"),
        CIRCLE("\uf111"),
        CIRCLE_NOTCH("\uf1ce"),
        CLINIC_MEDICAL("\uf7f2"),
        CLIPBOARD("\uf0ea"),
        CLOUD_DOWNLOAD("\uf381"),
        CODE("\uf121"),
        COFFEE("\uf0f4"),
        COGS("\uf085"),
        COMPACT_DISC("\uf51f"),
        COMPRESS("\uf066"),
        COPY("\uf0c5"),
        CROSS("\uf654"),
        CROWN("\uf521"),
        EDIT("\uf044"),
        ELLIPSIS_H("\uf141"),
        EXCLAMATION("\uf12a"),
        EXCLAMATION_CIRCLE("\uf06a"),
        EXCLAMATION_TRIANGLE("\uf071"),
        EXCHANGE("\uf362"),
        EXPAND("\uf065"),
        EXTERNAL_LINK("\uf35d"),
        EXTERNAL_LINK_SQUARE("\uf360"),
        EYE("\uf06e"),
        FAST_BACKWARD("\uf049"),
        FAST_FORWARD("\uf050"),
        FILE("\uf15b"),
        FILE_CODE_O("\uf1c9"),
        FILE_EXCEL_O("\uf1c3"),
        FILE_IMAGE_O("\uf1c5"),
        FILTER("\uf0b0"),
        FOLDER_OPEN("\uf07c"),
        FROG("\uf52e"),
        FROWN("\uf119"),
        FROWN_OPEN("\uf57a"),
        FUT_BOL("\uf1e3"),
        GAS_PUMP("\uf52f"),
        GEM("\uf3a5"),
        GIFT("\uf06b"),
        GIFTS("\uf79c"),
        HASH_TAG("\uf292"),
        HOSPITAL("\uf0f8"),
        HOTEL("\uf594"),
        INFO("\uf129"),
        INFO_CIRCLE("\uf05a"),
        KEY("\uf084"),
        LANGUAGE("\uf1ab"),
        LEVEL_DOWN("\uf3be"),
        LEVEL_UP("\uf3bf"),
        LINK("\uf0c1"),
        LIST("\uf03a"),
        LONG_ARROW_RIGHT("\uf30b"),
        MINUS_CIRCLE("\uf056"),
        MONEY("\uf0d6"),
        PENCIL("\uf303"),
        PLUS("\uf067"),
        PLUS_CIRCLE("\uf055"),
        PLUS_SQUARE("\uf0fe"),
        PRAY("\uf683"),
        PRAY_HANDS("\uf684"),
        PRESCRIPTION("\uf5b1"),
        PRINT("\uf02f"),        // broken in fontawesome
        PROCEDURES("\uf487"),
        QUESTION_CIRCLE("\uf059"),
        SAVE("\uf0c7"),
        SIGN_OUT("\uf2f5"),
        STEP_BACKWARD("\uf048"),
        STEP_FORWARD("\uf051"),
        STOP_CIRCLE("\uf28d"),
        SYNC_ALT("\uf2f1"),
        TABLE("\uf0ce"),
        // TAG("\uf02b"),               // broken in fontawesome
        // TAGS("\uf02c"),              // broken in fontawesome
        TERMINAL("\uf120"),
        TEXT_HEIGHT("\uf034"),
        TIMES_CIRCLE("\uf057"),
        TOOTH("\uf5c9"),
        TRASH_O("\uf1f8"),
        UNIVERSITY("\uf19c"),
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
