/**
 * A dialog for displaying recurring event / transactions when they occur.
 *
 * @author Craig Cavanaugh
 */
package jgnash.uifx.views.recurring;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;

import jgnash.engine.recurring.Reminder;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.util.ResourceUtils;

/**
 * @author Craig Cavanaugh
 */
public class RecurringEntryDialog {

    private final ObjectProperty<RecurringPropertiesController> controllerProperty = new SimpleObjectProperty<>();

    private RecurringEntryDialog(final Reminder reminder) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final Stage stage = FXMLUtils.loadFXML(RecurringPropertiesController.class.getResource("RecurringProperties.fxml")
                , controllerProperty, ResourceUtils.getBundle());

        Objects.requireNonNull(controllerProperty.get());

        if (reminder == null) {
            stage.setTitle(resources.getString("Title.NewReminder"));
        } else {
            stage.setTitle(resources.getString("Title.ModifyReminder"));
            controllerProperty.get().showReminder(reminder);
        }

        stage.setResizable(false);

        StageUtils.addBoundsListener(stage, RecurringEntryDialog.class);

        stage.show();
    }

    public static Optional<Reminder> showAndWait(final Reminder reminder) {
        final RecurringEntryDialog dialog = new RecurringEntryDialog(reminder);
        return dialog.controllerProperty.get().getReminder();
    }
}
