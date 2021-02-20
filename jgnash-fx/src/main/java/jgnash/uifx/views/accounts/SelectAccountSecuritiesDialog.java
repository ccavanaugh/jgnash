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
package jgnash.uifx.views.accounts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.control.LockedCommodityListCell;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.LockedCommodityNode;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Dialog for selecting allowed account securities from a list.  If a security is used within the account, selection
 * will be forced.
 *
 * @author Craig Cavanaugh
 */
public class SelectAccountSecuritiesDialog {

    private final ResourceBundle resources = ResourceUtils.getBundle();

    private Button moveToTarget;
    private Button moveToSource;

    private final ListView<LockedCommodityNode<SecurityNode>> sourceListView = new ListView<>();
    private final ListView<LockedCommodityNode<SecurityNode>> targetListView = new ListView<>();

    private boolean result;
    
    public SelectAccountSecuritiesDialog(@Nullable final Account account, @NotNull Set<SecurityNode> preSelected) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        sourceListView.setCellFactory(param -> new LockedCommodityListCell<>());
        targetListView.setCellFactory(param -> new LockedCommodityListCell<>());

        Set<SecurityNode> usedSecurities = new HashSet<>();
        Set<SecurityNode> selectedSecurityNodes = new HashSet<>();

        if (account != null) {
            usedSecurities = account.getUsedSecurities();
            selectedSecurityNodes = account.getSecurities();
        }

        // Add the preselected set
        selectedSecurityNodes.addAll(preSelected);

        for (final SecurityNode securityNode : selectedSecurityNodes) {
            if (usedSecurities.contains(securityNode)) {
                targetListView.getItems().add(new LockedCommodityNode<>(securityNode, true));
            } else {
                targetListView.getItems().add(new LockedCommodityNode<>(securityNode, false));
            }
        }

        for (final SecurityNode securityNode : engine.getSecurities()) {
            if (!selectedSecurityNodes.contains(securityNode)) {
                sourceListView.getItems().add(new LockedCommodityNode<>(securityNode, false));
            }
        }

