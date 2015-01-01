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
package jgnash.uifx.control;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

/**
 * Enhanced DatePicker.  Adds short cuts for date entry with better input character filters
 *
 * @author Craig Cavanuugh
 */
public class DatePickerEx extends DatePicker {

    private final String ALLOWED_DATE_CHARACTERS;

    public DatePickerEx() {
        super(LocalDate.now()); // initialize with a valid date

        final StringBuilder buf = new StringBuilder("0123456789");
        final DateTimeFormatter dateFormatter = getDateFormatter();

        final char[] chars = dateFormatter.format(LocalDate.now()).toCharArray();

        for (final char aChar : chars) {
            if (!Character.isDigit(aChar)) {
                if (buf.indexOf(Character.toString(aChar)) == -1) {
                    buf.append(aChar);
                }
            }
        }

        ALLOWED_DATE_CHARACTERS = buf.toString();

        setConverter(new DateConverter());

        getEditor().addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent event) {
                if (ALLOWED_DATE_CHARACTERS.indexOf(event.getCharacter().charAt(0)) < 0) {
                    event.consume();
                }
            }
        });

        getEditor().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent event) {
                final int caretPosition = getEditor().getCaretPosition();
                final LocalDate date = getValue();

                switch (event.getCode()) {
                    case ADD:
                    case UP:
                    case KP_UP:
                        Platform.runLater(() -> {
                            setValue(date.plusDays(1));
                            getEditor().positionCaret(caretPosition);
                        });
                        break;
                    case SUBTRACT:
                    case DOWN:
                    case KP_DOWN:
                        Platform.runLater(() -> {
                            setValue(date.minusDays(1));
                            getEditor().positionCaret(caretPosition);
                        });
                        break;
                    case T:
                        Platform.runLater(() -> {
                            setValue(LocalDate.now());
                            getEditor().positionCaret(caretPosition);
                        });
                        break;
                    case PAGE_UP:
                        Platform.runLater(() -> {
                            setValue(date.plusMonths(1));
                            getEditor().positionCaret(caretPosition);
                        });
                        break;
                    case PAGE_DOWN:
                        Platform.runLater(() -> {
                            setValue(date.minusMonths(1));
                            getEditor().positionCaret(caretPosition);
                        });
                        break;
                    default:
                }
            }
        });
    }

    private String getPattern() {
        final Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        final Chronology chronology = getChronology();

        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT,
                null, chronology, locale);

        if (pattern.contains("d") && !pattern.contains("dd")) {
            // Modify pattern to show two-digit day, including leading zeros.
            pattern = pattern.replace("d", "dd");
        }

        if (pattern.contains("M") && !pattern.contains("MM")) {
            // Modify pattern to show two-digit month, including leading zeros.
            pattern = pattern.replace("M", "MM");
        }

        return pattern;
    }

    private DateTimeFormatter getDateFormatter() {
        final Locale locale = Locale.getDefault(Locale.Category.FORMAT);

        return DateTimeFormatter.ofPattern(getPattern()).withDecimalStyle(DecimalStyle.of(locale));
    }


    private class DateConverter extends StringConverter<LocalDate> {

        @Override
        public String toString(final LocalDate value) {
            if (value != null) {
                final Chronology chronology = getChronology();
                ChronoLocalDate cDate;
                try {
                    cDate = chronology.date(value);
                } catch (final DateTimeException ex) {
                    cDate = value;
                }

                return getDateFormatter().format(cDate);
            } else {
                return "";
            }
        }

        @Override
        public LocalDate fromString(final String text) {
            if (text != null && !text.isEmpty()) {
                final Locale locale = Locale.getDefault(Locale.Category.FORMAT);
                final Chronology chronology = getChronology();

                final String pattern =
                        DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT,
                                null, chronology, locale);

                final DateTimeFormatter df =
                        new DateTimeFormatterBuilder().parseLenient()
                                .appendPattern(pattern)
                                .toFormatter()
                                .withChronology(chronology)
                                .withDecimalStyle(DecimalStyle.of(locale));

                final ChronoLocalDate cDate = chronology.date(df.parse(text));
                return LocalDate.from(cDate);
            }
            return null;
        }
    }
}
