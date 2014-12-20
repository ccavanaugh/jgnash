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

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import jgnash.engine.Account;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.TransactionNumberComboBox;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Transaction Entry Controller
 *
 * @author Craig Cavanaugh
 */
public class TransactionPanelController implements Initializable {

    @FXML
    protected TextField payeeTextField;

    @FXML
    protected AccountComboBox accountComboBox;

    @FXML
    protected Button splitsButton;

    @FXML
    protected TransactionNumberComboBox numberComboBox;

    @FXML
    protected DatePickerEx datePicker;

    @FXML
    protected DecimalTextField amountTextField;

    @FXML
    protected TextField memoTextField;

    @FXML
    protected HBox exchangePanel;

    @FXML
    protected ButtonBar buttonBar;

    @FXML
    protected CheckBox reconciledButton;

    @FXML
    protected Button attachmentButton;

    @FXML
    protected Button viewAttachmentButton;

    @FXML
    protected Button deleteAttachmentButton;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        // Create and add the ok and cancel buttons to the button bar
        final Button okButton = new Button(resources.getString("Button.Ok"));
        final Button cancelButton = new Button(resources.getString("Button.Cancel"));

        ButtonBar.setButtonData(okButton, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);
        buttonBar.getButtons().addAll(okButton, cancelButton);

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        attachmentButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.LINK));
        viewAttachmentButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EYE));
        deleteAttachmentButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.UNLINK));

        getAccountProperty().addListener(new ChangeListener<Account>() {
            @Override
            public void changed(final ObservableValue<? extends Account> observable, final Account oldValue, final Account newValue) {
                numberComboBox.setAccount(newValue);
            }
        });

    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }
}
