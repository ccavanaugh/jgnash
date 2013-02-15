/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.ui.report.compiled;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.JDateField;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Profit loss report
 * 
 * @author Michael Mueller
 * @author Craig Cavanaugh
 * @author David Robertson
 */
public class ProfitLossTXT {

    private NumberFormat numberFormat;

    private static final boolean SHOW_EMPTY_ACCOUNT = false;

    private ArrayList<BigDecimal> balance = new ArrayList<>();

    private final ArrayList<String> pl = new ArrayList<>();

    private CurrencyNode baseCommodity;

    private Date[] dates;

    private final Resource rb = Resource.get();

    public void run() {
        createReport();
        writePLFile(getFileName());
    }

    private void createReport() {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Account root = engine.getRootAccount();

        baseCommodity = engine.getDefaultCurrency();

        numberFormat = CommodityFormat.getFullNumberFormat(baseCommodity);
        SimpleDateFormat df = new SimpleDateFormat("dd-MMMMM-yyyy");

        dates = getDates();

        // title and dates
        pl.add(rb.getString("Title.ProfitLoss"));
        pl.add("");
        pl.add("From " + df.format(dates[0]) + " To " + df.format(dates[1]));
        pl.add("");
        pl.add("");

        //Income
        pl.add("Income");
        pl.add("------------------------------------------------------");
        getBalances(root, dates, AccountType.INCOME);

        //Add up the  Gross Income.
        BigDecimal total1 = BigDecimal.ZERO;
        for (Object aBalance1 : balance) {
            total1 = total1.add((BigDecimal) aBalance1);
        }
        pl.add("------------------------------------------------------");
        pl.add(formatAcctNameOut(rb.getString("Word.GrossIncome")) + " " + formatDecimalOut(total1));
        pl.add("------------------------------------------------------");
        pl.add("");
        pl.add("");

        //Expense
        pl.add("Expenses");
        pl.add("------------------------------------------------------");
        balance = new ArrayList<>();
        getBalances(root, dates, AccountType.EXPENSE);

        //Add up the Gross Expenses
        BigDecimal total2 = BigDecimal.ZERO;
        for (Object aBalance : balance) {
            total2 = total2.add((BigDecimal) aBalance);
        }
        pl.add("------------------------------------------------------");
        pl.add(formatAcctNameOut(rb.getString("Word.GrossExpense")) + " " + formatDecimalOut(total2));
        pl.add("------------------------------------------------------");
        pl.add("");
        pl.add("");

        //Net Total
        pl.add("------------------------------------------------------");
        pl.add(formatAcctNameOut(rb.getString("Word.NetIncome")) + " " + formatDecimalOut(total1.add(total2)));
        pl.add("======================================================");

    }

    void writePLFile(final String fileName) {
        if (fileName == null || dates == null) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))) {            
            for (Object aPl : pl) {  //write the array list pl to the file
                //print (pl.get(i));
                writer.write(aPl.toString());
                writer.newLine();
            }
            writer.newLine();            
        } catch (IOException e) {
            Logger.getLogger(ProfitLossTXT.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }         
    }

    private static Date[] getLastDays(final Date start, final Date stop) {
        ArrayList<Date> list = new ArrayList<>();

        Date s = DateUtils.trimDate(start);
        Date t = DateUtils.trimDate(stop);
        list.add(s);
        list.add(t);
        return list.toArray(new Date[list.size()]);
    }

    Date[] getDates() {

        Date start = new Date();
        start = DateUtils.subtractYear(start);

        JDateField startField = new JDateField();
        JDateField endField = new JDateField();

        startField.setValue(start);

        FormLayout layout = new FormLayout("right:p, 4dlu, p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.rowGroupingEnabled(true);

        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();

        JPanel panel = builder.getPanel();

        int option = JOptionPane.showConfirmDialog(null, new Object[] { panel }, rb.getString("Message.StartEndDate"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            return getLastDays(startField.dateValue(), endField.dateValue());
        }

        return null;
    }

    String getFileName() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Message.TXTFile"), "txt"));

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (!fileName.endsWith(".txt")) {
                fileName = fileName + ".txt";
            }
            return fileName;
        }
        return null;
    }

    /**
     * format output decimal amount
     * 
     * @param amt the BigDecimal value to format
     * @return formated string
     */
    private String formatDecimalOut(final BigDecimal amt) {

        int maxLen = 15; //(-000,000,000.00)
        StringBuilder sb = new StringBuilder();

        String formattedAmt = numberFormat.format(amt);

        // right align amount to pre-defined maximum length (maxLen)
        int amtLen = formattedAmt.length();
        if (amtLen < maxLen) {
            for (int ix = amtLen; ix < maxLen; ix++) {
                sb.append(' ');
            }
        }

        sb.append(formattedAmt);

        return sb.toString();
    }

    /**
     * format output account name
     * 
     * @param acctName the account name to format
     * @return the formatted account name
     */
    private static String formatAcctNameOut(final String acctName) {

        int maxLen = 30; // max 30 characters
        StringBuilder sb = new StringBuilder(maxLen);

        sb.append(acctName);

        // set name to pre-defined maximum length (maxLen)
        int nameLen = acctName.length();
        for (int ix = nameLen; ix < maxLen; ix++) {
            sb.append(' ');
        }
        sb.setLength(maxLen);

        return sb.toString();
    }

    void getBalances(final Account a, final Date[] dates1, final AccountType type) {

        for (Account child : a.getChildren()) {
            int len = child.getTransactionCount();
            if ((SHOW_EMPTY_ACCOUNT || len > 0) && type == child.getAccountType()) {
                String acctName = child.getName();

                BigDecimal acctBal = AccountBalanceDisplayManager.convertToSelectedBalanceMode(child.getAccountType(), child.getBalance(dates1[0], dates1[1], baseCommodity));

                // output account name and balance
                pl.add(formatAcctNameOut(acctName) + " " + formatDecimalOut(acctBal));

                balance.add(acctBal);
            }
            if (child.isParent()) {
                getBalances(child, dates1, type);
            }
        }
    }
}
