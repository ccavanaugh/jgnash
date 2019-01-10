/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.LocalDateStringConverter;

import jgnash.time.DateUtils;

/**
 * Enhanced DatePicker.  Adds short cuts for date entry with better input character filters
 *
 * @author Craig Cavanuugh
 */
public class DatePickerEx extends DatePicker {

    private final String allowedDateCharacters;

    private final DateTimeFormatter dateFormatter;

    private char dateFormatSeparator = '/';

    /**
     * Reference is needed to prevent premature garbage collection.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<Boolean> focusChangeListener;

    public DatePickerEx() {
        super(LocalDate.now()); // initialize with a valid date

        final StringBuilder buf = new StringBuilder("0123456789");

        dateFormatter = DateUtils.getShortDateManualEntryFormatter();

        final char[] chars = dateFormatter.format(LocalDate.now()).toCharArray();

        for (final char aChar : chars) {
            if (!Character.isDigit(aChar)) {
                if (buf.indexOf(Character.toString(aChar)) == -1) {
                    buf.append(aChar);

                    dateFormatSeparator = aChar;
                }
            }
        }

        allowedDateCharacters = buf.toString();

        setConverter(new LocalDateStringConverter(dateFormatter, dateFormatter));

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

            // An empty event character is possible... must protect against it
            if (!event.getCharacter().isEmpty() && allowedDateCharacters.indexOf(event.getCharacter().charAt(0)) < 0) {
                event.consume();
            }
        });

        focusChangeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> {
            final int caretPosition = getEditor().getCaretPosition();
            getEditor().setText(dateFormatter.format(_getValue()));
            getEditor().positionCaret(caretPosition);
        });

        // Ensure the last parsable value is displayed after focus is lost
        getEditor().focusedProperty().addListener(new WeakChangeListener<>(focusChangeListener));

        getEditor().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            final int caretPosition = getEditor().getCaretPosition();   // preserve caret position for restoration
            final LocalDate date = _getValue(); // force an update to the current value

            switch (event.getCode()) {
                case PERIOD:    // substitute common separators with the current locale's SEPARATOR
                case SLASH:     // while preventing entry of consecutive separators
                case COMMA:
                case BACK_SLASH:
                    Platform.runLater(() -> {
                        final StringBuilder text = new StringBuilder(getEditor().getText());

                        if (text.length() > caretPosition) {
                            if (text.charAt(caretPosition) != dateFormatSeparator && (caretPosition > 0
                                    && text.charAt(caretPosition - 1) != dateFormatSeparator)) {
                                text.insert(caretPosition, dateFormatSeparator);
                            }
                        } else {
                            text.append(dateFormatSeparator);
                        }
                        getEditor().setText(text.toString());
                        getEditor().positionCaret(caretPosition + 1);
                    });
                    break;
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

        valueProperty().addListener(new WeakChangeListener<>((observable, oldValue, newValue) -> {
            if (newValue == null) {
                setValue(oldValue);
            }
        }));
    }

    private LocalDate _getValue() {
        try {
            final LocalDate date = LocalDate.parse(getEditor().getText(), dateFormatter);
            setValue(date);
            return date;
        } catch (final DateTimeParseException ignored) {
            return getValue();  // return the current value
        }
    }
}
