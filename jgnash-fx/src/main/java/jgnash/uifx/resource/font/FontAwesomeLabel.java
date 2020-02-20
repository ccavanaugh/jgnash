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

    private static final String TTF_PATH = "/jgnash/fonts/fontawesome-webfont.ttf";

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
        final char unicode = (char)value;

        final Color c = Color.web(EncodeDecode.longToColorString(color));

        for (final FAIcon faIcon : FAIcon.values()) {
            if (faIcon.unicode == unicode) {
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
                "-fx-font-family: FontAwesome; -fx-font-size: %1$.6f;",
                ThemeManager.fontScaleProperty().multiply(sizeProperty));

        setGlyphName(glyphValue);

        styleProperty().bind(iconStyleProperty);

        if (paint != null) {
            setTextFill(paint);
        } else {
            textFillProperty().bind(ThemeManager.controlTextFillProperty());
        }

        setUserData(glyphValue);    // enum is saved as user data to make lookup easier
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
                    setText(String.valueOf(FAIcon.valueOf(value.toString()).getUnicode()));
                }
            }
        } catch (final IllegalArgumentException e) {
            System.err.println(e.toString());
            setText(Character.toString(FAIcon.BUG.getUnicode()));
        }
    }

    public Double getSize() {
        return sizeProperty.getValue();
    }

    @SuppressWarnings("unused")
    public void setSize(final Double value) {
        sizeProperty.set(value);
    }

    @SuppressWarnings("unused")
    public enum FAIcon {
        ADJUST('\uf042'),
        AMBULANCE('\uf0f9'),
        ANCHOR('\uf13d'),
        ARROWS('\uf047'),
        ARROWS_H('\uf07e'),
        ARROWS_V('\uf07d'),
        BED('\uf236'),
        BEER('\uf0fc'),
        BELL('\uf0f3'),
        BICYCLE('\uf206'),
        BIRTHDAY_CAKE('\uf1fd'),
        BOLT('\uf0e7'),
        BOOKMARK('\uf02e'),
        BUG('\uf188'),
        BUILDING('\uf1ad'),
        BULLS_EYE('\uf140'),
        BUS('\uf207'),
        BUS_ALT('\uf55e'),
        CALENDAR('\uf073'),
        CALENDAR_CHECK('\uf274'),
        CAMERA_RETRO('\uf083'),
        CAR('\uf1b9'),
        CHART_AREA('\uf1fe'),
        CHART_BAR('\uf080'),
        CHART_LINE('\uf201'),
        CHART_PIE('\uf200'),
        CHEVRON_LEFT('\uf053'),
        CHEVRON_RIGHT('\uf054'),
        CHILD('\uf1ae'),
        CIRCLE('\uf111'),
        CIRCLE_NOTCH('\uf10c'),
        CIRCLE_OPEN_NOTCH('\uf1ce'),
        CLIPBOARD('\uf0ea'),
        CLOCK('\uf017'),
        CLOSE('\uf00d'),
        CLOUD_DOWNLOAD('\uf0ed'),
        CODE('\uf121'),
        COG('\uf013'),
        COFFEE('\uf0f4'),
        COGS('\uf085'),
        COMPRESS('\uf066'),
        COPY('\uf0c5'),
        CROSS_HAIRS('\uf05b'),
        EDIT('\uf044'),
        ELLIPSIS_H('\uf141'),
        ENVELOPE('\uf0e0'),
        EXCLAMATION('\uf12a'),
        EXCLAMATION_CIRCLE('\uf06a'),
        EXCLAMATION_TRIANGLE('\uf071'),
        EXCHANGE('\uf0ec'),
        EXPAND('\uf065'),
        EXTERNAL_LINK('\uf08e'),
        EXTERNAL_LINK_SQUARE('\uf14c'),
        EYE('\uf06e'),
        FAST_BACKWARD('\uf049'),
        FAST_FORWARD('\uf050'),
        FEMALE('\uf182'),
        FILE('\uf15b'),
        FILE_CODE_O('\uf1c9'),
        FILE_EXCEL_O('\uf1c3'),
        FILE_IMAGE_O('\uf1c5'),
        FILTER('\uf0b0'),
        FLAG('\uf024'),
        FIRE_EXTINGUISHER('\uf134'),
        FLASK('\uf0c3'),
        FOLDER_OPEN('\uf07c'),
        FROWN('\uf119'),
        FUT_BOL('\uf1e3'),
        GAMEPAD('\uf11b'),
        GIFT('\uf06b'),
        HASH_TAG('\uf292'),
        HEARTBEAT('\uf21e'),
        HOSPITAL('\uf0f8'),
        INFO('\uf129'),
        INFO_CIRCLE('\uf05a'),
        KEY('\uf084'),
        LANGUAGE('\uf1ab'),
        LAPTOP('\uf109'),
        LEVEL_DOWN('\uf149'),
        LEVEL_UP('\uf148'),
        LINK('\uf0c1'),
        LIST('\uf03a'),
        LONG_ARROW_RIGHT('\uf178'),
        MALE('\uf183'),
        MAP('\uf279'),
        MINUS_CIRCLE('\uf056'),
        MONEY_BILL('\uf0d6'),
        MOTORCYCLE('\uf21c'),
        PAPER_PLANE('\uf1d8'),
        PAW('\uf1b0'),
        PENCIL('\uf040'),
        PHONE('\uf095'),
        PLUG('\uf1e6'),
        PLUS('\uf067'),
        PLUS_CIRCLE('\uf055'),
        PLUS_SQUARE('\uf0fe'),
        QUESTION_CIRCLE('\uf059'),
        REFRESH('\uf021'),
        SAVE('\uf0c7'),
        SEARCH_MINUS('\uf010'),
        SEARCH_PLUS('\uf00e'),
        SHIP('\uf21A'),
        SIGN_OUT('\uf08b'),
        STEP_BACKWARD('\uf048'),
        STEP_FORWARD('\uf051'),
        STOP_CIRCLE('\uf28d'),
        SUITCASE('\uf0f2'),
        TABLE('\uf0ce'),
        TAG('\uf02b'),
        TAGS('\uf02c'),
        TERMINAL('\uf120'),
        TEXT_HEIGHT('\uf034'),
        TIMES('\uf00d'),
        TIMES_CIRCLE('\uf057'),
        TRASH_O('\uf014'),
        UNIVERSITY('\uf19c'),
        UNLINK('\uf127'),
        WINDOW_RESTORE('\uf2d2'),
        WRENCH('\uf0ad');

        private final char unicode;

        FAIcon(@NotNull final char unicode) {
            this.unicode = unicode;
        }

        public char getUnicode() {
            return unicode;
        }
    }
}
