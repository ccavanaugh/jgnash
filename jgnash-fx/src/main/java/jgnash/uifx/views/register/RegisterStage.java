package jgnash.uifx.views.register;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Transaction;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainApplication;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * A Stage that displays a single account register. Size and position is preserved
 *
 * @author Craig Cavanaugh
 */
public class RegisterStage extends Stage {

    /**
     * Static list of register stages
     */
    final private static ListProperty<RegisterStage> registerStageListProperty
            = new SimpleListProperty<>(FXCollections.observableArrayList());

    private static final double SCALE_FACTOR = 0.7;

    final private ReadOnlyObjectWrapper<Account> accountProperty = new ReadOnlyObjectWrapper<>();

    private final RegisterPaneController controller;

    private RegisterStage(@NotNull final Account account) {
        super(StageStyle.DECORATED);

        accountProperty.setValue(account);

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

        controller = FXMLUtils.loadFXML(scene -> setScene(new Scene((Parent) scene)), formResource,
                ResourceUtils.getBundle());

        getScene().getStylesheets().addAll(MainApplication.DEFAULT_CSS);

        double minWidth = Double.MAX_VALUE;
        double minHeight = Double.MAX_VALUE;

        for (final Screen screen : Screen.getScreens()) {
            minWidth = Math.min(minWidth, screen.getVisualBounds().getWidth());
            minHeight = Math.min(minHeight, screen.getVisualBounds().getHeight());
        }

        setWidth(minWidth * SCALE_FACTOR);
        setHeight(minHeight * SCALE_FACTOR);

        // Push the account to the controller at the end of the application thread
        Platform.runLater(() -> controller.accountProperty().setValue(account));

        updateTitle(account);

        StageUtils.addBoundsListener(this, account.getUuid());

        registerStageListProperty.get().add(this);

        setOnHidden(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                registerStageListProperty.get().remove(RegisterStage.this);
            }
        });
    }

    public static RegisterStage getRegisterStage(@NotNull final Account account) {

        // look for an existing stage first
        for (final RegisterStage registerStage : registerStageListProperty) {
            if (registerStage.accountProperty.get().equals(account)) {
                registerStage.requestFocus();
                return registerStage;
            }
        }

        return new RegisterStage(account);
    }

    public void show(final Transaction transaction) {
        show();
        Platform.runLater(() -> controller.selectTransaction(transaction));
    }

    public ReadOnlyObjectProperty<Account> accountProperty() {
        return accountProperty.getReadOnlyProperty();
    }

    public static ListProperty<RegisterStage> registerStageList() {
        return registerStageListProperty;
    }

    private void updateTitle(final Account account) {
        setTitle("jGnash - " + account.getPathName());
    }
}
