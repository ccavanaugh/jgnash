/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.ui.reconcile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.JFloatField;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.ui.util.DialogUtils;
import jgnash.util.DateUtils;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Account reconcile settings dialog.
 *
 * @author Craig Cavanaugh
 */
public class ReconcileSettingsDialog extends JDialog implements ActionListener {

    private static final int FUZZY_DATE_RANGE = 2;

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final JButton okButton;

    private final JButton cancelButton;

    private final JFloatField openField = new JFloatField();

    private final JFloatField closeField = new JFloatField();

    private final DatePanel datePanel = new DatePanel();

    private boolean returnValue = false;

    private final Account account;

    public ReconcileSettingsDialog(@NotNull final Account account) {
        super(UIApplication.getFrame(), true);

        this.account = account;

        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        layoutMainPanel();

        setTitle(rb.getString("Title.ReconcileSettings"));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        /* Load default values first */

        // Last date of the month for the 1st unreconciled transaction
        LocalDate statementDate = account.getFirstUnreconciledTransactionDate().with(TemporalAdjusters.lastDayOfMonth());

        // Balance at the 1st unreconciled transaction
        BigDecimal openingBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getOpeningBalanceForReconcile());

        // Balance at the statement date
        BigDecimal closingBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getBalance(statementDate));

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        // Determine the best reconcile values to use
        if (engine != null) {
            LocalDate lastSuccessDate = null;
            LocalDate lastAttemptDate = null;
            LocalDate lastStatementDate = LocalDate.now();

            BigDecimal lastOpeningBalance = null;
            BigDecimal lastClosingBalance = null;

            String value = account.getAttribute(Account.RECONCILE_LAST_SUCCESS_DATE);
            if (value != null) {
                lastSuccessDate = DateUtils.asLocalDate(Long.parseLong(value));
            }

            value = account.getAttribute(Account.RECONCILE_LAST_ATTEMPT_DATE);
            if (value != null) {
                lastAttemptDate = DateUtils.asLocalDate(Long.parseLong(value));
            }

            value = account.getAttribute(Account.RECONCILE_LAST_STATEMENT_DATE);
            if (value != null) {
                lastStatementDate = DateUtils.asLocalDate(Long.parseLong(value));
            }

            value = account.getAttribute(Account.RECONCILE_LAST_CLOSING_BALANCE);
            if (value != null) {
                lastClosingBalance = new BigDecimal(value);
            }

            value = account.getAttribute(Account.RECONCILE_LAST_OPENING_BALANCE);
            if (value != null) {
                lastOpeningBalance = new BigDecimal(value);
            }

            if (lastSuccessDate != null) { // we had prior success, use a new date one month out if the date is earlier than today
                if (DateUtils.before(lastStatementDate, LocalDate.now())) {

                    // set the new statement date
                    statementDate = lastStatementDate.plusMonths(1);

                    // use the account balance of the estimated statement date
                    closingBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getBalance(statementDate));
                }
            }

            // an recent attempt has been made before, override defaults
            if (lastAttemptDate != null && Math.abs(ChronoUnit.DAYS.between(lastAttemptDate, LocalDate.now())) <= FUZZY_DATE_RANGE) {
                if (lastStatementDate != null) {
                    statementDate = lastStatementDate; // set the new statement date + 1 month
                }

                if (lastOpeningBalance != null) {
                    openingBalance = lastOpeningBalance;
                }

                if (lastClosingBalance != null) {
                    closingBalance = lastClosingBalance;
                }
            }
        }

        datePanel.setDate(statementDate);
        openField.setDecimal(openingBalance);
        closeField.setDecimal(closingBalance);

        DialogUtils.addBoundsListener(this);
    }

    private void layoutMainPanel() {
        final FormLayout layout = new FormLayout("p:g, $lcgap, max(70dlu;min)", "");
        final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);
        builder.rowGroupingEnabled(true);

        builder.append(rb.getString("Label.StatementDate"), datePanel);
        builder.append(rb.getString("Label.OpeningBalance"), openField);
        builder.append(rb.getString("Label.EndingBalance"), closeField);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(builder.getPanel());
        pack();
    }

    public BigDecimal getClosingBalance() {
        return closeField.getDecimal();
    }

    public BigDecimal getOpeningBalance() {
        return openField.getDecimal();
    }

    public LocalDate getStatementDate() {
        return datePanel.getLocalDate();
    }

    /**
     * Invoked when an action occurs
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        returnValue = e.getSource() == okButton;

        if (returnValue) {
            new Thread() {  // save the value in a background thread as this could take a little bit
                @Override
                public void run() {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    if (engine != null) {
                        engine.setAccountAttribute(account, Account.RECONCILE_LAST_ATTEMPT_DATE, Long.toString(DateUtils.asEpochMilli(LocalDate.now())));
                        engine.setAccountAttribute(account, Account.RECONCILE_LAST_STATEMENT_DATE, Long.toString(DateUtils.asEpochMilli(getStatementDate())));
                        engine.setAccountAttribute(account, Account.RECONCILE_LAST_OPENING_BALANCE, getOpeningBalance().toString());
                        engine.setAccountAttribute(account, Account.RECONCILE_LAST_CLOSING_BALANCE, getClosingBalance().toString());
                    }
                }
            }.start();
        }

        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    public boolean showDialog() {
        setVisible(true);
        return returnValue;
    }
}
