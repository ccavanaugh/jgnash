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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javafx.application.Platform;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import jgnash.util.DateUtils;

/**
 * Enhanced DatePicker.  Adds short cuts for date entry with better input character filters
 *
 * @author Craig Cavanuugh
 */
public class DatePickerEx extends DatePicker {

    private final String ALLOWED_DATE_CHARACTERS;

    //private final int validLength;

    private final DateTimeFormatter dateFormatter;

    public DatePickerEx() {
        super(LocalDate.now()); // initialize with a valid date

        final StringBuilder buf = new StringBuilder("0123456789");

        dateFormatter = DateUtils.getShortDateTimeEntryFormat();

        final char[] chars = dateFormatter.format(LocalDate.now()).toCharArray();

        //validLength = chars.length;

        for (final char aChar : chars) {
            if (!Character.isDigit(aChar)) {
                if (buf.indexOf(Character.toString(aChar)) == -1) {
                    buf.append(aChar);
                }
            }
        }

        ALLOWED_DATE_CHARACTERS = buf.toString();

        setConverter(new DateConverter());

        // Handle horizontal and vertical scroll wheel events
        getEditor().setOnScroll(event -> {
            final int caretPosition = getEditor().getCaretPosition();
            final LocalDate date = _getValue();

            if (event.getDeltaY() > 0) {
                Platform.runLater(() -> {
                    setValue(date.plusDays(1));
                    getEditor().positionCaret(caretPosition);
                });
            } else if (event.getDeltaY() < 0) {
                Platform.runLater(() -> {
                    setValue(date.minusDays(1));
                    getEditor().positionCaret(caretPosition);
                });
            }

            if (event.getDeltaX() > 0) {
                Platform.runLater(() -> {
                    setValue(date.plusMonths(1));
                    getEditor().positionCaret(caretPosition);
                });
            } else if (event.getDeltaX() < 0) {
                Platform.runLater(() -> {
                    setValue(date.minusMonths(1));
                    getEditor().positionCaret(caretPosition);
                });
            }
        });

        getEditor().addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (ALLOWED_DATE_CHARACTERS.indexOf(event.getCharacter().charAt(0)) < 0) {
                event.consume();
            }
        });

        // Ensure the last parsable value is displayed after focus is lost
        getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                final int caretPosition = getEditor().getCaretPosition();
                getEditor().setText(dateFormatter.format(_getValue()));
                getEditor().positionCaret(caretPosition);
            });
        });

        getEditor().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            final int caretPosition = getEditor().getCaretPosition();
            final LocalDate date = _getValue();

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
        });
    }

    private LocalDate _getValue() {
        //if (getEditor().getText().length() == validLength) {
            try {
                final LocalDate date = LocalDate.parse(getEditor().getText(), dateFormatter);
                Platform.runLater(() -> setValue(date));
                return date;
            } catch (final DateTimeParseException ignored) {
                return getValue();  // return the current value
            }
        //}
        //return getValue();
    }

    private class DateConverter extends StringConverter<LocalDate> {

        @Override
        public String toString(final LocalDate value) {
            if (value != null) {
                return dateFormatter.format(value);
            } else {
                return "";
            }
        }

        @Override
        public LocalDate fromString(final String text) {
            if (text != null && !text.isEmpty()) {
                return LocalDate.from(dateFormatter.parse(text));
            }
            return null;
        }
    }
}
