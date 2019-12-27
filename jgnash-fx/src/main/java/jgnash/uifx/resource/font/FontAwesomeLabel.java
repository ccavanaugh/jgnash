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
                "-fx-font-family: Font Awesome 5 Free; -fx-font-weight: 900; -fx-font-size: %1$.6f;",
                ThemeManager.fontScaleProperty().multiply(sizeProperty));

        setGlyphName(glyphValue);

        styleProperty().bind(iconStyleProperty);

        if (paint != null) {
            setTextFill(paint);
        } else {
            textFillProperty().bind(ThemeManager.controlTextFillProperty());
        }

        setUserData(glyphValue);    // enum is saved as user data to make lookup easier

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
        ALLERGIES('\uf461'),
        AMBULANCE('\uf0f9'),
        ANCHOR('\uf13d'),
        ANGRY('\uf556'),
        ARROWS('\uf0b2'),
        ARROWS_H('\uf337'),
        ARROWS_V('\uf338'),
        ARCHWAY('\uf557'),
        BABY('\uf77c'),
        BABY_CARRIAGE('\uf77d'),
        BAND_AID('\uf462'),
        // BASE_BALL('\uf433'),             // broken in fontawesome
        // BASKETBALL_BALL('\uf434'),       // broken in fontawesome
        BED('\uf236'),
        BEER('\uf0fc'),
        BELL('\uf0f3'),
        BIBLE('\uf647'),
        BICYCLE('\uf206'),
        BIKING('\uf84a'),
        BIRTHDAY_CAKE('\uf1fd'),
        BOLT('\uf0e7'),
        BONE('\uf5d7'),
        // BOOK('\uf02d'),                  // broken in fontawesome
        BOOK_MEDICAL('\uf7e6'),
        // BOWLING_BALL('\uf436'),          // broken in fontawesome
        BRUSH('\uf55d'),
        BUG('\uf188'),
        BUILDING('\uf1ad'),
        BULLS_EYE('\uf140'),
        BUS('\uf207'),
        BUS_ALT('\uf55e'),
        CALENDAR('\uf073'),
        CALENDAR_CHECK('\uf274'),
        // CAMERA('\uf030'),                // broken in fontawesome
        CAMERA_RETRO('\uf083'),
        CAMPGROUND('\uf6bb'),
        CANDY_CANE('\uf786'),
        CAR('\uf1b9'),
        CAR_CRASH('\uf5e1'),
        CAT('\uf6be'),
        CHAIR('\uf6c0'),
        CHALKBOARD('\uf51b'),
        CHALKBOARD_TEACHER('\uf51c'),
        CHARGING_STATION('\uf5e7'),
        CHART_AREA('\uf1fe'),
        CHART_BAR('\uf080'),
        CHART_LINE('\uf201'),
        CHART_PIE('\uf200'),
        CHEVRON_LEFT('\uf053'),
        CHEVRON_RIGHT('\uf054'),
        CHEESE('\uf7ef'),
        // CHESS('\uf439'),                 // broken in fontawesome
        CHILD('\uf1ae'),
        CITY('\uf64f'),
        CIRCLE('\uf111'),
        CIRCLE_NOTCH('\uf1ce'),
        CLINIC_MEDICAL('\uf7f2'),
        CLIPBOARD('\uf0ea'),
        CLOUD_DOWNLOAD('\uf381'),
        COCKTAIL('\uf561'),
        CODE('\uf121'),
        COFFEE('\uf0f4'),
        COGS('\uf085'),
        COMPACT_DISC('\uf51f'),
        COOKIE_BITE('\uf564'),
        COMPRESS('\uf066'),
        COPY('\uf0c5'),
        CROSS('\uf654'),
        CROSS_HAIRS('\uf05b'),
        CROW('\uf520'),
        CROWN('\uf521'),
        CRUTCH('\uf7f7'),
        DOG('\uf6d3'),
        DOVE('\uf4ba'),
        DUMBBELL('\uf44b'),
        DUMPSTER('\uf793'),
        EDIT('\uf044'),
        ELLIPSIS_H('\uf141'),
        ENVELOPE('\uf0e0'),
        EXCLAMATION('\uf12a'),
        EXCLAMATION_CIRCLE('\uf06a'),
        EXCLAMATION_TRIANGLE('\uf071'),
        EXCHANGE('\uf362'),
        EXPAND('\uf065'),
        EXTERNAL_LINK('\uf35d'),
        EXTERNAL_LINK_SQUARE('\uf360'),
        EYE('\uf06e'),
        FAN('\uf863'),
        FAST_BACKWARD('\uf049'),
        FAST_FORWARD('\uf050'),
        FEMALE('\uf182'),
        FILE('\uf15b'),
        FILE_CODE_O('\uf1c9'),
        FILE_EXCEL_O('\uf1c3'),
        FILE_IMAGE_O('\uf1c5'),
        // FILM('\uf008'),                      // broken in fontawesome
        FILTER('\uf0b0'),
        FIRE_EXTINGUISHER('\uf134'),
        FISH('\uf578'),
        FLASK('\uf0c3'),
        FOLDER_OPEN('\uf07c'),
        FOOTBALL_BALL('\uf44e'),
        FROG('\uf52e'),
        FROWN('\uf119'),
        FROWN_OPEN('\uf57a'),
        FUT_BOL('\uf1e3'),
        GAMEPAD('\uf11b'),
        GAS_PUMP('\uf52f'),
        GEM('\uf3a5'),
        GHOST('\uf6e2'),
        GIFT('\uf06b'),
        GIFTS('\uf79c'),
        GOLF_BALL('\uf450'),
        GUITAR('\uf7a6'),
        HASH_TAG('\uf292'),
        HARD_HAT('\uf807'),
        HANUKIAH('\uf6e6'),
        HEARTBEAT('\uf21e'),
        HIKING('\uf6ec'),
        //HOME('\uf015'),                   // broken in fontawesome
        HORSE('\uf6f0'),
        HOSPITAL('\uf0f8'),
        HOUSE_DAMAGE('\uf6f1'),
        HOTEL('\uf594'),
        ICE_CREAM('\uf810'),
        INFO('\uf129'),
        INFO_CIRCLE('\uf05a'),
        KEY('\uf084'),
        LANGUAGE('\uf1ab'),
        LAPTOP('\uf109'),
        LEVEL_DOWN('\uf3be'),
        LEVEL_UP('\uf3bf'),
        LINK('\uf0c1'),
        LIST('\uf03a'),
        LONG_ARROW_RIGHT('\uf30b'),
        LUGGAGE_CART('\uf59d'),
        MALE('\uf183'),
        MAP('\uf279'),
        MINUS_CIRCLE('\uf056'),
        MONEY_BILL('\uf0d6'),
        MONEY_BILL_WAVE('\uf53a'),
        MONEY_CHECK('\uf53c'),
        MOTORCYCLE('\uf21c'),
        PAPER_PLANE('\uf1d8'),
        PAW('\uf1b0'),
        PENCIL('\uf303'),
        PEOPLE_CARRY('\uf4ce'),
        PHONE('\uf095'),
        PILLS('\uf484'),
        PIZZA_SLICE('\uf818'),
        PLACE_OF_WORSHIP('\uf67f'),
        PLUG('\uf1e6'),
        PLUS('\uf067'),
        PLUS_CIRCLE('\uf055'),
        PLUS_SQUARE('\uf0fe'),
        PRAY('\uf683'),
        PRAY_HANDS('\uf684'),
        PRESCRIPTION('\uf5b1'),
        PRINT('\uf02f'),        // broken in fontawesome
        PROCEDURES('\uf487'),
        QUESTION_CIRCLE('\uf059'),
        // ROAD('\uf018'),              // broken in fontawesome
        RUNNING('\uf70c'),
        SATELLITE_DISH('\uf7c0'),
        SAVE('\uf0c7'),
        SCHOOL('\uf549'),
        SCREWDRIVER('\uf54A'),
        SEEDLING('\uf4d8'),
        SHIP('\uf21A'),
        SIGN_OUT('\uf2f5'),
        SKATING('\uf7c5'),
        SKIING('\uf7c9'),
        SNOW_PLOW('\uf7d2'),
        SPIDER('\uf717'),
        STEP_BACKWARD('\uf048'),
        STEP_FORWARD('\uf051'),
        STOP_CIRCLE('\uf28d'),
        SUITCASE('\uf0f2'),
        SWIMMER('\uf5c4'),
        SYNC_ALT('\uf2f1'),
        TABLE('\uf0ce'),
        // TAG('\uf02b'),               // broken in fontawesome
        // TAGS('\uf02c'),              // broken in fontawesome
        TERMINAL('\uf120'),
        TEXT_HEIGHT('\uf034'),
        TIMES_CIRCLE('\uf057'),
        TOOTH('\uf5c9'),
        TRASH_O('\uf1f8'),
        UNIVERSITY('\uf19c'),
        UNLINK('\uf127'),
        USER_TAG('\uf507'),
        UTENSILS('\uf2e7'),
        WALKING('\uf554'),
        WAREHOUSE('\uf494'),
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
