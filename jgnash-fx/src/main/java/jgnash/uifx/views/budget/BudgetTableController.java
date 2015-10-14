package jgnash.uifx.views.budget;

import java.time.LocalDate;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetResultsModel;

/**
 * @author Craig Cavanaugh
 */
public class BudgetTableController {

    private static final String HIDE_HORIZONTAL_CSS = "jgnash/skin/tableHideHorizontalScrollBar.css";
    private static final String HIDE_VERTICAL_CSS = "jgnash/skin/tableHideVerticalScrollBar.css";
    private static final String HIDE_HEADER_CSS = "jgnash/skin/tableHideColumnHeader.css";

    @FXML
    private Spinner<Integer> yearSpinner;

    @FXML
    private ScrollBar verticalScrollBar;

    @FXML
    private ScrollBar horizontalScrollBar;

    @FXML
    private TreeView<Account> accountTreeView;

    @FXML
    private TableView<Object> dataTable;

    @FXML
    private TableView accountSummaryTable;

    @FXML
    private TableView periodSummaryTable;

    @FXML
    private TableView accountPeriodSummaryTable;

    @FXML
    private TableView accountTypeTable;

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<Budget> budgetProperty = new SimpleObjectProperty<>();

    private BudgetResultsModel model;

    @FXML
    private void initialize() {
        yearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                2000, 2200,
                LocalDate.now().getYear(), 1));

        accountTreeView.getStylesheets().addAll(HIDE_VERTICAL_CSS);
        accountTreeView.setShowRoot(false);

        dataTable.getStylesheets().addAll(HIDE_VERTICAL_CSS, HIDE_HORIZONTAL_CSS);

        accountSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountSummaryTable.getStylesheets().add(HIDE_VERTICAL_CSS);

        accountTypeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountPeriodSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        accountTypeTable.getStylesheets().add(HIDE_HEADER_CSS);
        periodSummaryTable.getStylesheets().add(HIDE_HEADER_CSS);
        accountPeriodSummaryTable.getStylesheets().add(HIDE_HEADER_CSS);

        /*Platform.runLater(() -> {
            hideHeader(accountTypeTable);
            hideHeader(periodSummaryTable);
            hideHeader(accountPeriodSummaryTable);
        });*/

        budgetProperty.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(BudgetTableController.this::handleBudgetChange);  // push change to end of EDT
        });
    }

    SimpleObjectProperty<Budget> budgetProperty() {
        return budgetProperty;
    }

    private void bindScrollBars() {
        final ScrollBar periodSummaryBar = findHorizontalScrollBar(periodSummaryTable);
        final ScrollBar hDataScrollBar = findHorizontalScrollBar(dataTable);

        horizontalScrollBar.minProperty().bindBidirectional(hDataScrollBar.minProperty());
        horizontalScrollBar.maxProperty().bindBidirectional(hDataScrollBar.maxProperty());
        horizontalScrollBar.valueProperty().bindBidirectional(hDataScrollBar.valueProperty());

        periodSummaryBar.valueProperty().bindBidirectional(hDataScrollBar.valueProperty());

        final ScrollBar accountScrollBar = findVerticalScrollBar(accountTreeView);
        final ScrollBar vDataScrollBar = findVerticalScrollBar(dataTable);
        final ScrollBar accountSumScrollBar = findVerticalScrollBar(accountSummaryTable);

        verticalScrollBar.minProperty().bindBidirectional(accountScrollBar.minProperty());
        verticalScrollBar.maxProperty().bindBidirectional(accountScrollBar.maxProperty());
        verticalScrollBar.valueProperty().bindBidirectional(accountScrollBar.valueProperty());

        accountScrollBar.valueProperty().bindBidirectional(vDataScrollBar.valueProperty());
        accountSumScrollBar.valueProperty().bindBidirectional(vDataScrollBar.valueProperty());
    }

    private void handleBudgetChange() {
        if (budgetProperty.get() != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            model = new BudgetResultsModel(budgetProperty.get(), yearSpinner.getValue(), engine.getDefaultCurrency());

            loadAccounts();

            bindScrollBars();
        } else {
            // TODO: Clear tables
            System.out.println("budget was cleared");
            accountTreeView.setRoot(null);
        }
    }

    private void loadAccounts() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final TreeItem<Account> root = new TreeItem<>(engine.getRootAccount());
        root.setExpanded(true);

        accountTreeView.setRoot(root);
        loadChildren(root);
    }

    private synchronized void loadChildren(final TreeItem<Account> parentItem) {
        final Account parent = parentItem.getValue();

        parent.getChildren(Comparators.getAccountByCode()).stream().filter(model::includeAccount).forEach(child ->
        {
            final TreeItem<Account> childItem = new TreeItem<>(child);
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);

            if (child.getChildCount() > 0) {
                loadChildren(childItem);
            }
        });
    }

    private ScrollBar findVerticalScrollBar(final Node table) {
        for (final Node node : table.lookupAll(".scroll-bar:vertical")) {
            if (node instanceof ScrollBar) {
                if (((ScrollBar)node).getOrientation() == Orientation.VERTICAL) {
                    return (ScrollBar) node;
                }
            }
        }

        throw new RuntimeException("Could not find horizontal scrollbar");
    }

    private ScrollBar findHorizontalScrollBar(final Node table) {
        for (final Node node : table.lookupAll(".scroll-bar:horizontal")) {
            if (node instanceof ScrollBar) {
                if (((ScrollBar)node).getOrientation() == Orientation.HORIZONTAL) {
                    return (ScrollBar) node;
                }
            }
        }

        throw new RuntimeException("Could not find horizontal scrollbar");
    }

    /*private void hideHeader(final TableView<?> table) {
        final Pane header = (Pane) table.lookup("TableHeaderRow");
        if (header.isVisible()){
            header.setMaxHeight(0);
            header.setMinHeight(0);
            header.setPrefHeight(0);
            header.setVisible(false);
        }
    }*/
}
