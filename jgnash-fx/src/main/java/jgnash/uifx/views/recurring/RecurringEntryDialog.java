package jgnash.uifx.views.recurring;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import jgnash.engine.recurring.Reminder;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.resource.util.ResourceUtils;

/**
 * A dialog for displaying recurring event / transactions when they occur.
 *
 * @author Craig Cavanaugh
 */
public class RecurringEntryDialog {

    private final ObjectProperty<RecurringPropertiesController> controller = new SimpleObjectProperty<>();

    private RecurringEntryDialog(final Reminder reminder) {
        final FXMLUtils.Pair<RecurringPropertiesController> pair =
                FXMLUtils.load(RecurringPropertiesController.class.getResource("RecurringProperties.fxml"),
                        reminder != null ? ResourceUtils.getString("Title.ModifyReminder")
                                : ResourceUtils.getString("Title.NewReminder"));

        controller.set(pair.getController());

        if (reminder != null) {
            controller.get().showReminder(reminder);
        }

        pair.getStage().setResizable(false);

        StageUtils.addBoundsListener(pair.getStage(), RecurringEntryDialog.class, MainView.getPrimaryStage());

        pair.getStage().showAndWait();  // must block the UI so the return value is generated correctly
    }

    public static Optional<Reminder> showAndWait(final Reminder reminder) {
        final RecurringEntryDialog dialog = new RecurringEntryDialog(reminder);
        return dialog.controller.get().getReminder();
    }
}
