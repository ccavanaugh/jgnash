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
package jgnash.uifx.control.behavior;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ComboBoxBase;

import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import com.sun.javafx.scene.control.behavior.KeyBinding;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;

/**
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextFieldBehavior extends ComboBoxBaseBehavior<BigDecimal> {

    /**
     * Opens the Popup
     */
    protected static final String OPEN_ACTION = "Open";

    /**
     * Closes the Popup
     */
    protected static final String CLOSE_ACTION = "Close";

    public DetailedDecimalTextFieldBehavior(final ComboBoxBase<BigDecimal> base) {
        super(base, KEY_BINDINGS);
    }

    @Override
    protected void callAction(final String name) {
        switch (name) {
            case OPEN_ACTION:
                show();
                break;
            case CLOSE_ACTION:
                hide();
                break;
            case "togglePopup":
                if (getControl().isShowing()) {
                    hide();
                } else {
                    show();
                }
                break;

            default:
                super.callAction(name);
        }
    }

    protected static final List<KeyBinding> KEY_BINDINGS = new ArrayList<>();

    static {
        KEY_BINDINGS.add(new KeyBinding(F4, KEY_RELEASED, "togglePopup"));
        KEY_BINDINGS.add(new KeyBinding(UP, "togglePopup").alt());
        KEY_BINDINGS.add(new KeyBinding(DOWN, "togglePopup").alt());
    }
}
