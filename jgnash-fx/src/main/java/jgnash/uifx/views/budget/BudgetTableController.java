package jgnash.uifx.views.budget;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.engine.budget.BudgetPeriodResults;
import jgnash.engine.budget.BudgetResultsModel;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.uifx.control.NullTableViewSelectionModel;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.NotNull;

/**
 * Controller for budget tables.
 *
 * @author Craig Cavanaugh
 */
public class BudgetTableController implements MessageListener {

    private static final String HIDE_HORIZONTAL_CSS = "jgnash/skin/tableHideHorizontalScrollBar.css";
    private static final String HIDE_VERTICAL_CSS = "jgnash/skin/tableHideVerticalScrollBar.css";
    private static final String HIDE_HEADER_CSS = "jgnash/skin/tableHideColumnHeader.css";

    private static final int ROW_HEIGHT_MULTIPLIER = 2;

    //TODO: Magic number that needs to be fixed or controlled with css
    private static final double BORDER_MARGIN = 2;

    // allow a selection span of +/- the specified number of years
    private static final int YEAR_MARGIN = 15;

    // Initial column width
    private static final double INITIAL_WIDTH = 75;

    @FXML
    private CheckBox runningTotalsButton;

    @FXML
    private HBox sparkLinePane;

    @FXML
    private ScrollBar horizontalScrollBar;

    @FXML
    private GridPane gridPane;

    @FXML
    private Spinner<Integer> yearSpinner;

    @FXML
    private ScrollBar verticalScrollBar;

    @FXML
    private TreeTableView<Account> accountTreeView;

    @FXML
    private TableView<Account> periodTable;

    @FXML
    private TableView<Account> accountSummaryTable;

    @FXML
    private TableView<AccountGroup> periodSummaryTable;

    @FXML
    private TableView<AccountGroup> accountGroupPeriodSummaryTable;

    @FXML
    private TableView<AccountGroup> accountTypeTable;

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<Budget> budgetProperty = new SimpleObjectProperty<>();

    private BudgetResultsModel budgetResultsModel;

    /**
     * This list is updated to track the expanded rows of the TreeTableView.
     * This should be the model for all account specific tables
     */
    private final ObservableList<Account> expandedAccountList = FXCollections.observableArrayList();

    /**
     * This list is updated to track the displayed AccountGroups.
     * This should be the model for all account specific tables
     */
    private final ObservableList<AccountGroup> accountGroupList = FXCollections.observableArrayList();

    private final DoubleProperty rowHeightProperty = new SimpleDoubleProperty();

    /**
     * Bind the max and minimum values of every column to this width.
     */
    private final DoubleProperty columnWidthProperty = new SimpleDoubleProperty(INITIAL_WIDTH);

    /**
     * Bind the max and minimum values of every column to this width.
     */
    private final DoubleProperty remainingColumnWidthProperty = new SimpleDoubleProperty(INITIAL_WIDTH);

    /**
     * The is the minimum column width required to display the largest numeric value. Value is cached and only
     * updated with budget or transaction changes
     */
    private double minColumnWidth = INITIAL_WIDTH;

    /**
     * The is the minimum column width required to display the largest numeric summary value.
     */
    private final DoubleProperty minSummaryColumnWidthProperty = new SimpleDoubleProperty(INITIAL_WIDTH);

    /**
     * Current index to be used for scrolling the display.  If 0 the first period is displayed to the left
     */
    private int index;

    /**
     * The number of visible columns.
     */
    private int visibleColumnCount = 1;

    /**
     * The number of periods in the model.
     */
    private final IntegerProperty periodCountProperty = new SimpleIntegerProperty(1);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Listens to changes to the width of the period table and optimizes the column widths.
     */
    private ChangeListener<Number> tableWidthChangeListener;

    /**
     * Rate limiting executor.
     */
    private ScheduledThreadPoolExecutor rateLimitExecutor;

    private static final int UPDATE_PERIOD = 350; // update period in milliseconds

