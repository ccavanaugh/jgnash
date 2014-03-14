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
import java.util.ResourceBundle;

import jgnash.MainFX;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.CurrencyComboBox;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author Craig Cavanaugh
 */
public class AccountPropertiesController implements Initializable {

    @FXML
    private ComboBox<AccountType> accountTypeComboBox;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

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

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        accountTypeComboBox.getItems().addAll(AccountType.values());
        accountTypeComboBox.setValue(AccountType.BANK); // set default
    }

    public CurrencyNode getSelectedCurrency() {
        return currencyComboBox.getValue();
    }

    public void setSelectedCurrency(final CurrencyNode currency) {
        currencyComboBox.setValue(currency);
    }

    @FXML
    private void handleParentAccountAction(final ActionEvent actionEvent) {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.ParentAccount"));

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("fxml/SelectAccountForm.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            dialog.setResizable(false);

            dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
            dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
