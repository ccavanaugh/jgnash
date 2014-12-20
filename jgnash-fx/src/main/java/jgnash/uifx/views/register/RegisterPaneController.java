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
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import jgnash.engine.Account;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Register pane
 *
 * @author Craig Cavanaugh
 */
public class RegisterPaneController implements Initializable {
    //private ResourceBundle resources;

    @FXML
    protected Button newButton;

    @FXML
    protected Button duplicateButton;

    @FXML
    protected Button jumpButton;

    @FXML
    protected Button deleteButton;

    @FXML
    protected StackPane register;

    @FXML
    protected TabPane transactionForms;

    /**
     * Active account for the pane
     */
    private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private RegisterTableController registerTableController;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        //this.resources = resources;

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        deleteButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.TRASH_ALT));
        duplicateButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.COPY));
        jumpButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EXTERNAL_LINK));
        newButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.MONEY));

        transactionForms.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);


        // Load the register table
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RegisterTable.fxml"), resources);
            register.getChildren().add(fxmlLoader.load());
            registerTableController = fxmlLoader.getController();

            // Bind  the register pane to this account property
            registerTableController.getAccountProperty().bind(accountProperty);
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TransactionPane.fxml"), resources);

            final Pane pane = fxmlLoader.load();

            TransactionPanelController transactionPanelController = fxmlLoader.getController();

            accountProperty.addListener(new ChangeListener<Account>() {
                @Override
                public void changed(ObservableValue<? extends Account> observable, Account oldValue, Account newValue) {
                    transactionPanelController.getAccountProperty().setValue(newValue);
                }
            });

            final Tab tab = new Tab("Increase");
            tab.setContent(pane);

            transactionForms.getTabs().addAll(tab);
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TransactionPane.fxml"), resources);

            final Pane pane = fxmlLoader.load();

            TransactionPanelController transactionPanelController = fxmlLoader.getController();

            accountProperty.addListener(new ChangeListener<Account>() {
                @Override
                public void changed(ObservableValue<? extends Account> observable, Account oldValue, Account newValue) {
                    transactionPanelController.getAccountProperty().setValue(newValue);
                }
            });

            final Tab tab = new Tab("Decrease");
            tab.setContent(pane);

            transactionForms.getTabs().addAll(tab);
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    @FXML
    public void handleDeleteAction(final ActionEvent actionEvent) {

    }
}