    @FXML
    private void initialize() {
        rateLimitExecutor = new ScheduledThreadPoolExecutor(1, new DefaultDaemonThreadFactory(),
                new ThreadPoolExecutor.DiscardPolicy());


        tableWidthChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                optimizeColumnWidths();
            }
        };

        updateHeights();

        yearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear() - YEAR_MARGIN, LocalDate.now().getYear() + YEAR_MARGIN,
                LocalDate.now().getYear(), 1));

        accountTreeView.getStylesheets().addAll(HIDE_VERTICAL_CSS);
        accountTreeView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        accountTreeView.setShowRoot(false);
        accountTreeView.setEditable(true);
        accountTreeView.fixedCellSizeProperty().bind(rowHeightProperty);

        accountSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountSummaryTable.getStylesheets().addAll(HIDE_VERTICAL_CSS, HIDE_HORIZONTAL_CSS);
        accountSummaryTable.setItems(expandedAccountList);
        accountSummaryTable.fixedCellSizeProperty().bind(rowHeightProperty);
        accountSummaryTable.setSelectionModel(new NullTableViewSelectionModel<>(accountSummaryTable));

        accountTypeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountTypeTable.getStylesheets().add(HIDE_HEADER_CSS);
        accountTypeTable.setItems(accountGroupList);
        accountTypeTable.fixedCellSizeProperty().bind(rowHeightProperty);
        accountTypeTable.prefHeightProperty()
                .bind(rowHeightProperty.multiply(Bindings.size(accountGroupList)).add(BORDER_MARGIN));
        accountTypeTable.setSelectionModel(new NullTableViewSelectionModel<>(accountTypeTable));

        accountGroupPeriodSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountGroupPeriodSummaryTable.getStylesheets().addAll(HIDE_HEADER_CSS, HIDE_HORIZONTAL_CSS, HIDE_VERTICAL_CSS);
        accountGroupPeriodSummaryTable.setItems(accountGroupList);
        accountGroupPeriodSummaryTable.fixedCellSizeProperty().bind(rowHeightProperty);
        accountGroupPeriodSummaryTable.prefHeightProperty()
                .bind(rowHeightProperty.multiply(Bindings.size(accountGroupList)).add(BORDER_MARGIN));
        accountGroupPeriodSummaryTable.setSelectionModel(new NullTableViewSelectionModel<>(accountGroupPeriodSummaryTable));

        buildAccountTreeTable();
        buildAccountTypeTable();
        buildAccountSummaryTable();
        buildAccountGroupSummaryTable();

        accountSummaryTable.maxWidthProperty().bind(minSummaryColumnWidthProperty.multiply(3.0).add(BORDER_MARGIN));
        accountGroupPeriodSummaryTable.maxWidthProperty().bind(minSummaryColumnWidthProperty.multiply(3.0).add(BORDER_MARGIN));

        accountSummaryTable.minWidthProperty().bind(minSummaryColumnWidthProperty.multiply(3.0).add(BORDER_MARGIN));
        accountGroupPeriodSummaryTable.minWidthProperty().bind(minSummaryColumnWidthProperty.multiply(3.0).add(BORDER_MARGIN));

        accountTreeView.expandedItemCountProperty().addListener((observable, oldValue, newValue)
                -> Platform.runLater(this::updateExpandedAccountList));

        final ChangeListener<Object> budgetChangeListener = (observable, oldValue, newValue) -> {
            // push change to end of the application thread
            Platform.runLater(BudgetTableController.this::handleBudgetChange);
        };

        budgetProperty.addListener(budgetChangeListener);
        yearSpinner.valueProperty().addListener(budgetChangeListener);

        runningTotalsButton.selectedProperty().addListener((observable, oldValue, newValue) -> {

            /* Setting the tables as un-managed effectively removes these tables from the GridPane.  The tables are
               redundant if showing the amounts as running balances. */
            accountSummaryTable.setManaged(!newValue);
            accountGroupPeriodSummaryTable.setManaged(!newValue);

            Platform.runLater(BudgetTableController.this::handleBudgetChange);
        });

        horizontalScrollBar.setMin(0);
        horizontalScrollBar.maxProperty().bind(periodCountProperty.subtract(visibleColumnCount));
        horizontalScrollBar.setUnitIncrement(1);
        horizontalScrollBar.disableProperty().bind(periodCountProperty.lessThanOrEqualTo(1));

        //System.out.println()

        // shift the table right and left with the ScrollBar value
        horizontalScrollBar.valueProperty().addListener((observable, oldValue, newValue)
                -> Platform.runLater(new Runnable() {   // push update to the end of the platform thread to stability
            @Override
            public void run() {
                // must be synchronized to prevent a race condition from multiple events and an out of bounds exception
                synchronized (this) {

                    //System.out.println(newValue + ", " + horizontalScrollBar.maxProperty().get());   // TODO: bad value


                    final int newIndex = (int) Math.round(newValue.doubleValue());

                    if (newIndex > index) {
                        while (newIndex > index) {
                            handleShiftRight();
                        }
                    } else if (newIndex < index) {
                        while (newIndex < index) {
                            handleShiftLeft();
                        }
                    }
                }
            }
        }));

        ThemeManager.fontScaleProperty().addListener((observable, oldValue, newValue) -> updateHeights());
    }

    private void rateLimitUpdate(final Runnable runnable) {
        rateLimitExecutor.schedule(() -> {
            if (rateLimitExecutor.getQueue().size() < 1) {   // ignore if we already have one waiting in the queue
                Platform.runLater(runnable);    // update is assumed to be on the platform thread
            }
        }, UPDATE_PERIOD, TimeUnit.MILLISECONDS);
    }

    @FXML
    private void handleShiftLeft() {
        lock.writeLock().lock();

        try {
            // remove the right column
            periodTable.getColumns().remove(visibleColumnCount - 1);
            periodSummaryTable.getColumns().remove(visibleColumnCount - 1);

            index--;

            // insert a new column to the left
            periodTable.getColumns().add(0, buildAccountPeriodResultsColumn(index));
            periodSummaryTable.getColumns().add(0, buildAccountPeriodSummaryColumn(index));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @FXML
    private void handleShiftRight() {
        lock.writeLock().lock();

        try {
            // remove leftmost column
            periodTable.getColumns().remove(0);
            periodSummaryTable.getColumns().remove(0);

            //System.out.println(index);

            int newColumn = index + visibleColumnCount;

            newColumn = Math.min(newColumn, budgetResultsModel.getDescriptorList().size() - 1);

            // add a new column to the right
            periodTable.getColumns().add(buildAccountPeriodResultsColumn(newColumn));
            periodSummaryTable.getColumns().add(buildAccountPeriodSummaryColumn(newColumn));

            index++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    BudgetResultsModel getBudgetResultsModel() {
        return budgetResultsModel;
    }

    SimpleObjectProperty<Budget> budgetProperty() {
        return budgetProperty;
    }

    private void updateHeights() {
        rowHeightProperty.setValue(ThemeManager.getBaseTextHeight() * ROW_HEIGHT_MULTIPLIER);
    }

    /**
     * The table view will lazily create the ScrollBars which makes finding them tricky.  We need to check for
     * their existence and try again later if they do not exist.
     * <p>
     * Synchronize binding, otherwise the ScrollBars get a bit confused and do not respond to a scroll wheel
     */
    private synchronized void bindScrollBars() {
        final Optional<ScrollBar> accountScrollBar = JavaFXUtils.findVerticalScrollBar(accountTreeView);
        final Optional<ScrollBar> vDataScrollBar = JavaFXUtils.findVerticalScrollBar(periodTable);
        final Optional<ScrollBar> accountSumScrollBar = JavaFXUtils.findVerticalScrollBar(accountSummaryTable);

        if (!vDataScrollBar.isPresent() || !accountScrollBar.isPresent() || !accountSumScrollBar.isPresent()) {
            Platform.runLater(BudgetTableController.this::bindScrollBars);  //re-spawn on the application thread
        } else {    // all here, lets bind then now
            verticalScrollBar.minProperty().bindBidirectional(accountScrollBar.get().minProperty());
            verticalScrollBar.maxProperty().bindBidirectional(accountScrollBar.get().maxProperty());
            verticalScrollBar.valueProperty().bindBidirectional(accountScrollBar.get().valueProperty());

            accountScrollBar.get().valueProperty().bindBidirectional(vDataScrollBar.get().valueProperty());
            accountSumScrollBar.get().valueProperty().bindBidirectional(vDataScrollBar.get().valueProperty());
        }
    }

    /**
     * Model must be rebuilt if the year or a budget property is changed.
     * <p>
     * This method is synchronized to limit more than one update attempt at a time.
     */
    private synchronized void handleBudgetChange() {
        lock.writeLock().lock();

        try {

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (budgetProperty.get() != null && engine != null) {
                // unregister from the old model
                if (budgetResultsModel != null) {
                    budgetResultsModel.removeMessageListener(this); // unregister from the old model
                }

                budgetResultsModel = new BudgetResultsModel(budgetProperty.get(), yearSpinner.getValue(),
                        engine.getDefaultCurrency(), runningTotalsButton.isSelected());


                // model has changed, calculate the minimum column width for the summary columns
                minSummaryColumnWidthProperty.setValue(calculateMinSummaryWidthColumnWidth());

                // model has changed, calculate the minimum column width
                minColumnWidth = calculateMinPeriodColumnWidth();

                // register with the new model
                budgetResultsModel.addMessageListener(this);    // register with the new model

                periodCountProperty.setValue(budgetResultsModel.getDescriptorList().size());
                loadModel();
            } else {
                accountTreeView.setRoot(null);
                expandedAccountList.clear();
                accountGroupList.clear();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Maintains the list of expanded accounts.
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
        lock.readLock().lock();

        try {
            loadAccountTree();

            accountGroupList.setAll(budgetResultsModel.getAccountGroupList());

            optimizeColumnWidths();

            buildPeriodTable();
            buildPeriodSummaryTable();
            updateExpandedAccountList();

            updateSparkLines();

            Platform.runLater(this::bindScrollBars);

            Platform.runLater(this::focusCurrentPeriod);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void optimizeColumnWidths() {
        lock.writeLock().lock();

        try {
            final double availWidth = periodTable.getWidth() - BORDER_MARGIN;   // width of the table

            // calculate the number of visible columns, period columns are 3 columns wide
            final int maxVisible = (int) Math.floor(availWidth / (minColumnWidth * 3.0));

            // update the number of visible columns factoring in the size of the descriptor list
            visibleColumnCount = (Math.min(budgetResultsModel.getDescriptorList().size(), maxVisible));

            final double width = Math.floor(availWidth /
                    Math.min(budgetResultsModel.getDescriptorList().size() * 3, maxVisible * 3));

            columnWidthProperty.setValue(width);

            double remainder = availWidth - (maxVisible * 3.0 * width);

            remainingColumnWidthProperty.setValue(Math.floor(width + (remainder / 3.0)));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void focusCurrentPeriod() {
        final LocalDate now = LocalDate.now();

        final List<BudgetPeriodDescriptor> budgetPeriodDescriptorList = budgetResultsModel.getDescriptorList();

        for (int i = 0; i < budgetPeriodDescriptorList.size(); i++) {
            final BudgetPeriodDescriptor budgetPeriodDescriptor = budgetPeriodDescriptorList.get(i);

            if (budgetPeriodDescriptor.isBetween(now)) {
                final int index = Math.max(Math.min(i, periodCountProperty.subtract(visibleColumnCount).intValue()), 0);

                Platform.runLater(() -> horizontalScrollBar.setValue(index));
                break;
            }
        }
    }

    private void loadAccountTree() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final TreeItem<Account> root = new TreeItem<>(engine.getRootAccount());
        root.setExpanded(true);

        accountTreeView.setRoot(root);
        loadChildren(root);
    }

    private synchronized void loadChildren(final TreeItem<Account> parentItem) {
        final Account parent = parentItem.getValue();

        parent.getChildren(Comparators.getAccountByCode()).stream().filter(budgetResultsModel::includeAccount)
                .forEach(child -> {
                    final TreeItem<Account> childItem = new TreeItem<>(child);
                    childItem.setExpanded(true);
                    parentItem.getChildren().add(childItem);

                    if (child.getChildCount() > 0) {
                        loadChildren(childItem);
                    }
                });
    }

    /**
     * Constructs the tree table.
     *
     * @see Stage#showAndWait() for need to push {@code handleEditAccountGoals(account)} to the platform thread
     */
    private void buildAccountTreeTable() {
        // empty column header is needed to match other table columns
        final TreeTableColumn<Account, String> headerColumn = new TreeTableColumn<>("");

        final TreeTableColumn<Account, Account> nameColumn
                = new TreeTableColumn<>(resources.getString("Column.Account"));

        nameColumn.setCellFactory(param -> new AccountTreeTableCell());
        nameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));

        nameColumn.setOnEditStart(event -> {
            final Account account = event.getRowValue().getValue();

            if (account != null && !account.isPlaceHolder()) {

                // push to the edit of the platform thread to avoid an IllegalStateException
                // "showAndWait is not allowed during animation or layout processing" exception
                // this may be reach outside the platform thread if a uses is click happy
                if (Platform.isFxApplicationThread()) {
                    handleEditAccountGoals(account);
                } else {
                    Platform.runLater(() -> handleEditAccountGoals(account));
                }
            }
        });
        nameColumn.setEditable(true);

        headerColumn.getColumns().add(nameColumn);

        accountTreeView.getColumns().add(headerColumn);

        headerColumn.minWidthProperty().bind(accountTreeView.widthProperty());
        headerColumn.maxWidthProperty().bind(accountTreeView.widthProperty());
    }

    private void buildAccountTypeTable() {
        final TableColumn<AccountGroup, String> nameColumn = new TableColumn<>(resources.getString("Column.Type"));
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().toString()));

        accountTypeTable.getColumns().add(nameColumn);
    }

    private void buildAccountSummaryTable() {
        final TableColumn<Account, BigDecimal> headerColumn = new TableColumn<>(resources.getString("Title.Summary"));

        final TableColumn<Account, BigDecimal> budgetedColumn
                = new TableColumn<>(resources.getString("Column.Budgeted"));

        budgetedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getBudgeted());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        budgetedColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        budgetedColumn.minWidthProperty().bind(minSummaryColumnWidthProperty);
        budgetedColumn.maxWidthProperty().bind(minSummaryColumnWidthProperty);
        budgetedColumn.setSortable(false);
        budgetedColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<Account, BigDecimal> actualColumn = new TableColumn<>(resources.getString("Column.Actual"));
        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        actualColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        actualColumn.minWidthProperty().bind(minSummaryColumnWidthProperty);
        actualColumn.maxWidthProperty().bind(minSummaryColumnWidthProperty);
        actualColumn.setSortable(false);
        actualColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(actualColumn);

        final TableColumn<Account, BigDecimal> remainingColumn
                = new TableColumn<>(resources.getString("Column.Remaining"));

        remainingColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getRemaining());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        remainingColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        remainingColumn.minWidthProperty().bind(minSummaryColumnWidthProperty);
        remainingColumn.maxWidthProperty().bind(minSummaryColumnWidthProperty);
        remainingColumn.setSortable(false);
        remainingColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(remainingColumn);
        headerColumn.resizableProperty().setValue(false);

        accountSummaryTable.getColumns().add(headerColumn);
    }

    private TableColumn<Account, BigDecimal> buildAccountPeriodResultsColumn(final int index) {

        final BudgetPeriodDescriptor descriptor = budgetResultsModel.getDescriptorList().get(index);

        final TableColumn<Account, BigDecimal> headerColumn = new TableColumn<>(descriptor.getPeriodDescription());

        final TableColumn<Account, BigDecimal> budgetedColumn
                = new TableColumn<>(resources.getString("Column.Budgeted"));

        budgetedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getBudgeted());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        budgetedColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        budgetedColumn.minWidthProperty().bind(columnWidthProperty);
        budgetedColumn.maxWidthProperty().bind(columnWidthProperty);
        budgetedColumn.setSortable(false);
        budgetedColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<Account, BigDecimal> actualColumn = new TableColumn<>(resources.getString("Column.Actual"));
        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        actualColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        actualColumn.minWidthProperty().bind(columnWidthProperty);
        actualColumn.maxWidthProperty().bind(columnWidthProperty);
        actualColumn.setSortable(false);
        actualColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(actualColumn);

        final TableColumn<Account, BigDecimal> remainingColumn
                = new TableColumn<>(resources.getString("Column.Remaining"));
        remainingColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getRemaining());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        remainingColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());

        // the max width is not bound to allow last column to grow and fill any voids
        remainingColumn.minWidthProperty().bind(remainingColumnWidthProperty);
        remainingColumn.maxWidthProperty().bind(remainingColumnWidthProperty);
        remainingColumn.setSortable(false);
        remainingColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(remainingColumn);

        headerColumn.resizableProperty().setValue(false);

        return headerColumn;
    }

    /**
     * The period table must be rebuilt because of JavaFx issues.
     */
    private void buildPeriodTable() {
        // remove the old listener so it does not leak
        periodTable.widthProperty().removeListener(tableWidthChangeListener);

        // recreate the table and load the new one into the grid pane
        final int row = GridPane.getRowIndex(periodTable);
        final int column = GridPane.getColumnIndex(periodTable);
        gridPane.getChildren().remove(periodTable);

        periodTable = new TableView<>();
        GridPane.setConstraints(periodTable, column, row);
        gridPane.getChildren().add(periodTable);

        periodTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        periodTable.getStylesheets().addAll(HIDE_HORIZONTAL_CSS, HIDE_VERTICAL_CSS);
        periodTable.fixedCellSizeProperty().bind(rowHeightProperty);
        periodTable.setSelectionModel(new NullTableViewSelectionModel<>(periodTable));

        // index exceeds allowed value because the user reduced the period count, reset to the maximum allowed value
        if (index > budgetResultsModel.getDescriptorList().size() - visibleColumnCount) {
            index = budgetResultsModel.getDescriptorList().size() - visibleColumnCount;
        }

        final int periodCount = Math.min(visibleColumnCount, budgetResultsModel.getDescriptorList().size());

        for (int i = index; i < index + periodCount; i++) {
            periodTable.getColumns().add(buildAccountPeriodResultsColumn(i));
        }

        periodTable.setItems(expandedAccountList);
        periodTable.widthProperty().addListener(tableWidthChangeListener);
    }

    private TableColumn<AccountGroup, BigDecimal> buildAccountPeriodSummaryColumn(final int index) {
        final BudgetPeriodDescriptor descriptor = budgetResultsModel.getDescriptorList().get(index);

        final TableColumn<AccountGroup, BigDecimal> headerColumn = new TableColumn<>(descriptor.getPeriodDescription());

        final TableColumn<AccountGroup, BigDecimal> budgetedColumn
                = new TableColumn<>(resources.getString("Column.Budgeted"));
        budgetedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getBudgeted());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        budgetedColumn.setCellFactory(param -> new AccountGroupTableCell());
        budgetedColumn.minWidthProperty().bind(columnWidthProperty);
        budgetedColumn.maxWidthProperty().bind(columnWidthProperty);
        budgetedColumn.setSortable(false);
        budgetedColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<AccountGroup, BigDecimal> actualColumn
                = new TableColumn<>(resources.getString("Column.Actual"));

        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor, param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        actualColumn.setCellFactory(param -> new AccountGroupTableCell());
        actualColumn.minWidthProperty().bind(columnWidthProperty);
        actualColumn.maxWidthProperty().bind(columnWidthProperty);
        actualColumn.setSortable(false);
        actualColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(actualColumn);

        final TableColumn<AccountGroup, BigDecimal> remainingColumn
                = new TableColumn<>(resources.getString("Column.Remaining"));

        remainingColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getRemaining());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        remainingColumn.setCellFactory(param -> new AccountGroupTableCell());

        // the max width is not bound to allow last column to grow and fill any voids
        remainingColumn.minWidthProperty().bind(remainingColumnWidthProperty);
        remainingColumn.maxWidthProperty().bind(remainingColumnWidthProperty);
        remainingColumn.setSortable(false);
        remainingColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(remainingColumn);

        return headerColumn;
    }

    /**
     * The period summary table must be rebuilt because of JavaFx issues.
     */
    private void buildPeriodSummaryTable() {
        // recreate the table and load the new one into the grid pane
        final int row = GridPane.getRowIndex(periodSummaryTable);
        final int column = GridPane.getColumnIndex(periodSummaryTable);
        gridPane.getChildren().remove(periodSummaryTable);
        periodSummaryTable = new TableView<>();
        GridPane.setConstraints(periodSummaryTable, column, row);
        gridPane.getChildren().add(periodSummaryTable);

        periodSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        periodSummaryTable.getStylesheets().addAll(HIDE_HORIZONTAL_CSS, HIDE_VERTICAL_CSS, HIDE_HEADER_CSS);
        periodSummaryTable.fixedCellSizeProperty().bind(rowHeightProperty);
        periodSummaryTable.prefHeightProperty()
                .bind(rowHeightProperty.multiply(Bindings.size(accountGroupList)).add(BORDER_MARGIN));
        periodSummaryTable.setSelectionModel(new NullTableViewSelectionModel<>(periodSummaryTable));

        final int periodCount = Math.min(visibleColumnCount, budgetResultsModel.getDescriptorList().size());

        for (int i = index; i < index + periodCount; i++) {
            periodSummaryTable.getColumns().add(buildAccountPeriodSummaryColumn(i));
        }

        periodSummaryTable.setItems(accountGroupList);
    }

    private void buildAccountGroupSummaryTable() {
        final TableColumn<AccountGroup, BigDecimal> headerColumn
                = new TableColumn<>(resources.getString("Title.Summary"));

        final TableColumn<AccountGroup, BigDecimal> budgetedColumn
                = new TableColumn<>(resources.getString("Column.Budgeted"));

        budgetedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getBudgeted());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        budgetedColumn.setCellFactory(param -> new AccountGroupTableCell());
        budgetedColumn.minWidthProperty().bind(minSummaryColumnWidthProperty);
        budgetedColumn.maxWidthProperty().bind(minSummaryColumnWidthProperty);
        budgetedColumn.setSortable(false);
        budgetedColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<AccountGroup, BigDecimal> actualColumn
                = new TableColumn<>(resources.getString("Column.Actual"));

        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        actualColumn.setCellFactory(param -> new AccountGroupTableCell());
        actualColumn.minWidthProperty().bind(minSummaryColumnWidthProperty);
        actualColumn.maxWidthProperty().bind(minSummaryColumnWidthProperty);
        actualColumn.setSortable(false);
        actualColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(actualColumn);

        final TableColumn<AccountGroup, BigDecimal> remainingColumn
                = new TableColumn<>(resources.getString("Column.Remaining"));

        remainingColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getRemaining());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        remainingColumn.setCellFactory(param -> new AccountGroupTableCell());
        remainingColumn.minWidthProperty().bind(minSummaryColumnWidthProperty);
        remainingColumn.maxWidthProperty().bind(minSummaryColumnWidthProperty);
        remainingColumn.setSortable(false);
        remainingColumn.resizableProperty().setValue(false);

        headerColumn.getColumns().add(remainingColumn);

        accountGroupPeriodSummaryTable.getColumns().add(headerColumn);
    }

    private double calculateMinColumnWidth(final BudgetPeriodDescriptor descriptor, final Account account) {
        double max = 0;
        double min = 0;

        BudgetPeriodResults budgetPeriodResults = budgetResultsModel.getResults(descriptor, account);

        max = Math.max(max, budgetPeriodResults.getBudgeted().doubleValue());
        max = Math.max(max, budgetPeriodResults.getChange().doubleValue());
        max = Math.max(max, budgetPeriodResults.getRemaining().doubleValue());

        min = Math.min(min, budgetPeriodResults.getBudgeted().doubleValue());
        min = Math.min(min, budgetPeriodResults.getChange().doubleValue());
        min = Math.min(min, budgetPeriodResults.getRemaining().doubleValue());

        return Math.max(JavaFXUtils.getDisplayedTextWidth(
                CommodityFormat.getFullNumberFormat(budgetResultsModel.getBaseCurrency()).format(max) +
                        BORDER_MARGIN, null), JavaFXUtils.getDisplayedTextWidth(
                CommodityFormat.getFullNumberFormat(budgetResultsModel.getBaseCurrency()).format(min) +
                        BORDER_MARGIN, null));
    }

    private double calculateMinColumnWidth(final Account account) {
        double max = 0;
        double min = 0;

        BudgetPeriodResults budgetPeriodResults = budgetResultsModel.getResults(account);
        max = Math.max(max, budgetPeriodResults.getBudgeted().doubleValue());
        max = Math.max(max, budgetPeriodResults.getChange().doubleValue());
        max = Math.max(max, budgetPeriodResults.getRemaining().doubleValue());

        min = Math.min(min, budgetPeriodResults.getBudgeted().doubleValue());
        min = Math.min(min, budgetPeriodResults.getChange().doubleValue());
        min = Math.min(min, budgetPeriodResults.getRemaining().doubleValue());


        return Math.max(JavaFXUtils.getDisplayedTextWidth(
                CommodityFormat.getFullNumberFormat(budgetResultsModel.getBaseCurrency()).format(max) +
                        BORDER_MARGIN, null), JavaFXUtils.getDisplayedTextWidth(
                CommodityFormat.getFullNumberFormat(budgetResultsModel.getBaseCurrency()).format(min) +
                        BORDER_MARGIN, null));
    }

    private double calculateMinColumnWidth(final BudgetPeriodDescriptor descriptor) {
        double max = 0;
        double min = 0;

        for (final AccountGroup accountGroup : accountGroupList) {
            BudgetPeriodResults budgetPeriodResults = budgetResultsModel.getResults(descriptor, accountGroup);
            max = Math.max(max, budgetPeriodResults.getBudgeted().doubleValue());
            max = Math.max(max, budgetPeriodResults.getChange().doubleValue());
            max = Math.max(max, budgetPeriodResults.getRemaining().doubleValue());

            min = Math.min(min, budgetPeriodResults.getBudgeted().doubleValue());
            min = Math.min(min, budgetPeriodResults.getChange().doubleValue());
            min = Math.min(min, budgetPeriodResults.getRemaining().doubleValue());
        }

        return Math.max(JavaFXUtils.getDisplayedTextWidth(
                CommodityFormat.getFullNumberFormat(budgetResultsModel.getBaseCurrency()).format(max) +
                        BORDER_MARGIN, null),
                JavaFXUtils.getDisplayedTextWidth(
                        CommodityFormat.getFullNumberFormat(budgetResultsModel.getBaseCurrency()).format(min) +
                                BORDER_MARGIN, null));
    }

    private double calculateMinPeriodColumnWidth() {
        double max = 0;

        for (final BudgetPeriodDescriptor descriptor : budgetResultsModel.getDescriptorList()) {
            for (final Account account: expandedAccountList) {
                max = Math.max(max, calculateMinColumnWidth(descriptor, account));
            }
        }

        max = Math.max(max, JavaFXUtils.getDisplayedTextWidth(resources.getString("Column.Budgeted")
                + BORDER_MARGIN, null));
        max = Math.max(max, JavaFXUtils.getDisplayedTextWidth(resources.getString("Column.Actual")
                + BORDER_MARGIN, null));
        max = Math.max(max, JavaFXUtils.getDisplayedTextWidth(resources.getString("Column.Remaining")
                + BORDER_MARGIN, null));

        return Math.ceil(max);
    }

    private double calculateMinSummaryWidthColumnWidth() {
        double max = 0;

        for (final BudgetPeriodDescriptor descriptor : budgetResultsModel.getDescriptorList()) {
            max = Math.max(max, calculateMinColumnWidth(descriptor));
        }

        for (final Account account : expandedAccountList) {
            max = Math.max(max, calculateMinColumnWidth(account));
        }

        max = Math.max(max, JavaFXUtils.getDisplayedTextWidth(resources.getString("Column.Budgeted")
                + BORDER_MARGIN, null));
        max = Math.max(max, JavaFXUtils.getDisplayedTextWidth(resources.getString("Column.Actual")
                + BORDER_MARGIN, null));
        max = Math.max(max, JavaFXUtils.getDisplayedTextWidth(resources.getString("Column.Remaining")
                + BORDER_MARGIN, null));

        return Math.ceil(max);
    }

    private void handleBudgetUpdate() {
        rateLimitUpdate(BudgetTableController.this::handleBudgetChange);
    }

    private void handleTransactionUpdate() {
        rateLimitUpdate(() -> {
            synchronized (this) {
                periodTable.refresh();
                periodSummaryTable.refresh();
            }
            accountSummaryTable.refresh();
            accountGroupPeriodSummaryTable.refresh();


            optimizeColumnWidths();

            updateSparkLines();
        });
    }

    private void updateSparkLines() {
        sparkLinePane.getChildren().clear();

        for (AccountGroup group : accountGroupList) {
            List<BigDecimal> remaining = budgetResultsModel.getDescriptorList().parallelStream().map(descriptor ->
                    budgetResultsModel.getResults(descriptor, group).getRemaining()).collect(Collectors.toList());

            final HBox hBox = new HBox(new Label(group.toString()), new BudgetSparkLine(remaining));
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.getStylesheets().addAll(MainView.DEFAULT_CSS);

            sparkLinePane.getChildren().add(hBox);
        }
    }

    private void handleEditAccountGoals(@NotNull final Account account) {
        Objects.requireNonNull(account);

        final FXMLUtils.Pair<BudgetGoalsDialogController> pair =
                FXMLUtils.load(BudgetGoalsDialogController.class.getResource("BudgetGoalsDialog.fxml"),
                        resources.getString("Title.BudgetManager") + " - " + account.getName());

        pair.getController().accountProperty().setValue(account);
        pair.getController().workingYearProperty().setValue(yearSpinner.getValue());

        try {
            final BudgetGoal oldGoal = (BudgetGoal) budgetProperty().get().getBudgetGoal(account).clone();
            pair.getController().budgetGoalProperty().setValue(oldGoal);
        } catch (final CloneNotSupportedException e) {
            Logger.getLogger(BudgetTableController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        pair.getStage().showAndWait();

        final Optional<BudgetGoal> result = pair.getController().getResult();

        if (result.isPresent()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.updateBudgetGoals(budgetProperty.get(), account, result.get());
        }
    }

    @Override
    public void messagePosted(final Message message) {
        switch (message.getEvent()) {
            case FILE_CLOSING:
                budgetResultsModel.removeMessageListener(this);
                budgetProperty().setValue(null);
                break;
            case BUDGET_REMOVE:
                if (budgetProperty().get().equals(message.getObject(MessageProperty.BUDGET))) {
                    budgetProperty().setValue(null);
                    budgetResultsModel.removeMessageListener(this);
                }
                break;
            case ACCOUNT_ADD:
            case ACCOUNT_MODIFY:
            case ACCOUNT_REMOVE:
            case BUDGET_UPDATE:
            case BUDGET_GOAL_UPDATE:
                if (budgetProperty().get().equals(message.getObject(MessageProperty.BUDGET))) {
                    handleBudgetUpdate();
                }
                break;
            case TRANSACTION_ADD:
            case TRANSACTION_REMOVE:
                handleTransactionUpdate();
                break;
            default:
                break;
        }
    }

    private class AccountCommodityFormatTableCell extends TableCell<Account, BigDecimal> {

        AccountCommodityFormatTableCell() {
            setStyle("-fx-alignment: center-right;");  // Right align
        }

        @Override
        protected void updateItem(final BigDecimal amount, final boolean empty) {
            super.updateItem(amount, empty);  // required

            if (!empty && amount != null && getTableRow() != null) {
                final Account account = expandedAccountList.get(getTableRow().getIndex());
                final NumberFormat format = CommodityFormat.getFullNumberFormat(account.getCurrencyNode());

                setText(format.format(amount));

                if (amount.signum() < 0) {
                    setId(StyleClass.NORMAL_NEGATIVE_CELL_ID);
                } else {
                    setId(StyleClass.NORMAL_CELL_ID);
                }
            } else {
                setText(null);
            }
        }
    }

    private class AccountGroupTableCell extends TableCell<AccountGroup, BigDecimal> {

        private final NumberFormat format;

        AccountGroupTableCell() {
            setStyle("-fx-alignment: center-right;");  // Right align
            format = CommodityFormat.getFullNumberFormat(budgetResultsModel.getBaseCurrency());
        }

        @Override
        protected void updateItem(final BigDecimal amount, final boolean empty) {
            super.updateItem(amount, empty);  // required

            if (!empty && amount != null && getTableRow() != null) {
                setText(format.format(amount));

                if (amount.signum() < 0) {
                    setId(StyleClass.NORMAL_NEGATIVE_CELL_ID);
                } else {
                    setId(StyleClass.NORMAL_CELL_ID);
                }
            } else {
                setText(null);
            }
        }
    }

    private static class AccountTreeTableCell extends TreeTableCell<Account, Account> {

        @Override
        protected void updateItem(final Account account, final boolean empty) {
            super.updateItem(account, empty);  // required

            if (!empty && account != null && getTreeTableRow() != null) {
                setText(account.getName());

                if (account.isPlaceHolder()) {
                    setId(StyleClass.DISABLED_CELL_ID);
                } else {
                    setId(StyleClass.NORMAL_CELL_ID);
                }
            } else {
                setText(null);
            }
        }
    }
}
