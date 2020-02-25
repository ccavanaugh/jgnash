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

import jgnash.resource.util.OS;
import jgnash.uifx.skin.ThemeManager;
import jgnash.util.EncodeDecode;
import jgnash.util.NotNull;

import java.util.Locale;

/**
 * Implementation of a Material Design Icons.
 * <p>
 * This scales well with the font sizeProperty changes and is good for use in table cells.
 *
 * @author Craig Cavanaugh
 */
public class MaterialDesignLabel extends Label {

    private static final String TTF_PATH = "/jgnash/fonts/materialdesignicons-webfont.ttf";

    public static final double DEFAULT_SIZE = 16.0;

    private static final double BASELINE_OFFSET;

    static {
        if (OS.isSystemWindows()) {
            BASELINE_OFFSET = -2;
        } else {    // Linux and OSX
            BASELINE_OFFSET = -5;
        }

        Font.loadFont(MaterialDesignLabel.class.getResource(TTF_PATH).toExternalForm(),
                ThemeManager.fontScaleProperty().get() * DEFAULT_SIZE);
    }

    private final ObjectProperty<Object> glyphName = new SimpleObjectProperty<>();

    private final DoubleProperty sizeProperty = new SimpleDoubleProperty(DEFAULT_SIZE);

    @SuppressWarnings("unused")
    public MaterialDesignLabel() {
        this(MDIcon.BUG);
    }

    public static MaterialDesignLabel fromInteger(final int value, final double size, final long color) {
        final Color c = Color.web(EncodeDecode.longToColorString(color));

        for (final MDIcon mdIcon : MDIcon.values()) {
            if (mdIcon.unicode == value) {
                return new MaterialDesignLabel(mdIcon, size, c);
            }
        }

        return new MaterialDesignLabel(MDIcon.BUG);
    }

    public MaterialDesignLabel(final MDIcon glyphValue) {
        this(glyphValue, ThemeManager.fontScaleProperty().get() * DEFAULT_SIZE, null);
    }

    public MaterialDesignLabel(final MDIcon glyphValue, final Double sizeValue) {
        this(glyphValue, sizeValue, null);
    }

