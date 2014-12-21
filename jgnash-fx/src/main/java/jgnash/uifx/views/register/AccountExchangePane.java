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
package jgnash.uifx.views.register;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DecimalTextField;
import jgnash.util.ResourceUtils;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Controller for handling the exchange of currencies
 *
 * @author Craig Cavanaugh
 */
public class AccountExchangePane extends GridPane implements Initializable {

    @FXML
    private AccountComboBox accountCombo;

    @FXML
    private DecimalTextField exchangeRateField;

    @FXML
    private Button expandButton;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    final private ObjectProperty<CurrencyNode> currencyProperty = new SimpleObjectProperty<>();

    public AccountExchangePane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AccountExchangePane.fxml"), ResourceUtils.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        expandButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EXCHANGE));

        accountProperty.addListener(new ChangeListener<Account>() {
            @Override
            public void changed(final ObservableValue<? extends Account> observable, final Account oldValue, final Account newValue) {
                accountCombo.filterAccount(newValue);
            }
        });

    }

    public ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    public ObjectProperty<CurrencyNode> getCurrencyProperty() {
        return currencyProperty;
    }

    public Account getSelectedAccount() {
        return accountCombo.getValue();
    }

    public void setSelectedAccount(final Account account) {
        accountCombo.setValue(account);
    }
}
