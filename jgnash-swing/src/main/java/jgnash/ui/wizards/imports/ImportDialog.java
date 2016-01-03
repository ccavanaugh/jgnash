/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
package jgnash.ui.wizards.imports;

import jgnash.ui.UIApplication;
import jgnash.ui.components.wizard.WizardDialog;
import jgnash.ui.util.DialogUtils;

/**
 * Dialog for import of OFX and MT940 files
 *
 * @author Craig Cavanaugh
 *
 */
public class ImportDialog extends WizardDialog {

    public enum Settings {

        BANK,
        ACCOUNT,
        TRANSACTIONS

    }

    public ImportDialog() {
        super(UIApplication.getFrame());

        setTitle(rb.getString("Title.ImportTransactions"));

        addTaskPage(new ImportOne());
        addTaskPage(new ImportTwo());
        addTaskPage(new ImportSummary());

        DialogUtils.addBoundsListener(this);
    }
}
