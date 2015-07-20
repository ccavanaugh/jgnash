package jgnash.uifx.dialog.security;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityNode;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;

import org.controlsfx.control.CheckListView;

/**
 * Historical import controller
 *
 * @author Craig Cavanaugh
 */
public class HistoricalImportController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckListView checkListView;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ResourceBundle resources;

    @FXML
    @SuppressWarnings("unchecked")
    void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final ObservableList<SecurityNode> securityNodes = FXCollections.observableArrayList();
        securityNodes.addAll(engine.getSecurities());

        final Iterator<SecurityNode> i = securityNodes.iterator();

        while (i.hasNext()) {
            if (i.next().getQuoteSource() == QuoteSource.NONE) {
                i.remove();
            }
        }

        FXCollections.sort(securityNodes);

        checkListView.getItems().addAll(securityNodes);

        startDatePicker.setValue(LocalDate.now().minusMonths(1));
    }

    @FXML
    private void handleSelectAllAction() {
        checkListView.getCheckModel().checkAll();
    }

    @FXML
    private void handleClearAllAction() {
        checkListView.getCheckModel().clearChecks();
    }

    @FXML
    private void handleInvertSelectionAction() {
        for (int i = 0; i < checkListView.getCheckModel().getItemCount(); i++) {
            if (checkListView.getCheckModel().isChecked(i)) {
                checkListView.getCheckModel().clearCheck(i);
            } else {
                checkListView.getCheckModel().check(i);
            }
        }
    }

    @FXML
    private void handleOkAction() {
        // TODO perform import
    }

    @FXML
    private void handleCancelAction() {
        ((Stage)parentProperty.get().getWindow()).close();
    }
}
