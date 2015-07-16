package jgnash.uifx.views.register;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.views.main.MainApplication;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * A Stage that displays a single account register. Size and position is preserved
 *
 * @author Craig Cavanaugh
 */
public class RegisterStage extends Stage {

    final private static ListProperty<RegisterStage> registerStageListProperty
            = new SimpleListProperty<>(FXCollections.observableArrayList());

    RegisterStage(@NotNull final Account account) {
        super(StageStyle.DECORATED);

        final String formResource;

        if (account.isLocked()) {
            if (account.memberOf(AccountGroup.INVEST)) {
                formResource = "LockedInvestmentRegisterPane.fxml";
            } else {
                formResource = "LockedBasicRegisterPane.fxml";
            }
        } else {
            if (account.memberOf(AccountGroup.INVEST)) {
                formResource = "InvestmentRegisterPane.fxml";
            } else {
                formResource = "BasicRegisterPane.fxml";
            }
        }

        final RegisterPaneController controller = FXMLUtils.loadFXML(scene -> setScene(new Scene((Parent) scene)),
                formResource, ResourceUtils.getBundle());

        getScene().getStylesheets().addAll(MainApplication.DEFAULT_CSS);

        // Push the account to the controller at the end of the application thread
        Platform.runLater(() -> controller.accountProperty().setValue(account));

        updateTitle(account);

        registerStageListProperty.get().add(this);
    }

    @Override
    public void close() {
        registerStageListProperty.get().remove(this);
        super.close();
    }

    public static ListProperty<RegisterStage> registerStageListProperty() {
        return registerStageListProperty;
    }

    private void updateTitle(final Account account) {
        setTitle("jGnash - " + account.getPathName());
    }
}
