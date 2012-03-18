/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.ui.register;

/**
 * Event object for register events
 *
 * @author Craig Cavanaugh
 *
 */
public class RegisterEvent extends java.util.EventObject {

    private static final long serialVersionUID = 3874537818635582561L;

    public enum Action {
        CANCEL,
        CLOSE,
        OK,
        OPEN
    }

    private Action action;

    public RegisterEvent(Object source, Action action) {
        super(source);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }
}
