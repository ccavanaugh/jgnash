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
package jgnash.uifx.control;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;

import jgnash.time.DateUtils;

/**
 * Expanded TableView with a default Copy to clipboard handler
 *
 * @param <S> The type of the objects contained within the TableView items list.
 *
 * @author Craig Cavanaugh
 */
public class TableViewEx<S> extends TableView<S> {

    private Function<S, String> clipBoardStringFunction = null;

    public TableViewEx() {
        super();

        // register a keyboard copy command for transactions
        setOnKeyPressed(event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.C) {
                if (clipBoardStringFunction != null) {
                    handleCopyToClipboard();
                } else {
                    handleGenericCopyToClipboard();
                }
                event.consume();
            }
        });
    }

    public void handleCopyToClipboard() {
        final List<S> items = getSelectionModel().getSelectedItems();

        if (items.size() > 0 && getClipBoardStringFunction() != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            final StringBuilder builder = new StringBuilder();

            for (final S item : items) {
                builder.append(getClipBoardStringFunction().apply(item));
                builder.append('\n');
            }

            content.putString(builder.toString());
            clipboard.setContent(content);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Function<S, String> getClipBoardStringFunction() {
        return clipBoardStringFunction;
    }

    public void setClipBoardStringFunction(final Function<S, String> clipBoardStringFunction) {
        this.clipBoardStringFunction = clipBoardStringFunction;
    }

    private void handleGenericCopyToClipboard() {
        final List<Integer> integerList = getSelectionModel().getSelectedIndices();

        if (integerList.size() > 0) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            final StringBuilder builder = new StringBuilder();

            for (final Integer row : integerList) {
                final List<TableColumn<S, ?>> columns = getColumns();
                for (int i = 0; i < columns.size(); i++) {
                    final TableColumn<S, ?> column = columns.get(i);

                    if (column.getCellObservableValue(row) != null) {
                        final Object value = column.getCellObservableValue(row).getValue();

                        if (value != null) {
                            if (value instanceof BigDecimal) {
                                builder.append(((BigDecimal) value).toPlainString());
                            } else if (value instanceof LocalDate) {
                                builder.append(DateUtils.getExcelDateFormatter().format((LocalDate) value));
                            } else {
                                builder.append(value);
                            }
                        }
                    }
                    if (i < columns.size() - i) {
                        builder.append('\t');
                    }
                }
                builder.append('\n');
            }
            content.putString(builder.toString());
            clipboard.setContent(content);
        }
    }
}
