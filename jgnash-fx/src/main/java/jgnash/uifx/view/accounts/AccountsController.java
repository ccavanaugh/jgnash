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
package jgnash.uifx.view.accounts;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import org.controlsfx.glyphfont.FontAwesome;

/**
 * @author Craig Cavanaugh
 */
public class AccountsController implements Initializable {

    @FXML
    Button newButton;

    @FXML
    Button modifyButton;

    @FXML
    Button reconcileButton;

    @FXML
    Button deleteButton;

    @FXML
    Button filterButton;

    @FXML
    Button zoomButton;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        newButton.setGraphic(FontAwesome.Glyph.PLUS.create());
        modifyButton.setGraphic(FontAwesome.Glyph.EDIT.create());
        reconcileButton.setGraphic(FontAwesome.Glyph.ADJUST.create());
        deleteButton.setGraphic(FontAwesome.Glyph.REMOVE_SIGN.create());
        filterButton.setGraphic(FontAwesome.Glyph.FILTER.create());
        zoomButton.setGraphic(FontAwesome.Glyph.ZOOM_IN.create());
    }
}
