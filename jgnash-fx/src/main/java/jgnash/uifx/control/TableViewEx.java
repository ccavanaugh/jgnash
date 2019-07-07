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
package jgnash.uifx.control;

import java.util.List;
import java.util.function.Function;

import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;

/**
 * Expanded TableView with a default Copy to clipboard handler
 *
 * @param <S> The type of the objects contained within the TableView items list.
 */
public class TableViewEx<S> extends TableView<S> {

    private Function<S, String> clipBoardStringFunction = null;

    public TableViewEx() {
        super();

        // register a keyboard copy command for transactions
        setOnKeyPressed(event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.C) {
                handleCopyToClipboard();
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
}
