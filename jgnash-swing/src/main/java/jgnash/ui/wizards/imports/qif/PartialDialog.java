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
package jgnash.ui.wizards.imports.qif;

import jgnash.engine.Account;
import jgnash.convert.importat.qif.QifAccount;
import jgnash.convert.importat.qif.QifParser;
import jgnash.ui.UIApplication;
import jgnash.ui.components.wizard.WizardDialog;

/**
 * Dialog for partial import of QIF files
 *
 * @author Craig Cavanaugh
 *
 */
public class PartialDialog extends WizardDialog {

    private final PartialOne partialOne;

    public PartialDialog(final QifParser parser) {
        super(UIApplication.getFrame());

        QifAccount account = parser.accountList.get(0);

        setTitle(rb.getString("Title.ImpPartQif"));

        partialOne = new PartialOne(account);
        addTaskPage(partialOne);
        addTaskPage(new PartialTwo(account));
        addTaskPage(new PartialSummary(account, this));
    }

    public Account getAccount() {
        return partialOne.getAccount();
    }
}
