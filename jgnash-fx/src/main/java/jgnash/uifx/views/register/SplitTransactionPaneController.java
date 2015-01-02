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
package jgnash.uifx.views.register;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import jgnash.engine.Account;
import jgnash.uifx.control.DecimalTextField;

/**
 * Split Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class SplitTransactionPaneController implements Initializable {

    @FXML
    protected DecimalTextField amountField;

    @FXML
    protected TextField memoTextField;

    @FXML
    protected AccountExchangePane accountExchangePane;

    @FXML
    protected CheckBox reconciledButton;

    @FXML
    private AttachmentPane attachmentPane;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private PanelType panelType;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        // Bind necessary properties to the exchange panel
        accountExchangePane.getBaseAccountProperty().bind(getAccountProperty());
        accountExchangePane.getAmountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.getAmountEditable().bind(amountField.editableProperty());
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    void setPanelType(final PanelType panelType) {
        this.panelType = panelType;
    }

    @FXML
    private void okAction() {
    }

    @FXML
    private void cancelAction() {
    }
}
