/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.ui.actions;

import javax.swing.*;

import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.ui.wizards.imports.jgnash.ImportDialog;

import java.awt.event.ActionEvent;

/**
 * UI Action to import an old jgnash file
 *
 * @author Craig Cavanaugh
 *
 */
@Action("jgnashimport-command")
public class ImportJgnashAction extends AbstractAction{

    @Override
    public void actionPerformed(final ActionEvent e) {
        ImportDialog.showDialog(UIApplication.getFrame());
    }
}
