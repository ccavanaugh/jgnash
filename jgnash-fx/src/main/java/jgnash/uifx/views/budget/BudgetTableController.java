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
import java.util.prefs.Preferences;
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
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
import javafx.scene.input.MouseEvent;
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
import jgnash.text.NumericFormats;
import jgnash.time.Period;
import jgnash.uifx.control.NullTableViewSelectionModel;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.NotNull;

import static jgnash.uifx.skin.StyleClass.BOLD_LABEL_ID;
import static jgnash.uifx.skin.StyleClass.BOLD_NEGATIVE_LABEL_ID;
import static jgnash.uifx.skin.StyleClass.NORMAL_CELL_ID;
import static jgnash.uifx.skin.StyleClass.NORMAL_NEGATIVE_CELL_ID;
import static jgnash.uifx.skin.StyleClass.TODAY_BOLD_LABEL_ID;
import static jgnash.uifx.skin.StyleClass.TODAY_BOLD_NEGATIVE_LABEL_ID;
import static jgnash.uifx.skin.StyleClass.TODAY_NORMAL_CELL_ID;
import static jgnash.uifx.skin.StyleClass.TODAY_NORMAL_NEGATIVE_CELL_ID;

/**
 * Controller for budget tables.
 *
 * @author Craig Cavanaugh
 */
public class BudgetTableController implements MessageListener {

    private static final String RUNNING_TOTALS = "runningTotals";

    private static final String ACCOUNT_COLUMN_WIDTH = "accountColumnWidth";

    private static final int ROW_HEIGHT_MULTIPLIER = 2;

    //TODO: Magic number that needs to be fixed or controlled with css
    private static final double BORDER_MARGIN = 2;

    /**
     * Limits the selection span of +/- the specified number of years
     */
    private static final int YEAR_MARGIN = 15;

    // Initial column width
    private static final double INITIAL_WIDTH = 55;

    private static final String NOW = "now";

    private final Preferences preferences = Preferences.userNodeForPackage(BudgetTableController.class);

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

    private final SimpleObjectProperty<Budget> budget = new SimpleObjectProperty<>();

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

    private final DoubleProperty rowHeight = new SimpleDoubleProperty();

    /**
     * Bind the max and minimum values of every column to this width.
     */
    private final DoubleProperty columnWidth = new SimpleDoubleProperty(INITIAL_WIDTH);

    /**
     * Bind the max and minimum values of every column to this width.
     */
    private final DoubleProperty remainingColumnWidth = new SimpleDoubleProperty(INITIAL_WIDTH);

    /**
     * The is the minimum column width required to display the largest numeric value. Value is cached and only
     * updated with budget or transaction changes
     */
    private double minColumnWidth = INITIAL_WIDTH;

    /**
     * The is the minimum column width required to display the largest numeric summary value.
     */
    private final DoubleProperty minSummaryColumnWidth = new SimpleDoubleProperty(INITIAL_WIDTH);

    /**
     * Current index to be used for scrolling the display.  0 is the first period is displayed to the left
     */
    private int index;

    /**
     * The number of visible columns.
     */
    private final IntegerProperty visibleColumnCount = new SimpleIntegerProperty(1);

    /**
     * The number of periods in the model.
     */
    private final IntegerProperty periodCount = new SimpleIntegerProperty(1);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Listens to changes to the width of the period table and optimizes the column widths.
     */
    private ChangeListener<Number> tableWidthChangeListener;

    /**
     * Listens for changes to the font scale
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Number> fontScaleListener;

    /**
     * Rate limiting executor.
     */
    private ScheduledThreadPoolExecutor rateLimitExecutor;

    /**
     * Maximum update period in milliseconds
     */
    private static final int UPDATE_PERIOD = 350;

    /**
     * Used to alter timing for rate limiting the first boot for a better visual effect
     */
    private volatile boolean booted = false;

    private double startDragX;

    private double startDragWidth;

    /**
     * Pseudo divider width is a function of the base text height
     */
    private double dividerWidth;

