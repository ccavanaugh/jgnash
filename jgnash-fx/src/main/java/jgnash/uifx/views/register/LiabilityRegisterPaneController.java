/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.uifx.views.register;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jgnash.engine.Account;
import jgnash.engine.AmortizeObject;
import jgnash.engine.CommodityNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.ResourceUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class LiabilityRegisterPaneController extends BankRegisterPaneController {

    @FXML
    private void handleAmortizeAction() {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(MainView.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getString("Title.AmortizationSetup"));

        final AmortizeSetupDialogController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AmortizeSetupDialog.fxml", ResourceUtils.getBundle());

        if (accountProperty().get().getAmortizeObject() != null) {
            controller.setAmortizeObject(accountProperty().get().getAmortizeObject());
        }

        dialog.getScene().getStylesheets().addAll(MainView.DEFAULT_CSS);

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AmortizeSetupDialogController.class, MainView.getPrimaryStage());

        dialog.showAndWait();

        if (controller.getResult()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Objects.requireNonNull(engine);

            if (!engine.setAmortizeObject(accountProperty().get(), controller.getAmortizeObject())) {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.AmortizationSave"));
            }
        }
    }

    @FXML
    private void handleNewPaymentAction() {
        AmortizeObject amortizeObject = accountProperty().get().getAmortizeObject();

        if (amortizeObject != null) {

            Transaction tran = null;

            final Account account = accountProperty().get();

            final Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainView.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getString("Title.NewTrans"));

            final DateTransNumberDialogController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                    "DateTransNumberDialog.fxml", ResourceUtils.getBundle());

            controller.setAccount(amortizeObject.getBankAccount());

            dialog.getScene().getStylesheets().addAll(MainView.DEFAULT_CSS);

            dialog.setResizable(false);

            StageUtils.addBoundsListener(dialog, DateTransNumberDialogController.class, MainView.getPrimaryStage());

            dialog.showAndWait();

            if (!controller.getResult()) {
                return;
            }

            BigDecimal balance = account.getBalance().abs();
            double payment = amortizeObject.getPayment();

            double interest;

            if (amortizeObject.getUseDailyRate()) {
                LocalDate today = controller.getDate();

                LocalDate last;

                if (account.getTransactionCount() > 0) {
                    last = account.getTransactionAt(account.getTransactionCount() - 1).getLocalDate();
                } else {
                    last = today;
                }

                interest = amortizeObject.getIPayment(balance, last, today); // get the interest portion

            } else {
                interest = amortizeObject.getIPayment(balance); // get the interest portion
            }

            // get debit account
            Account bank = amortizeObject.getBankAccount();

            if (bank != null) {
                CommodityNode n = bank.getCurrencyNode();

                Transaction transaction = new Transaction();
                transaction.setDate(controller.getDate());
                transaction.setNumber(controller.getNumber());
                transaction.setPayee(amortizeObject.getPayee());

                // transaction is made relative to the debit/checking account

                TransactionEntry entry = new TransactionEntry();

                // this entry is the principal payment
                entry.setCreditAccount(account);
                entry.setDebitAccount(bank);
                entry.setAmount(n.round(payment - interest));
                entry.setMemo(amortizeObject.getMemo());

                transaction.addTransactionEntry(entry);

                // handle interest portion of the payment
                Account i = amortizeObject.getInterestAccount();
                if (i != null && interest != 0.0) {
                    entry = new TransactionEntry();
                    entry.setCreditAccount(i);
                    entry.setDebitAccount(bank);
                    entry.setAmount(n.round(interest));
                    entry.setMemo(resources.getString("Word.Interest"));
                    transaction.addTransactionEntry(entry);
                }

                // a fee has been assigned
                if (amortizeObject.getFees().compareTo(BigDecimal.ZERO) != 0) {
                    Account f = amortizeObject.getFeesAccount();
                    if (f != null) {
                        entry = new TransactionEntry();
                        entry.setCreditAccount(f);
                        entry.setDebitAccount(bank);
                        entry.setAmount(amortizeObject.getFees());
                        entry.setMemo(resources.getString("Word.Fees"));
                        transaction.addTransactionEntry(entry);
                    }
                }

                // the remainder of the balance should be loan principal
                tran = transaction;
            }

            if (tran != null) {// display the transaction in the register

                TransactionDialog.showAndWait(amortizeObject.getBankAccount(), tran, transaction -> {
                    Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    Objects.requireNonNull(engine);

                    engine.addTransaction(transaction);
                });

            } else {
                StaticUIMethods.displayWarning(resources.getString("Message.Warn.ConfigAmortization"));
            }
        } else { // could not generate the transaction
            StaticUIMethods.displayWarning(resources.getString("Message.Warn.ConfigAmortization"));
        }

    }
}