    public MaterialDesignLabel(final MDIcon glyphValue, final Double sizeValue, final Paint paint) {

        sizeProperty.set(sizeValue);

        final StringExpression iconStyleProperty = Bindings.format(Locale.US,
                "-fx-font-family: 'Material Design Icons'; -fx-font-size: %1$.4f; -fx-padding: 0 0 %2$.4f 0",
                ThemeManager.fontScaleProperty().multiply(sizeProperty),
                ThemeManager.fontScaleProperty().multiply(BASELINE_OFFSET));

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
                if (value instanceof Integer) {
                    setText(new String(new int[]{(int) value}, 0, 1));
                } else {    //  MDIcon is assumed
                    setText(new String(new int[]{MDIcon.valueOf(value.toString()).getUnicode()}, 0, 1));
                }
            }
        } catch (final IllegalArgumentException e) {
            System.err.println(e.toString());
            setText(Integer.toString(MDIcon.BUG.getUnicode()));
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
    public enum MDIcon {
        ADJUST(0xf01a),
        AIRPLANE(0xf01d),
        AMBULANCE(0xf02f),
        ANCHOR(0xf031),
        ARROW_COLLAPSE_VERTICAL(0xf84c),
        ARROW_RIGHT_BOLD(0xf733),
        ARROWS(0xf04c),
        ARROWS_H(0xf84d),
        ARROWS_V(0xf84e),
        BANK(0xf070),
        BANK_PLUS(0xfd8d),
        BED_EMPTY(0xf89f),
        BEER(0xf098),
        BELL(0xf09A),
        BIKE(0xf0a3),
        BOOKMARK(0xf0c0),
        BUG(0xf0e4),
        OFFICE_BUILDING(0xf990),
        BULLS_EYE(0xf5dd),
        BUS(0xf0e7),
        BUS_SCHOOL(0xf79e),
        CAKE(0xf0eb),
        CALENDAR(0xf0ed),
        CALENDAR_CHECK(0xf0ef),
        CALENDAR_PLUS(0xf0f3),
        CASH(0xf114),
        CASH_MULTIPLE(0xf116),
        CAMERA(0xf100),
        CAR(0xf10b),
        CHART_AREA(0xfeae),
        CHART_BAR(0xf128),
        CHART_LINE(0xf12a),
        CHART_PIE(0xf12b),
        CHEVRON_LEFT(0xf141),
        CHEVRON_RIGHT(0xf142),
        CHILD(0xf2e7),
        CIRCLE(0xf764),
        CIRCLE_OPEN(0xf130),
        CLIPBOARD(0xf147),
        CLOCK_OUTLINE(0xf150),
        CLOSE_CIRCLE(0xf159),
        CLOUD_DOWNLOAD(0xf162),
        COFFEE(0xf176),
        COGS(0xf8d5),
        CONSOLE(0xf18d),
        CROSS_HAIRS(0xf1a3),
        EDIT(0xfda4),
        ELLIPSIS_H(0xf1d8),
        ENVELOPE(0xf1ee), // email
        EXCLAMATION(0xf0263), // exclamation-thick
        EXCLAMATION_CIRCLE(0xf028), // alert-circle
        EXCLAMATION_TRIANGLE(0xf026), // alert
        EXTERNAL_LINK(0xffe5),  //location-exit
        EYE(0xf208),
        SKIP_BACKWARD(0xf4ab),
        SKIP_FORWARD(0xf4ac),
        FEMALE(0xf649),
        FILE(0xf214),
        FILE_CODE_O(0xf22e),
        FILE_DOCUMENT_BOX_PLUS(0xfec7),
        FILE_EXCEL_O(0xf004f),
        FILE_IMAGE_O(0xf21f),
        FILTER(0xf232),
        FLAG(0xf23b),
        FIRE(0xf238),
        FIRE_EXTINGUISHER(0xff0f),
        FLASK(0xf093),
        FOLDER_OPEN(0xf76f),
        /*
        FROWN('\uf119'),
        FUT_BOL('\uf1e3'),
        GAMEPAD('\uf11b'),
        GIFT('\uf06b'),*/
        HANDSHAKE(0xf0243),
        /*HASH_TAG('\uf292'),
        HEARTBEAT('\uf21e'),
        HOSPITAL('\uf0f8'),*/
        INFO(0xf64e),
        INFO_CIRCLE(0xf2fc),
        KEY(0xf306),
        LANGUAGE(0xf0368),
        LANGUAGE_JAVASCRIPT(0xf31e),
        LAPTOP(0xf322),
        LEVEL_DOWN(0xf046),   // arrow_down_thick
        LEVEL_UP(0xf05e), // arrow_up_thick
        LINK(0xf337),
        LINK_OFF(0xf338),
        LIST(0xf279),
        MALE(0xf64d),
        MAP(0xf34d),
        MINUS_CIRCLE(0xf376),
        MOTORCYCLE(0xf37c),
        PAW(0xf3e9),
        PENCIL(0xf3eb),
        PHONE(0xf3f2),
        PLUG(0xf64a), // power-plug
        PLUS(0xf0217),
        PLUS_CIRCLE(0xf417),
        PLUS_SQUARE(0xf416),
        PRINT(0xf42a),  //printer
        QUESTION_CIRCLE(0xf2d7),
        REFRESH(0xf4e6), // SYNC
        SAVE(0xf193),
        SCREW(0xfe56),
        /*SEARCH_MINUS('\uf010'),
        SEARCH_PLUS('\uf00e'),
        SHIP('\uf21A')*/
        SIGN_OUT(0xf343),
        STEP_BACKWARD(0xf4ae),
        STEP_FORWARD(0xf4ad),
        STOP_CIRCLE(0xf666),
        SWAP_HORIZONTAL(0xf4e1),
        /*SUITCASE('\uf0f2'),
        TABLE('\uf0ce'),*/
        TABLE_COLUMN_WIDTH(0xf4ef),
        TAG(0xf4f9),
        TAGS(0xf4fb),
        TEXT_HEIGHT(0xf27f),
        /* TIMES('\uf00d'),
        TIMES_CIRCLE('\uf057'),*/
        TRASH_O(0xfa79),
        /*
        UNLINK('\uf127'),
        WINDOW_RESTORE('\uf2d2'),
        WRENCH('\uf0ad')*/;

        private final int unicode;

        MDIcon(@NotNull final int unicode) {
            this.unicode = unicode;
        }

        public int getUnicode() {
            return unicode;
        }
    }
}
