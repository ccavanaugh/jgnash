package jgnash.uifx.views.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
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
    private TreeTableView<Account> accountTreeView;

    @FXML
    private TableView<Account> dataTable;

    @FXML
    private TableView<Account> accountSummaryTable;

    @FXML
    private TableView periodSummaryTable;

    @FXML
    private TableView accountPeriodSummaryTable;

    @FXML
    private TableView<AccountGroup> accountTypeTable;

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<Budget> budgetProperty = new SimpleObjectProperty<>();

    private BudgetResultsModel budgetResultsModel;

    /** This list is updated to track the expanded rows of the TreeTableView.
     * This should be the model for all account specific tables
     */
    private final ObservableList<Account> expandedAccountList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        yearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                2000, 2200,
                LocalDate.now().getYear(), 1));

        accountTreeView.getStylesheets().addAll(HIDE_VERTICAL_CSS);
        accountTreeView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        accountTreeView.setShowRoot(false);

        dataTable.getStylesheets().addAll(HIDE_VERTICAL_CSS, HIDE_HORIZONTAL_CSS);
        dataTable.setItems(expandedAccountList);

        accountSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountSummaryTable.getStylesheets().add(HIDE_VERTICAL_CSS);
        accountSummaryTable.setItems(expandedAccountList);

        accountTypeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountPeriodSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        accountTypeTable.getStylesheets().add(HIDE_HEADER_CSS);
        periodSummaryTable.getStylesheets().add(HIDE_HEADER_CSS);
        accountPeriodSummaryTable.getStylesheets().add(HIDE_HEADER_CSS);

        buildAccountTreeTable();
        buildAccountTypeTable();
        buildAccountSummaryTable();

        accountTreeView.expandedItemCountProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(this::updateExpandedAccountList);
        });

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

            budgetResultsModel = new BudgetResultsModel(budgetProperty.get(), yearSpinner.getValue(), engine.getDefaultCurrency());

            loadModel();

            bindScrollBars();
        } else {
            // TODO: Clear tables
            System.out.println("budget was cleared");
            accountTreeView.setRoot(null);
            accountTypeTable.getItems().clear();
        }
    }

    /**
     * Maintains the list of expanded accounts
     */
    private synchronized void updateExpandedAccountList() {
        final int count = accountTreeView.getExpandedItemCount();

        // Create a new list and update the observable list in one shot to minimize visual updates
        final List<Account> accountList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            accountList.add(accountTreeView.getTreeItem(i).getValue());
        }

        expandedAccountList.setAll(accountList);
    }

    private void loadModel() {
        loadAccounts();
        loadAccountTypes();

        Platform.runLater(this::updateExpandedAccountList);
    }

    private void loadAccounts() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final TreeItem<Account> root = new TreeItem<>(engine.getRootAccount());
        root.setExpanded(true);

        accountTreeView.setRoot(root);
        loadChildren(root);
    }

    private void loadAccountTypes() {
        accountTypeTable.getItems().setAll(budgetResultsModel.getAccountGroupList());
    }

    private synchronized void loadChildren(final TreeItem<Account> parentItem) {
        final Account parent = parentItem.getValue();

        parent.getChildren(Comparators.getAccountByCode()).stream().filter(budgetResultsModel::includeAccount).forEach(child ->
        {
            final TreeItem<Account> childItem = new TreeItem<>(child);
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);

            if (child.getChildCount() > 0) {
                loadChildren(childItem);
            }
        });
    }

    private void buildAccountTreeTable() {
        // empty column header is needed
        final TreeTableColumn<Account, String> headerColumn = new TreeTableColumn<>("");

        final TreeTableColumn<Account, String> nameColumn = new TreeTableColumn<>(resources.getString("Column.Account"));
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getName()));

        headerColumn.getColumns().add(nameColumn);

        accountTreeView.getColumns().add(headerColumn);
    }

    private void buildAccountTypeTable() {
        final TableColumn<AccountGroup, String> nameColumn = new TableColumn<>(resources.getString("Column.Type"));
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().toString()));

        accountTypeTable.getColumns().add(nameColumn);
    }

    private void buildAccountSummaryTable() {

        //TODO, add cell renderers

        final TableColumn<Account, ?> headerColumn = new TableColumn<>(resources.getString("Title.Summary"));

        final TableColumn<Account, BigDecimal> budgetedColumn = new TableColumn<>(resources.getString("Column.Budgeted"));
        budgetedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getBudgeted());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<Account, BigDecimal> actualColumn = new TableColumn<>(resources.getString("Column.Budgeted"));
        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        headerColumn.getColumns().add(actualColumn);

        final TableColumn<Account, BigDecimal> remainingColumn = new TableColumn<>(resources.getString("Column.Remaining"));
        remainingColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getRemaining());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        headerColumn.getColumns().add(remainingColumn);

        accountSummaryTable.getColumns().add(headerColumn);
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
}
