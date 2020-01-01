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
package jgnash.uifx.views.register;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import jgnash.util.LogUtil;

import java.io.IOException;

/**
 * Locked Investment Register pane controller.
 *
 * @author Craig Cavanaugh
 */
public class LockedInvestmentRegisterPaneController extends RegisterPaneController {

    @FXML
    @Override
    void initialize() {
        super.initialize();

        // Load the register table
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvestmentRegisterTable.fxml"), resources);
            registerTablePane.getChildren().add(fxmlLoader.load());
            registerTableController.set(fxmlLoader.getController());
        } catch (final IOException e) {
            LogUtil.logSevere(LockedInvestmentRegisterPaneController.class, e);
        }
    }
}
