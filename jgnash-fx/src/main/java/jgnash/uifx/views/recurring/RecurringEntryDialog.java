/**
 * A dialog for displaying recurring event / transactions when they occur.
 *
 * @author Craig Cavanaugh
 */
package jgnash.uifx.views.recurring;

import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;

import jgnash.engine.recurring.Reminder;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.ResourceUtils;

/**
 * @author Craig Cavanaugh
 */
public class RecurringEntryDialog {

    private RecurringEntryDialog(final Reminder reminder) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final ObjectProperty<RecurringPropertiesController> controllerProperty = new SimpleObjectProperty<>();

        final Stage stage = FXMLUtils.loadFXML(RecurringPropertiesController.class.getResource("RecurringProperties.fxml")
                , controllerProperty, ResourceUtils.getBundle());

        Objects.requireNonNull(controllerProperty.get());

        if (reminder == null) {
            stage.setTitle(resources.getString("Title.NewReminder"));
        } else {
            stage.setTitle(resources.getString("Title.ModifyReminder"));
            controllerProperty.get().showReminder(reminder);
        }

        stage.showAndWait();
    }

    public static void showAndWait(final Reminder reminder) {
        new RecurringEntryDialog(reminder);
    }
}
