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
package jgnash.uifx.wizard.imports;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import jgnash.convert.importat.ImportState;
import jgnash.convert.importat.ImportTransaction;
import jgnash.engine.Account;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Import Wizard, base account selection.
 *
 * @author Craig Cavanaugh
 */
public class ImportPageThreeController extends AbstractWizardPaneController<ImportWizard.Settings> {

    @FXML
    private Label destLabel;

    @FXML
    private Label transCountLabel;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        updateDescriptor();
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
        // intentionally empty
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {
        final Account account = (Account) map.get(ImportWizard.Settings.ACCOUNT);
        final List<ImportTransaction> transactions = (List<ImportTransaction>) map.get(ImportWizard.Settings.TRANSACTIONS);

        JavaFXUtils.runLater(() -> destLabel.setText(account.getName()));

        final AtomicInteger count = new AtomicInteger();

        transactions.stream().filter(tran -> tran.getState() == ImportState.NEW
                || tran.getState() == ImportState.NOT_EQUAL)
                .forEach(tran -> count.incrementAndGet());

        JavaFXUtils.runLater(() -> transCountLabel.setText(Integer.toString(count.get())));

        updateDescriptor();
    }

    @Override
    public boolean isPaneValid() {
        return true;
    }

    @Override
    public String toString() {
        return "3. " + ResourceUtils.getString("Title.ImpSum");
    }
}