        FXCollections.sort(sourceListView.getItems());
        FXCollections.sort(targetListView.getItems());
    }

    public boolean showAndWait() {

        // Create the base dialog
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainView.getPrimaryStage());
        dialog.setTitle(resources.getString("Title.AccountSecurities"));

        final Label availableLabel = new Label(resources.getString("Title.Available"));
        availableLabel.getStyleClass().add(StyleClass.LIST_TITLE_STYLE);

        final Label currentLabel = new Label(resources.getString("Title.Current"));
        currentLabel.getStyleClass().add(StyleClass.LIST_TITLE_STYLE);

        final GridPane gridPane = createGridPane();
        gridPane.add(availableLabel, 0, 0);
        gridPane.add(sourceListView, 0, 1);
        gridPane.add(createButtonBox(), 1, 1);
        gridPane.add(currentLabel, 2, 0);
        gridPane.add(targetListView, 2, 1);
        gridPane.add(createButtonBar(), 0, 2, 3, 1);

        dialog.setScene(new Scene(gridPane));
        ThemeManager.applyStyleSheets(dialog.getScene());
        dialog.getScene().getRoot().styleProperty().bind(ThemeManager.styleProperty());
        dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

        StageUtils.addBoundsListener(dialog, this.getClass(), MainView.getPrimaryStage());

        dialog.showAndWait();

        return result;
    }

    private ButtonBar createButtonBar() {
        final ButtonBar buttonBar = new ButtonBar();

        // Create and add the ok and cancel buttons to the button bar
        final Button okButton = new Button(resources.getString("Button.Ok"));
        final Button cancelButton = new Button(resources.getString("Button.Cancel"));

        ButtonBar.setButtonData(okButton, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);

        buttonBar.getButtons().addAll(okButton, cancelButton);

        okButton.setOnAction(event -> {
            result = true;
            ((Stage) okButton.getScene().getWindow()).close();
        });

        cancelButton.setOnAction(event -> {
            result = false;
            ((Stage) cancelButton.getScene().getWindow()).close();
        });

        return buttonBar;
    }

    private VBox createButtonBox() {
        final VBox vBox = new VBox();
        vBox.setFillWidth(true);
        vBox.getStyleClass().add("form");

        moveToTarget = new Button("", new MaterialDesignLabel(MaterialDesignLabel.MDIcon.CHEVRON_RIGHT));
        moveToTarget.getStyleClass().add(StyleClass.LIST_BUTTON_STYLE);

        moveToSource = new Button("", new MaterialDesignLabel(MaterialDesignLabel.MDIcon.CHEVRON_LEFT));
        moveToSource.getStyleClass().add(StyleClass.LIST_BUTTON_STYLE);

        moveToTarget.setMaxWidth(Double.MAX_VALUE);
        moveToSource.setMaxWidth(Double.MAX_VALUE);

        sourceListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        targetListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        sourceListView.selectionModelProperty().addListener(listener -> bindMoveButtonsToListItems());
        targetListView.selectionModelProperty().addListener(listener -> bindMoveButtonsToListItems());

        bindMoveButtonsToListItems();

        moveToTarget.setOnAction(event -> moveItemsToTarget());
        moveToSource.setOnAction(event -> moveItemsToSource());

        vBox.getChildren().addAll(moveToTarget, moveToSource);

        return vBox;
    }

    private GridPane createGridPane() {
        final GridPane gridPane = new GridPane();
        gridPane.getStyleClass().add("form");

        final ColumnConstraints col0 = new ColumnConstraints();
        col0.setFillWidth(true);
        col0.setHgrow(Priority.ALWAYS);
        col0.setMaxWidth(Double.MAX_VALUE);
        col0.setPrefWidth(220);

        final ColumnConstraints col1 = new ColumnConstraints();
        col1.setFillWidth(true);
        col1.setHgrow(Priority.NEVER);

        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setFillWidth(true);
        col2.setHgrow(Priority.ALWAYS);
        col2.setMaxWidth(Double.MAX_VALUE);
        col2.setPrefWidth(220);

        gridPane.getColumnConstraints().addAll(col0, col1, col2);

        final RowConstraints row0 = new RowConstraints();
        row0.setFillHeight(true);
        row0.setVgrow(Priority.NEVER);

        final RowConstraints row1 = new RowConstraints();
        row1.setMaxHeight(Double.MAX_VALUE);
        row1.setPrefHeight(220);
        row1.setVgrow(Priority.ALWAYS);

        final RowConstraints row2 = new RowConstraints();
        row0.setFillHeight(true);
        row0.setVgrow(Priority.NEVER);

        gridPane.getRowConstraints().addAll(row0, row1, row2);

        return gridPane;
    }

    private void bindMoveButtonsToListItems() {
        moveToTarget.disableProperty().bind(Bindings.isEmpty(sourceListView.getItems()));
        moveToSource.disableProperty().bind(Bindings.isEmpty(targetListView.getItems()));
    }

    synchronized private void moveItems(final ListView<LockedCommodityNode<SecurityNode>> sourceView,
                                        final ListView<LockedCommodityNode<SecurityNode>> destinationView) {
        final List<LockedCommodityNode<SecurityNode>> selectedItems
                = new ArrayList<>(sourceView.getSelectionModel().getSelectedItems());

        // filter out any locked items
        selectedItems.removeIf(LockedCommodityNode::isLocked);

        moveItems(sourceView, destinationView, selectedItems);
    }

    synchronized private void moveItems(final ListView<LockedCommodityNode<SecurityNode>> sourceView,
                                        final ListView<LockedCommodityNode<SecurityNode>> destinationView,
                                        final List<LockedCommodityNode<SecurityNode>> items) {
        for (final LockedCommodityNode<SecurityNode> item : items) {
            sourceView.getItems().remove(item);
            destinationView.getItems().add(item);

            // Sort the destination list
            FXCollections.sort(destinationView.getItems());
        }
    }

    synchronized private void moveItemsToTarget() {
        moveItems(sourceListView, targetListView);
        sourceListView.getSelectionModel().clearSelection();
    }

    synchronized private void moveItemsToSource() {
        moveItems(targetListView, sourceListView);
        targetListView.getSelectionModel().clearSelection();
    }

    public Set<SecurityNode> getSelectedSecurities() {
        return targetListView.getItems().stream().map(LockedCommodityNode::getNode)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