    @FXML
    private void initialize() {
        runningTotalsButton.selectedProperty().setValue(preferences.getBoolean(RUNNING_TOTALS, false));

        rateLimitExecutor = new ScheduledThreadPoolExecutor(1,
                new DefaultDaemonThreadFactory("Budget View Rate Limit Executor"),
                new ThreadPoolExecutor.DiscardPolicy());

        tableWidthChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null && !oldValue.equals(newValue)) {
                optimizeColumnWidths();
            }
        };

        handleFontHeightChange();

        yearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear() - YEAR_MARGIN, LocalDate.now().getYear() + YEAR_MARGIN,
                LocalDate.now().getYear(), 1));

        accountTreeView.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS, StyleClass.HIDE_VERTICAL_CSS);
        accountTreeView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        accountTreeView.setShowRoot(false);
        accountTreeView.setEditable(true);
        accountTreeView.fixedCellSizeProperty().bind(rowHeight);

        accountSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountSummaryTable.getStyleClass().addAll(StyleClass.HIDDEN_ROW_FOCUS);
        accountSummaryTable.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS, StyleClass.HIDE_VERTICAL_CSS);
        accountSummaryTable.setItems(expandedAccountList);
        accountSummaryTable.fixedCellSizeProperty().bind(rowHeight);
        accountSummaryTable.setSelectionModel(new NullTableViewSelectionModel<>(accountSummaryTable));

        accountTypeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountTypeTable.getStyleClass().addAll(StyleClass.HIDDEN_COLUMN_HEADER, StyleClass.HIDDEN_ROW_FOCUS);
        accountTypeTable.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS, StyleClass.HIDE_VERTICAL_CSS);
        accountTypeTable.setItems(accountGroupList);
        accountTypeTable.fixedCellSizeProperty().bind(rowHeight);
        accountTypeTable.prefHeightProperty()
                .bind(rowHeight.multiply(Bindings.size(accountGroupList)).add(BORDER_MARGIN));
        accountTypeTable.setSelectionModel(new NullTableViewSelectionModel<>(accountTypeTable));

        // widths need to be bound to the tree view widths for drag/resizing to work
        accountTypeTable.minWidthProperty().bind(accountTreeView.minWidthProperty());
        accountTypeTable.prefWidthProperty().bind(accountTreeView.prefWidthProperty());

        accountGroupPeriodSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountGroupPeriodSummaryTable.getStyleClass().addAll(StyleClass.HIDDEN_COLUMN_HEADER, StyleClass.HIDDEN_ROW_FOCUS);
        accountGroupPeriodSummaryTable.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS, StyleClass.HIDE_VERTICAL_CSS);
        accountGroupPeriodSummaryTable.setItems(accountGroupList);
        accountGroupPeriodSummaryTable.fixedCellSizeProperty().bind(rowHeight);
        accountGroupPeriodSummaryTable.prefHeightProperty()
                .bind(rowHeight.multiply(Bindings.size(accountGroupList)).add(BORDER_MARGIN));
        accountGroupPeriodSummaryTable.setSelectionModel(new NullTableViewSelectionModel<>(accountGroupPeriodSummaryTable));

        buildAccountTreeTable();
        buildAccountTypeTable();
        buildAccountSummaryTable();
        buildAccountGroupSummaryTable();

        accountSummaryTable.maxWidthProperty().bind(minSummaryColumnWidth.multiply(3.0).add(BORDER_MARGIN));
        accountGroupPeriodSummaryTable.maxWidthProperty().bind(minSummaryColumnWidth.multiply(3.0).add(BORDER_MARGIN));

        accountSummaryTable.minWidthProperty().bind(minSummaryColumnWidth.multiply(3.0).add(BORDER_MARGIN));
        accountGroupPeriodSummaryTable.minWidthProperty().bind(minSummaryColumnWidth.multiply(3.0).add(BORDER_MARGIN));

        accountTreeView.expandedItemCountProperty().addListener((observable, oldValue, newValue)
                -> JavaFXUtils.runLater(this::updateExpandedAccountList));

        // Calling handleBudgetChange which works for most changes, but can trigger an exception.
        // handleBudgetUpdate rate limits and prevents an exception.
        final ChangeListener<Object> budgetChangeListener = (observable, oldValue, newValue) -> handleBudgetUpdate();

        budget.addListener(budgetChangeListener);
        yearSpinner.valueProperty().addListener(budgetChangeListener);
        runningTotalsButton.selectedProperty().addListener(budgetChangeListener);
        visibleColumnCount.addListener(budgetChangeListener);

        runningTotalsButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                preferences.putBoolean(RUNNING_TOTALS, newValue));

        /* Setting the tables as un-managed effectively removes these tables from the GridPane.  The tables are
           redundant if showing the amounts as running balances. */
        accountSummaryTable.managedProperty().bind(runningTotalsButton.selectedProperty().not());
        accountGroupPeriodSummaryTable.managedProperty().bind(runningTotalsButton.selectedProperty().not());

        horizontalScrollBar.setMin(0);
        horizontalScrollBar.maxProperty().bind(periodCount.subtract(visibleColumnCount));
        horizontalScrollBar.setUnitIncrement(1);
        horizontalScrollBar.disableProperty().bind(periodCount.lessThanOrEqualTo(1));

        // shift the table right and left with the ScrollBar value
        horizontalScrollBar.valueProperty().addListener((observable, oldValue, newValue) -> {

                    /* must be synchronized to prevent a race condition from multiple events and an out of
                     * bounds exception */
                    synchronized (this) {

                        /* don't try unless columns exist.  This can occur if the UI is not large enough to display
                         * a minimum of one period of information.
                         */
                        if (periodTable.getColumns().size() > 0) {
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
                }
        );

        // listen for changes in the font scale and update.  Listener needs to be weak to prevent memory leaks
        fontScaleListener = (observable, oldValue, newValue) -> handleFontHeightChange();
        ThemeManager.fontScaleProperty().addListener(new WeakChangeListener<>(fontScaleListener));

        accountTreeView.setOnMouseMoved(this::handleMouseMove);         // cursor handler
        accountTreeView.setOnMouseDragged(this::handleDividerDrag);     // drag handler
        accountTreeView.setOnMousePressed(this::handleMouseClicked);    // drag handler

        accountTypeTable.setOnMouseMoved(this::handleMouseMove);         // cursor handler
        accountTypeTable.setOnMouseDragged(this::handleDividerDrag);     // drag handler
        accountTypeTable.setOnMousePressed(this::handleMouseClicked);    // drag handler

        JavaFXUtils.runLater(() -> accountTreeView.setPrefWidth(preferences.getDouble(ACCOUNT_COLUMN_WIDTH, INITIAL_WIDTH * 2)));
    }

    /**
     * Determines if the cursor is hovering over the pseudo divider
     *
     * @param xPos x position of the scene
     * @return true if hovering over the divider
     */
    private boolean isOnDivider(final double xPos) {
        final Point2D nodeInScene
                = accountTreeView.localToScene(accountTreeView.getLayoutX(), accountTreeView.getLayoutY());

        return Math.abs(accountTreeView.getWidth() + nodeInScene.getX() - xPos) < (dividerWidth * 0.5);
    }

    private void handleDividerDrag(final MouseEvent event) {
        accountTreeView.setPrefWidth(Math.max(startDragWidth + event.getSceneX() - startDragX, INITIAL_WIDTH * 2));
        preferences.putDouble(ACCOUNT_COLUMN_WIDTH, accountTreeView.getWidth());
        event.consume();
    }

    /**
     * Saves information for calculating drag/resize of the account tree column
     *
     * @param event Mouse event
     */
    private void handleMouseClicked(final MouseEvent event) {
        startDragX = event.getSceneX();
        startDragWidth = accountTreeView.getWidth();
        event.consume();
    }

    /**
     * Handles changing the cursor shape when hovering over the pseudo divider
     *
     * @param event Mouse event
     */
    private void handleMouseMove(final MouseEvent event) {
        gridPane.getScene().setCursor(event != null && isOnDivider(event.getSceneX()) ? Cursor.H_RESIZE : Cursor.DEFAULT);
    }

    private void rateLimitUpdate(final Runnable runnable) {
        rateLimitExecutor.schedule(() -> {
            if (rateLimitExecutor.getQueue().size() < 1) {   // ignore if we already have one waiting in the queue
                JavaFXUtils.runNow(runnable);                // update at the front of the queue with rate limiting
            }

            booted = true;
        }, booted ? UPDATE_PERIOD : 0, TimeUnit.MILLISECONDS);
    }

    @FXML
    private void handleShiftLeft() {
        lock.writeLock().lock();

        try {
            // remove the right column
            periodTable.getColumns().remove(visibleColumnCount.get() - 1);
            periodSummaryTable.getColumns().remove(visibleColumnCount.get() - 1);

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

            int newColumn = index + visibleColumnCount.get();

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
        return budget;
    }

    private void handleFontHeightChange() {
        rowHeight.set(ThemeManager.getBaseTextHeight() * ROW_HEIGHT_MULTIPLIER);
        dividerWidth = ThemeManager.getBaseTextHeight();    // update divider width
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

        if (vDataScrollBar.isEmpty() || accountScrollBar.isEmpty() || accountSumScrollBar.isEmpty()) {
            // re-spawn off the off the application thread
            new Thread(this::bindScrollBars).start();
        } else {    // all here, lets bind then now
            JavaFXUtils.runLater(() -> {
                verticalScrollBar.minProperty().bindBidirectional(accountScrollBar.get().minProperty());
                verticalScrollBar.maxProperty().bindBidirectional(accountScrollBar.get().maxProperty());
                verticalScrollBar.valueProperty().bindBidirectional(accountScrollBar.get().valueProperty());

                accountScrollBar.get().valueProperty().bindBidirectional(vDataScrollBar.get().valueProperty());
                accountSumScrollBar.get().valueProperty().bindBidirectional(vDataScrollBar.get().valueProperty());
            });
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

            if (budget.get() != null && engine != null) {

                // unregister listener from the old model because the model will be replaced
                if (budgetResultsModel != null) {
                    budgetResultsModel.removeMessageListener(this); // unregister from the old model
                }

                // Build the new results model
                budgetResultsModel = new BudgetResultsModel(budget.get(), yearSpinner.getValue(),
                        engine.getDefaultCurrency(), runningTotalsButton.isSelected());

                // model has changed, calculate the minimum column width for the summary columns
                minSummaryColumnWidth.set(calculateMinSummaryWidthColumnWidth());

                // model has changed, calculate the minimum column width the data model needs
                minColumnWidth = calculateMinPeriodColumnWidth();

                // register the listener with the new model
                budgetResultsModel.addMessageListener(this);    // register with the new model

                // update the number of periods the budget model has
                periodCount.set(budgetResultsModel.getDescriptorList().size());

                // load the model
                loadModel();
            } else {
                JavaFXUtils.runLater(() -> {
                    accountTreeView.setRoot(null);
                    expandedAccountList.clear();
                    accountGroupList.clear();
                });
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

            JavaFXUtils.runLater(this::bindScrollBars);

            JavaFXUtils.runLater(this::focusCurrentPeriod);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void optimizeColumnWidths() {
        lock.writeLock().lock();

        try {
            final double availWidth = periodTable.getWidth() - BORDER_MARGIN;   // width of the table

            /* calculate the number of visible columns, period columns are 3 columns wide
               the maximum is caped to no more than the number of available periods */
            final int maxVisible = Math.min((int) Math.floor(availWidth / (minColumnWidth * 3.0)),
                    budgetResultsModel.getDescriptorList().size());

            /* update the number of visible columns factoring in the size of the descriptor list */
            visibleColumnCount.set((Math.min(budgetResultsModel.getDescriptorList().size(), maxVisible)));

            if (visibleColumnCount.get() == 0) {
                periodTable.placeholderProperty()
                        .setValue(new Label(resources.getString("Message.Warn.WindowWidth")));

                periodSummaryTable.placeholderProperty()
                        .setValue(new Label(resources.getString("Message.Warn.WindowWidth")));
            }

            final double width = Math.floor(availWidth /
                    Math.min(budgetResultsModel.getDescriptorList().size() * 3, maxVisible * 3));

            columnWidth.set(width);

            double remainder = availWidth - (maxVisible * 3.0 * width);

            remainingColumnWidth.set(Math.floor(width + (remainder / 3.0)));
        } finally {
            lock.writeLock().unlock();
        }
    }

    void focusCurrentPeriod() {
        final LocalDate now = LocalDate.now();

        final List<BudgetPeriodDescriptor> budgetPeriodDescriptorList = budgetResultsModel.getDescriptorList();

        for (int i = 0; i < budgetPeriodDescriptorList.size(); i++) {
            final BudgetPeriodDescriptor budgetPeriodDescriptor = budgetPeriodDescriptorList.get(i);

            if (budgetPeriodDescriptor.isBetween(now)) {
                final int index = Math.max(Math.min(i, periodCount.subtract(visibleColumnCount).intValue()), 0);

                JavaFXUtils.runLater(() -> horizontalScrollBar.setValue(index));
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
                // this may be reached outside the platform thread if a uses is click happy
                if (Platform.isFxApplicationThread()) {
                    handleEditAccountGoals(account);
                } else {
                    JavaFXUtils.runLater(() -> handleEditAccountGoals(account));
                }
            }
        });
        nameColumn.setEditable(true);

        headerColumn.getColumns().add(nameColumn);

        accountTreeView.getColumns().add(headerColumn);
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
        lockColumnBehavior(budgetedColumn, minSummaryColumnWidth);

        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<Account, BigDecimal> actualColumn = new TableColumn<>(resources.getString("Column.Actual"));
        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        actualColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        lockColumnBehavior(actualColumn, minSummaryColumnWidth);

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
        lockColumnBehavior(remainingColumn, minSummaryColumnWidth);

        headerColumn.getColumns().add(remainingColumn);
        headerColumn.resizableProperty().set(false);

        accountSummaryTable.getColumns().add(headerColumn);
    }

    private TableColumn<Account, BigDecimal> buildAccountPeriodResultsColumn(final int index) {

        final BudgetPeriodDescriptor descriptor = budgetResultsModel.getDescriptorList().get(index);

        // determine if the column is to be highlighted if the period is not yearly
        final Boolean highlight = (descriptor.isBetween(LocalDate.now()) ? Boolean.TRUE : Boolean.FALSE)
                && budget.get().getBudgetPeriod() != Period.YEARLY;

        final TableColumn<Account, BigDecimal> headerColumn = new TableColumn<>(descriptor.getPeriodDescription());

        final TableColumn<Account, BigDecimal> budgetedColumn
                = new TableColumn<>(resources.getString("Column.Budgeted"));

        budgetedColumn.getProperties().put(NOW, highlight);
        budgetedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getBudgeted());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        budgetedColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        lockColumnBehavior(budgetedColumn, columnWidth);

        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<Account, BigDecimal> actualColumn = new TableColumn<>(resources.getString("Column.Actual"));

        actualColumn.getProperties().put(NOW, highlight);
        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        actualColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        lockColumnBehavior(actualColumn, columnWidth);

        headerColumn.getColumns().add(actualColumn);

        final TableColumn<Account, BigDecimal> remainingColumn
                = new TableColumn<>(resources.getString("Column.Remaining"));

        remainingColumn.getProperties().put(NOW, highlight);
        remainingColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getRemaining());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        remainingColumn.setCellFactory(param -> new AccountCommodityFormatTableCell());
        lockColumnBehavior(remainingColumn, remainingColumnWidth);

        headerColumn.getColumns().add(remainingColumn);

        headerColumn.resizableProperty().set(false);

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
        periodTable.getStyleClass().addAll(StyleClass.HIDDEN_ROW_FOCUS);
        periodTable.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS, StyleClass.HIDE_VERTICAL_CSS);
        periodTable.fixedCellSizeProperty().bind(rowHeight);
        periodTable.setSelectionModel(new NullTableViewSelectionModel<>(periodTable));

        // index exceeds allowed value because the user reduced the period count, reset to the maximum allowed value
        if (index > budgetResultsModel.getDescriptorList().size() - visibleColumnCount.get()) {
            index = budgetResultsModel.getDescriptorList().size() - visibleColumnCount.get();
        }

        final int periodCount = Math.min(visibleColumnCount.get(), budgetResultsModel.getDescriptorList().size());

        for (int i = index; i < index + periodCount; i++) {
            periodTable.getColumns().add(buildAccountPeriodResultsColumn(i));
        }

        periodTable.setItems(expandedAccountList);
        periodTable.widthProperty().addListener(tableWidthChangeListener);

        periodTable.setOnMouseMoved(this::handleMouseMove);         // cursor
        periodTable.setOnMouseDragged(this::handleDividerDrag);     // drag
        periodTable.setOnMousePressed(this::handleMouseClicked);    // drag
    }

    private TableColumn<AccountGroup, BigDecimal> buildAccountPeriodSummaryColumn(final int index) {
        final BudgetPeriodDescriptor descriptor = budgetResultsModel.getDescriptorList().get(index);

        // determine if the column is to be highlighted if the period is not yearly
        final Boolean highlight = (descriptor.isBetween(LocalDate.now()) ? Boolean.TRUE : Boolean.FALSE)
                && budget.get().getBudgetPeriod() != Period.YEARLY;

        final TableColumn<AccountGroup, BigDecimal> headerColumn = new TableColumn<>(descriptor.getPeriodDescription());

        final TableColumn<AccountGroup, BigDecimal> budgetedColumn
                = new TableColumn<>(resources.getString("Column.Budgeted"));

        budgetedColumn.getProperties().put(NOW, highlight);
        budgetedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getBudgeted());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        budgetedColumn.setCellFactory(param -> new AccountGroupTableCell());
        lockColumnBehavior(budgetedColumn, columnWidth);

        headerColumn.getColumns().add(budgetedColumn);

        final TableColumn<AccountGroup, BigDecimal> actualColumn
                = new TableColumn<>(resources.getString("Column.Actual"));

        actualColumn.getProperties().put(NOW, highlight);
        actualColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor, param.getValue()).getChange());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        actualColumn.setCellFactory(param -> new AccountGroupTableCell());
        lockColumnBehavior(actualColumn, columnWidth);

        headerColumn.getColumns().add(actualColumn);

        final TableColumn<AccountGroup, BigDecimal> remainingColumn
                = new TableColumn<>(resources.getString("Column.Remaining"));

        remainingColumn.getProperties().put(NOW, highlight);
        remainingColumn.setCellValueFactory(param -> {
            if (param.getValue() != null) {
                return new SimpleObjectProperty<>(budgetResultsModel.getResults(descriptor,
                        param.getValue()).getRemaining());
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        remainingColumn.setCellFactory(param -> new AccountGroupTableCell());

        // the max width is not bound to allow last column to grow and fill any voids
        lockColumnBehavior(remainingColumn, remainingColumnWidth);

        headerColumn.getColumns().add(remainingColumn);

        return headerColumn;
    }

    private void lockColumnBehavior(final TableColumn<?, ?> column, final DoubleProperty columnWidth) {
        column.minWidthProperty().bind(columnWidth);
        column.maxWidthProperty().bind(columnWidth);
        column.setSortable(false);
        column.resizableProperty().set(false);
        column.setReorderable(false);
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
        periodSummaryTable.getStyleClass().add(StyleClass.HIDDEN_COLUMN_HEADER);
        periodSummaryTable.getStyleClass().addAll(StyleClass.HIDDEN_ROW_FOCUS);
        periodSummaryTable.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS, StyleClass.HIDE_VERTICAL_CSS);
        periodSummaryTable.fixedCellSizeProperty().bind(rowHeight);
        periodSummaryTable.prefHeightProperty()
                .bind(rowHeight.multiply(Bindings.size(accountGroupList)).add(BORDER_MARGIN));
        periodSummaryTable.setSelectionModel(new NullTableViewSelectionModel<>(periodSummaryTable));

        final int periodCount = Math.min(visibleColumnCount.get(), budgetResultsModel.getDescriptorList().size());

        for (int i = index; i < index + periodCount; i++) {
            periodSummaryTable.getColumns().add(buildAccountPeriodSummaryColumn(i));
        }

        periodSummaryTable.setItems(accountGroupList);

        periodSummaryTable.setOnMouseMoved(this::handleMouseMove);         // cursor
        periodSummaryTable.setOnMouseDragged(this::handleDividerDrag);     // drag
        periodSummaryTable.setOnMousePressed(this::handleMouseClicked);    // drag
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
        budgetedColumn.minWidthProperty().bind(minSummaryColumnWidth);
        budgetedColumn.maxWidthProperty().bind(minSummaryColumnWidth);
        budgetedColumn.setSortable(false);
        budgetedColumn.resizableProperty().set(false);

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
        actualColumn.minWidthProperty().bind(minSummaryColumnWidth);
        actualColumn.maxWidthProperty().bind(minSummaryColumnWidth);
        actualColumn.setSortable(false);
        actualColumn.resizableProperty().set(false);

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
        remainingColumn.minWidthProperty().bind(minSummaryColumnWidth);
        remainingColumn.maxWidthProperty().bind(minSummaryColumnWidth);
        remainingColumn.setSortable(false);
        remainingColumn.resizableProperty().set(false);

        headerColumn.getColumns().add(remainingColumn);

        accountGroupPeriodSummaryTable.getColumns().add(headerColumn);
    }

    private double calculateMinColumnWidth(final BudgetPeriodResults budgetPeriodResults) {
        double max = 0;
        double min = 0;

        max = Math.max(max, budgetPeriodResults.getBudgeted().doubleValue());
        max = Math.max(max, budgetPeriodResults.getChange().doubleValue());
        max = Math.max(max, budgetPeriodResults.getRemaining().doubleValue());

        min = Math.min(min, budgetPeriodResults.getBudgeted().doubleValue());
        min = Math.min(min, budgetPeriodResults.getChange().doubleValue());
        min = Math.min(min, budgetPeriodResults.getRemaining().doubleValue());

        return Math.max(JavaFXUtils.getDisplayedTextWidth(
                NumericFormats.getFullCommodityFormat(budgetResultsModel.getBaseCurrency()).format(max) +
                        BORDER_MARGIN, null), JavaFXUtils.getDisplayedTextWidth(
                NumericFormats.getFullCommodityFormat(budgetResultsModel.getBaseCurrency()).format(min) +
                        BORDER_MARGIN, null));
    }

    private double calculateMinColumnWidth(final BudgetPeriodDescriptor descriptor, final Account account) {
        return calculateMinColumnWidth(budgetResultsModel.getResults(descriptor, account));
    }

    private double calculateMinColumnWidth(final Account account) {
        return calculateMinColumnWidth(budgetResultsModel.getResults(account));
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
                NumericFormats.getFullCommodityFormat(budgetResultsModel.getBaseCurrency()).format(max) +
                        BORDER_MARGIN, null),
                JavaFXUtils.getDisplayedTextWidth(
                        NumericFormats.getFullCommodityFormat(budgetResultsModel.getBaseCurrency()).format(min) +
                                BORDER_MARGIN, null));
    }

    private double calculateMinPeriodColumnWidth() {
        double max = 0;

        for (final BudgetPeriodDescriptor descriptor : budgetResultsModel.getDescriptorList()) {
            for (final Account account : expandedAccountList) {
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

        for (final AccountGroup group : accountGroupList) {
            List<BigDecimal> remaining = budgetResultsModel.getDescriptorList().parallelStream().map(descriptor ->
                    budgetResultsModel.getResults(descriptor, group).getRemaining()).collect(Collectors.toList());

            final HBox hBox = new HBox(new Label(group.toString()), new BudgetSparkLine(remaining));
            hBox.setAlignment(Pos.CENTER_LEFT);

            sparkLinePane.getChildren().add(hBox);
        }
    }

    private void handleEditAccountGoals(@NotNull final Account account) {
        Objects.requireNonNull(account);

        final FXMLUtils.Pair<BudgetGoalsDialogController> pair =
                FXMLUtils.load(BudgetGoalsDialogController.class.getResource("BudgetGoalsDialog.fxml"),
                        resources.getString("Title.BudgetGoal") + " - " + account.getName());

        pair.getController().startMonthProperty().set(budget.get().getStartMonth());
        pair.getController().accountProperty().set(account);
        pair.getController().workingYearProperty().set(yearSpinner.getValue());

        try {
            final BudgetGoal oldGoal = (BudgetGoal) budgetProperty().get().getBudgetGoal(account).clone();
            pair.getController().budgetGoalProperty().set(oldGoal);
        } catch (final CloneNotSupportedException e) {
            Logger.getLogger(BudgetTableController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        StageUtils.addBoundsListener(pair.getStage(), BudgetGoalsDialogController.class);

        pair.getStage().showAndWait();

        final Optional<BudgetGoal> optional = pair.getController().getResult();

        optional.ifPresent(budgetGoal -> {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.updateBudgetGoals(budget.get(), account, budgetGoal);
        });
    }

    @Override
    public void messagePosted(final Message message) {
        switch (message.getEvent()) {
            case FILE_CLOSING:
                budgetResultsModel.removeMessageListener(this);
                budgetProperty().set(null);
                break;
            case BUDGET_REMOVE:
                if (budget.get().equals(message.getObject(MessageProperty.BUDGET))) {
                    budget.set(null);
                    budgetResultsModel.removeMessageListener(this);
                }
                break;
            case ACCOUNT_ADD:
            case ACCOUNT_MODIFY:
            case ACCOUNT_REMOVE:
            case BUDGET_UPDATE:
            case BUDGET_GOAL_UPDATE:
                if (budget.get().equals(message.getObject(MessageProperty.BUDGET))) {
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

            setId(NORMAL_CELL_ID);   // reset, cell is reused

            final boolean now = Boolean.TRUE == getTableColumn().getProperties().getOrDefault(NOW, Boolean.FALSE);

            if (!empty && amount != null && getTableRow() != null) {
                final Account account = expandedAccountList.get(getTableRow().getIndex());
                final NumberFormat format = NumericFormats.getFullCommodityFormat(account.getCurrencyNode());

                setText(format.format(amount));

                if (account.isPlaceHolder()) {
                    if (amount.signum() < 0) {
                        setId(now ? TODAY_BOLD_NEGATIVE_LABEL_ID : BOLD_NEGATIVE_LABEL_ID);
                    } else {
                        setId(now ? TODAY_BOLD_LABEL_ID : BOLD_LABEL_ID);
                    }
                } else {
                    if (amount.signum() < 0) {
                        setId(now ? TODAY_NORMAL_NEGATIVE_CELL_ID : NORMAL_NEGATIVE_CELL_ID);
                    } else {
                        setId(now ? TODAY_NORMAL_CELL_ID : NORMAL_CELL_ID);
                    }
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
            format = NumericFormats.getFullCommodityFormat(budgetResultsModel.getBaseCurrency());
        }

        @Override
        protected void updateItem(final BigDecimal amount, final boolean empty) {
            super.updateItem(amount, empty);  // required

            setId(NORMAL_CELL_ID);   // reset, cell is reused

            final boolean now = Boolean.TRUE == getTableColumn().getProperties().getOrDefault(NOW, Boolean.FALSE);

            if (!empty && amount != null && getTableRow() != null) {
                setText(format.format(amount));

                if (amount.signum() < 0) {
                    setId(now ? TODAY_NORMAL_NEGATIVE_CELL_ID : NORMAL_NEGATIVE_CELL_ID);
                } else {
                    setId(now ? TODAY_NORMAL_CELL_ID : NORMAL_CELL_ID);
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

            setId(NORMAL_CELL_ID);   // reset, cell is reused

            if (!empty && account != null && getTreeTableRow() != null) {
                setText(account.getName());

                if (account.isPlaceHolder()) {
                    setId(BOLD_LABEL_ID);
                } else {
                    setId(NORMAL_CELL_ID);
                }
            } else {
                setText(null);
            }
        }
    }
}
