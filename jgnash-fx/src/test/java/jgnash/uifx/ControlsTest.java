/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.uifx;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.DefaultCurrencies;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.uifx.resource.font.FontAwesomeLabel;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.DetailedDecimalTextField;
import jgnash.uifx.control.PopOverButton;
import jgnash.uifx.control.SecurityComboBox;
import jgnash.uifx.control.TextFieldEx;
import jgnash.uifx.control.TimePeriodComboBox;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.uifx.skin.ThemeManager;
import jgnash.util.LogUtil;

import static jgnash.uifx.views.main.MainView.DEFAULT_CSS;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI controls test application.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings({"WeakerAccess"})
public class ControlsTest extends Application {

    private String testFile;

    private String tempFile;

    public static void main(final String[] args) {
        //Locale.setDefault(Locale.FRANCE);

        launch(args);
    }
   
	@Override
    public void start(final Stage primaryStage) {

        Engine engine = createEngine();
        Objects.requireNonNull(engine);

        ThemeManager.restoreLastUsedTheme();

        DecimalTextField decimalTextField = new DecimalTextField();
        DecimalTextField decimalTextField2 = new DecimalTextField();
        decimalTextField2.scaleProperty().set(4);

        primaryStage.setTitle("Controls Test");
        Button btn = new Button();

        btn.setText("getDecimal()");

        // Create the DatePicker.
        DatePicker datePicker = new DatePickerEx();

        datePicker.setOnAction(event -> {
            LocalDate date = datePicker.getValue();
            System.out.println("Selected date: " + date);
        });

        decimalTextField.decimalProperty().addListener((observable, oldValue, newValue)
                -> System.out.println("decimalTextField: " + newValue));

        decimalTextField2.decimalProperty().addListener((observable, oldValue, newValue)
                -> System.out.println("decimalTextField2: " + newValue));

        ObjectProperty<BigDecimal> decimal = new SimpleObjectProperty<>();

        decimalTextField2.decimalProperty().bindBidirectional(decimal);
        decimal.set(BigDecimal.TEN);

        btn.setOnAction(event -> {
            decimal.set(BigDecimal.ONE);
            System.out.println(decimalTextField2.getDecimal());
        });

        System.out.println(decimal.isBound());
        System.out.println(decimalTextField2.decimalProperty().isBound());

        TransactionNumberComboBox numberComboBox = new TransactionNumberComboBox();
        numberComboBox.accountProperty().set(engine.getAccountList().get(0));

        Button exceptionButton = new Button("Show Exception");
        exceptionButton.setOnAction(event -> StaticUIMethods.displayException(new Exception("Test exception")));

        SecurityComboBox securityComboBox = new SecurityComboBox();

        TextFieldEx textFieldEx = new TextFieldEx();

        PopOverButton popOverButton = new PopOverButton(new FontAwesomeLabel(FontAwesomeLabel.FAIcon.EXCHANGE));
        popOverButton.setContentNode(new DecimalTextField());

        DetailedDecimalTextField detailedDecimalTextField2 = new DetailedDecimalTextField();

        VBox vBox = new VBox();
        vBox.getChildren().addAll(decimalTextField, decimalTextField2, datePicker, new AccountComboBox(),
                numberComboBox, btn, exceptionButton, securityComboBox, new TimePeriodComboBox(), textFieldEx,
                popOverButton, detailedDecimalTextField2);

        primaryStage.setScene(new Scene(vBox, 300, 420));

        primaryStage.getScene().getStylesheets().add(DEFAULT_CSS);
        primaryStage.getScene().getRoot().getStyleClass().addAll("form", "dialog");

        primaryStage.show();
        primaryStage.requestFocus();
    }

    @Override
    public void stop() throws IOException {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        Files.deleteIfExists(Paths.get(testFile));

        cleanup();
    }

    private Engine createEngine() {
        try {
            testFile = Files.createTempFile("test",
                    DataStoreType.BINARY_XSTREAM.getDataStore().getFileExt()).toFile().getAbsolutePath();
            tempFile = testFile;
        } catch (IOException e1) {
            LogUtil.logSevere(ControlsTest.class, e1);
        }

        EngineFactory.deleteDatabase(testFile);

        final Engine engine = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT,
                EngineFactory.EMPTY_PASSWORD, DataStoreType.BINARY_XSTREAM);

        Objects.requireNonNull(engine);

        CurrencyNode node = engine.getDefaultCurrency();

        if (!node.getSymbol().equals("USD")) {
            engine.setDefaultCurrency(DefaultCurrencies.buildNode(Locale.US));
        }

        node = engine.getCurrency("CAD");

        if (node == null) {
            node = DefaultCurrencies.buildNode(Locale.CANADA);
            assertNotNull(node);
            assertTrue(engine.addCurrency(node));
        }

        Account account = new Account(AccountType.BANK, engine.getDefaultCurrency());
        account.setName("Bank Accounts");

        engine.addAccount(engine.getRootAccount(), account);

        SecurityNode securityNode = new SecurityNode();
        securityNode.setSymbol("GGG");
        securityNode.setDescription("Google");
        securityNode.setReportedCurrencyNode(engine.getDefaultCurrency());

        engine.addSecurity(securityNode);

        securityNode = new SecurityNode();
        securityNode.setSymbol("MSFT");
        securityNode.setDescription("Microsoft");
        securityNode.setReportedCurrencyNode(engine.getDefaultCurrency());

        engine.addSecurity(securityNode);

        return engine;
    }

    private void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(tempFile));
        Files.deleteIfExists(Paths.get(tempFile + ".backup"));
    }
}
