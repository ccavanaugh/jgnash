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

import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import org.controlsfx.control.ButtonBar;
import org.controlsfx.control.CheckListView;

/**
 * Controller for selecting allowed securities from a list.  If a security is used within the account, checked
 * state will be forced.
 * <p/>
 * A custom selection manager would be a better choice
 *
 * @author Craig Cavanaugh
 */
public class SelectSecuritiesController implements Initializable {

    @FXML
    private CheckListView<LockedDecorator> checkListView;

    @FXML
    private ButtonBar buttonBar;

    private boolean result = false;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        //checkListView.setCellFactory(cell -> new LockedCheckBoxListCell());

        // Create and add the ok and cancel buttons to the button bar
        Button okButton = new Button(resources.getString("Button.Ok"));
        Button cancelButton = new Button(resources.getString("Button.Cancel"));

        buttonBar.addButton(okButton, ButtonBar.ButtonType.OK_DONE);
        buttonBar.addButton(cancelButton, ButtonBar.ButtonType.CANCEL_CLOSE);

        okButton.setOnAction(event -> {
            result = true;
            ((Stage) okButton.getScene().getWindow()).close();
        });

        cancelButton.setOnAction(event -> {
            result = false;
            ((Stage) cancelButton.getScene().getWindow()).close();
        });

        checkListView.getCheckModel().getSelectedItems().addListener((ListChangeListener<LockedDecorator>) change -> forceLockedSecurities());
    }

    private void forceLockedSecurities() {
        // must be pushed later
        checkListView.getItems().stream().filter(LockedDecorator::isLocked).forEach(lockedDecorator ->
                Platform.runLater(() -> checkListView.getCheckModel().select(lockedDecorator)));
    }

    public void loadSecuritiesForAccount(final Account account) {

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Set<SecurityNode> usedSecurities = Collections.<SecurityNode>emptySet();

        if (account != null) {
            usedSecurities = account.getUsedSecurities();
        }

        final ObservableList<LockedDecorator> items = checkListView.getItems();

        for (SecurityNode node : engine.getSecurities()) {
            if (usedSecurities.contains(node)) {
                items.add(new LockedDecorator(node, true));
            } else {
                items.add(new LockedDecorator(node, false));
            }
        }

        forceLockedSecurities();
    }

    public boolean getResult() {
        return result;
    }

    private class LockedDecorator {

        private boolean locked;

        private SecurityNode securityNode;

        protected LockedDecorator(final SecurityNode securityNode, final boolean locked) {
            this.securityNode = securityNode;
            this.locked = locked;
        }

        @Override
        public String toString() {
            return securityNode.toString();
        }

        public boolean isLocked() {
            return locked;
        }
    }

    public Set<SecurityNode> getSelectedSecurities() {
        Set<SecurityNode> securityNodeSet = new TreeSet<>();

        securityNodeSet.addAll(checkListView.getCheckModel().getSelectedItems().stream().map(lockedDecorator -> lockedDecorator.securityNode).collect(Collectors.toList()));

        return securityNodeSet;
    }

    private class LockedCheckBoxListCell extends CheckBoxListCell<LockedDecorator> {

        @Override
        public void updateItem(final LockedDecorator item, final boolean empty) {
            super.updateItem(item, empty);  // required

            if (!empty) {
                if (item.isLocked()) {
                    setStyle("-fx-text-fill:red;");
                }
            }
        }
    }
}
