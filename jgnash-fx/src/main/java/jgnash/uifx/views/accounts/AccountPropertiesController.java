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
package jgnash.uifx.views.accounts;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.MainFX;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.SecurityNode;
import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.CurrencyComboBox;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Loads all account properties into a form and returns a template Account based on the form properties
 *
 * @author Craig Cavanaugh
 */
public class AccountPropertiesController implements Initializable {

    private boolean result = false;

    @FXML
    private ComboBox<AccountType> accountTypeComboBox;

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private TextArea notesTextArea;

    @FXML
    private TextField nameTextField;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private TextField accountIdField;

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

    private ResourceBundle resources;

    private Account parentAccount;

    private Account baseAccount = null;

    private Set<SecurityNode> securityNodeSet = new TreeSet<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        this.resources = resources;

        accountTypeComboBox.getItems().addAll(AccountType.values());
        accountTypeComboBox.setValue(AccountType.BANK); // set default

        // Create and add the ok and cancel buttons to the button bar
        final Button okButton = new Button(resources.getString("Button.Ok"));
        final Button cancelButton = new Button(resources.getString("Button.Cancel"));

        ButtonBar.setButtonData(okButton, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);

        buttonBar.getButtons().addAll(okButton, cancelButton);

        descriptionTextField.setText(resources.getString("Word.Description"));
        nameTextField.setText(resources.getString("Word.Name"));

        okButton.setOnAction(event -> {
            result = true;
            ((Stage) okButton.getScene().getWindow()).close();
        });

        cancelButton.setOnAction(event -> {
            result = false;
            ((Stage) cancelButton.getScene().getWindow()).close();
        });
    }

    public void setSelectedCurrency(final CurrencyNode currency) {
        currencyComboBox.setValue(currency);
    }

    @FXML
    private void handleParentAccountAction(final ActionEvent actionEvent) {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.ParentAccount"));

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("fxml/SelectAccountForm.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            SelectAccountController controller = loader.getController();

            dialog.setResizable(false);

            dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
            dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            if (parentAccount != null) {
                controller.setSelectedAccount(parentAccount);
            }

            dialog.showAndWait();

            setParentAccount(controller.getSelectedAccount());
        } catch (final IOException ex) {
            Logger.getLogger(AccountPropertiesController.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    public boolean getResult() {
        return result;
    }

    public Set<SecurityNode> getSecurityNodes() {
        return securityNodeSet;
    }

    public void setParentAccount(final Account parentAccount) {
        this.parentAccount = parentAccount;

        Platform.runLater(() -> {
            if (parentAccount != null) {
                parentAccountButton.setText(parentAccount.getName());
            }
        });
    }

    public Account getTemplate() {
        Account account = new Account(accountTypeComboBox.getValue(), currencyComboBox.getValue());

        account.setAccountNumber(accountIdField.getText());
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

        if (account.getAccountType().getAccountGroup().equals(AccountGroup.INVEST)) {
            securityNodeSet.forEach(account::addSecurity);
        }

        return account;
    }

    public void loadProperties(final Account account) {
        baseAccount = account;
        securityNodeSet.clear();

        Platform.runLater(() -> {
            setParentAccount(account.getParent());
            nameTextField.setText(account.getName());
            descriptionTextField.setText(account.getDescription());
            accountIdField.setText(account.getAccountNumber());
            bankIdField.setText(account.getBankId());
            setSelectedCurrency(account.getCurrencyNode());
            notesTextArea.setText(account.getNotes());
            lockedCheckBox.setSelected(account.isLocked());
            hideAccountCheckBox.setSelected(!account.isVisible());
            excludeBudgetCheckBox.setSelected(account.isExcludedFromBudget());

            if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                securityNodeSet.addAll(account.getSecurities());
                updateCommodityText();
            }

            accountTypeComboBox.setValue(account.getAccountType());

            if (account.getTransactionCount() > 0) {
                placeholderCheckBox.setDisable(true);
            } else {
                placeholderCheckBox.setSelected(account.isPlaceHolder());
            }
        });
    }

    @FXML
    public void handleSecuritiesButtonAction(final ActionEvent actionEvent) {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.AccountSecurities"));

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("fxml/SelectSecuritiesForm.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            SelectSecuritiesController controller = loader.getController();

            dialog.setResizable(false);

            dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
            dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            controller.loadSecuritiesForAccount(baseAccount);

            dialog.showAndWait();

            if (controller.getResult()) {
                securityNodeSet.clear();
                securityNodeSet.addAll(controller.getSelectedSecurities());

                updateCommodityText();
            }
        } catch (final IOException ex) {
            Logger.getLogger(AccountPropertiesController.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
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

            Platform.runLater(() -> {
                securitiesButton.setText(buf.toString());
                securitiesButton.setTooltip(new Tooltip(buf.toString()));
            });
        } else {
            Platform.runLater(() -> securitiesButton.setText(resources.getString("Word.None")));
        }
    }
}
