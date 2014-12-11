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
package jgnash.uifx;

import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.utils.StageUtils;

import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author Craig Cavanaugh
 */
public class DecimalFieldTest extends Application {

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        // Force application wide style sheet. Use is StyleManager is a private API and may break later
        Application.setUserAgentStylesheet(null);
        StyleManager.getInstance().addUserAgentStylesheet(MainApplication.DEFAULT_CSS);

        DecimalTextField decimalTextField = new DecimalTextField();

        TextField dummyField = new TextField(); // help to change focus to test eval function

        primaryStage.setTitle("Decimal Field Test");
        Button btn = new Button();

        btn.setText("getDecimal()");
        btn.setOnAction(event -> System.out.println(decimalTextField.getDecimal()));

        VBox vBox = new VBox();
        vBox.getChildren().addAll(decimalTextField, dummyField, btn);

        primaryStage.setScene(new Scene(vBox, 300, 250));

        StageUtils.applyDialogFormCSS(primaryStage);

        primaryStage.show();
    }
}
