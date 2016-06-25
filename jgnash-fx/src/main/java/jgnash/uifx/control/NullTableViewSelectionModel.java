package jgnash.uifx.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

/**
 * Disables selection.
 *
 * @author Craig Cavanaugh
 */
public class NullTableViewSelectionModel<S> extends TableView.TableViewSelectionModel<S> {

    public NullTableViewSelectionModel(TableView<S> tableView) {
        super(tableView);
    }

    @SuppressWarnings("rawtypes")
	@Override
    public ObservableList<TablePosition> getSelectedCells() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public void clearSelection(int row, TableColumn<S,?> column) {

    }

    @Override
    public void clearAndSelect(int row, TableColumn<S,?> column) {

    }

    @Override
    public void select(int row, TableColumn<S,?> column) {

    }



    @Override
    public boolean isSelected(int row, TableColumn<S,?> column) {
        return false;
    }

    @Override
    public void selectLeftCell() {

    }

    @Override
    public void selectRightCell() {

    }

    @Override
    public void selectAboveCell() {

    }

    @Override
    public void selectBelowCell() {

    }

    public void selectIndices(int row, int... rows) {

    }

    @Override public void selectAll() {

    }

    @Override public void clearSelection(int index) {

    }

    @Override public void clearSelection() {

    }
}
