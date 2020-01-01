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

package jgnash.uifx.dialog.options;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.fxml.FXML;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.control.TextFieldEx;
import jgnash.util.LogUtil;

import static jgnash.net.security.iex.IEXParser.IEX_SECRET_KEY;

/**
 * Controller for configuring Data Providers
 *
 * @author Craig Cavanaugh
 */
public class DataProviderTabController {

    @FXML
    private TextFieldEx iexPrivateKeyTextField;

    @FXML
    private void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            final String key = engine.getPreference(IEX_SECRET_KEY);

            if (key != null && key.length() > 0) {
                iexPrivateKeyTextField.setText(key);
            }

            // save on loss of focus
            iexPrivateKeyTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    handleIexCloudKey();
                }
            });
        } else {
            iexPrivateKeyTextField.setDisable(true);
        }
    }

    @FXML
    private void handleIexCloudKey() {
        final String key = iexPrivateKeyTextField.getText();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        engine.setPreference(IEX_SECRET_KEY, key);
    }

    @FXML
    private void handleHyperLink() {
        if (Desktop.isDesktopSupported()) {
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://iexcloud.io"));
                } catch (IOException | URISyntaxException ioe) {
                    LogUtil.logSevere(DataProviderTabController.class, ioe);
                }
            }).start();
        }
    }
}
