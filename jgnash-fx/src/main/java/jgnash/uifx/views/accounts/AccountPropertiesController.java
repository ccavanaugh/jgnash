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
package jgnash.uifx.views.accounts;

import java.util.Iterator;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.SecurityNode;
import jgnash.uifx.Options;
import jgnash.uifx.control.CurrencyComboBox;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Loads all account properties into a form and returns a template Account based on the form properties.
 *
 * @author Craig Cavanaugh
 */
public class AccountPropertiesController {

    @FXML
    private ButtonBar buttonBar;

    private boolean result = false;

    @FXML
    private ComboBox<AccountType> accountTypeComboBox;

    @FXML
    private TextArea notesTextArea;

    @FXML
    private TextField nameTextField;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private IntegerTextField accountCodeField;

    @FXML
    private TextField accountNumberField;

    @FXML
    private TextField bankIdField;

    @FXML
    private CurrencyComboBox currencyComboBox;

    @FXML
    private CheckBox lockedCheckBox;

    @FXML
    private CheckBox hideAccountCheckBox;

    @FXML
    private CheckBox placeholderCheckBox;

    @FXML
    private CheckBox excludeBudgetCheckBox;

    @FXML
    private Button parentAccountButton;

    @FXML
    private Button securitiesButton;

    @FXML
    private ResourceBundle resources;

    private Account parentAccount;

    private DisableSecuritiesBinding disableSecuritiesBinding;

    @Nullable private Account baseAccount = null;

    private final Set<SecurityNode> securityNodeSet = new TreeSet<>();

    @FXML
    public void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        accountTypeComboBox.setCellFactory(param -> new DisabledListCell());    // set cell factory
        accountTypeComboBox.getItems().addAll(AccountType.values());
        accountTypeComboBox.setValue(AccountType.BANK); // set default value

        descriptionTextField.setText(resources.getString("Word.Description"));
        nameTextField.setText(resources.getString("Word.Name"));

        disableSecuritiesBinding = new DisableSecuritiesBinding();

        securitiesButton.disableProperty().bind(disableSecuritiesBinding);
        accountTypeComboBox.setOnAction(event -> disableSecuritiesBinding.invalidate());
    }

    void setSelectedCurrency(final CurrencyNode currency) {
        currencyComboBox.setValue(currency);
    }

    @FXML
    private void handleParentAccountAction() {
        final Optional<Account> optional = StaticAccountsMethods.selectAccount(parentAccount, baseAccount);

        optional.ifPresent(this::setParentAccount);
    }

    public boolean getResult() {
        return result;
    }

    Set<SecurityNode> getSecurityNodes() {
        return securityNodeSet;
    }

    void setParentAccount(final Account parentAccount) {
        this.parentAccount = parentAccount;

        JavaFXUtils.runLater(() -> {
            if (parentAccount != null) {
                parentAccountButton.setText(parentAccount.getName());

                if (parentAccount.getAccountType() != AccountType.ROOT) {   // don't force a root account type
                    accountTypeComboBox.setValue(parentAccount.getAccountType());
                }
            }
        });
    }

    Account getTemplate() {
        Account account = new Account(accountTypeComboBox.getValue(), currencyComboBox.getValue());

        account.setAccountCode(accountCodeField.getInteger());
        account.setAccountNumber(accountNumberField.getText());
        account.setBankId(bankIdField.getText());
        account.setName(nameTextField.getText());
        account.setDescription(descriptionTextField.getText());
        account.setNotes(notesTextArea.getText());
        account.setLocked(lockedCheckBox.isSelected());
        account.setPlaceHolder(placeholderCheckBox.isSelected());

        if (parentAccount != baseAccount) {
            account.setParent(parentAccount);
        } else {
            Logger.getLogger(AccountPropertiesController.class.getName()).warning("Prevented an attempt to assign an account's parent to itself");
        }

        account.setVisible(!hideAccountCheckBox.isSelected());
        account.setExcludedFromBudget(excludeBudgetCheckBox.isSelected());

        return account;
    }

    void loadProperties(@NotNull final Account account) {
        baseAccount = account;
        securityNodeSet.clear();

        JavaFXUtils.runLater(() -> {
            setParentAccount(account.getParent());
            nameTextField.setText(account.getName());
            descriptionTextField.setText(account.getDescription());
            accountCodeField.setInteger(account.getAccountCode());
            accountNumberField.setText(account.getAccountNumber());
            bankIdField.setText(account.getBankId());
            setSelectedCurrency(account.getCurrencyNode());
            notesTextArea.setText(account.getNotes());
            lockedCheckBox.setSelected(account.isLocked());
            hideAccountCheckBox.setSelected(!account.isVisible());
            excludeBudgetCheckBox.setSelected(account.isExcludedFromBudget());

            if (baseAccount.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                securityNodeSet.addAll(account.getSecurities());
                updateCommodityText();
            }

            accountTypeComboBox.setValue(account.getAccountType());

            if (baseAccount.getTransactionCount() > 0) {
                placeholderCheckBox.setDisable(true);
            } else {
                placeholderCheckBox.setSelected(account.isPlaceHolder());
            }

            disableSecuritiesBinding.invalidate();
        });
    }

    @FXML
    public void handleSecuritiesButtonAction() {

        final SelectAccountSecuritiesDialog control = new SelectAccountSecuritiesDialog(baseAccount, securityNodeSet);

        if (control.showAndWait()) {
            securityNodeSet.clear();
            securityNodeSet.addAll(control.getSelectedSecurities());

            updateCommodityText();
        }
    }

    private void updateCommodityText() {
        if (!securityNodeSet.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            Iterator<SecurityNode> it = securityNodeSet.iterator();

            SecurityNode node = it.next();
            buf.append(node.getSymbol());
            while (it.hasNext()) {
                buf.append(", ");
                node = it.next();
                buf.append(node.getSymbol());
            }

            JavaFXUtils.runLater(() -> {
                securitiesButton.setText(buf.toString());
                securitiesButton.setTooltip(new Tooltip(buf.toString()));
            });
        } else {
            JavaFXUtils.runLater(() -> securitiesButton.setText(resources.getString("Word.None")));
        }
    }
    @FXML
    private void okAction() {
        result = true;
        ((Stage) nameTextField.getScene().getWindow()).close();
    }

    @FXML
    private void cancelAction() {
        result = false;
        ((Stage) nameTextField.getScene().getWindow()).close();
    }

    private class DisableSecuritiesBinding extends BooleanBinding {

        @Override
        protected boolean computeValue() {
            return !(baseAccount != null && baseAccount.memberOf(AccountGroup.INVEST))
                    && accountTypeComboBox.getValue().getAccountGroup() != AccountGroup.INVEST;

        }
    }

    /**
     * Disables selection of the account type if it is not within the same account group as the account being modified.
     */
    private class DisabledListCell extends ListCell<AccountType> {

        @Override
        protected void updateItem(final AccountType item, final boolean empty) {
            super.updateItem(item, empty);

            if (!empty) {
                setText(item.toString());

                if (baseAccount != null && baseAccount.getAccountType().getAccountGroup() != item.getAccountGroup()) {
                    setId(StyleClass.DISABLED_CELL_ID);
                    setDisable(true);
                } else {
                    setId(StyleClass.ENABLED_CELL_ID);
                    setDisable(false);
                }
            }
        }
    }
}
