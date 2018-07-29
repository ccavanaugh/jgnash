/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.ui.actions;

import jgnash.convert.in.DateFormat;
import jgnash.convert.in.qif.NoAccountException;
import jgnash.convert.in.qif.QifImport;
import jgnash.convert.in.qif.QifUtils;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.MultiLineLabel;
import jgnash.ui.components.YesNoDialog;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.builder.Action;
import jgnash.ui.wizards.imports.qif.PartialDialog;
import jgnash.util.ResourceUtils;
import jgnash.util.TextResource;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;

/**
 * @author Craig Cavanaugh
 */
@Action("qifimport-command")
public class ImportQifAction extends AbstractEnabledAction {

    private static final String QIFDIR = "QifDirectory";

    private static final boolean debug = true;

    public ImportQifAction() {
    }

    private static void importQif() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(ImportQifAction.class);

        final Logger logger = Logger.getLogger("qifimport");

        if (debug) {
            try {
                Handler fh = new FileHandler("%h/jgnash%g.log");
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
                logger.setLevel(Level.FINEST);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Could not install file handler", ioe);              
            }
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (engine.getRootAccount() == null) {
            StaticUIMethods.displayError(rb.getString("Message.Error.CreateBasicAccounts"));
            return;
        }

        final JFileChooser chooser = new JFileChooser(pref.get(QIFDIR, null));
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Qif Files (*.qif)", "qif"));

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            pref.put(QIFDIR, chooser.getCurrentDirectory().getAbsolutePath());

            boolean fullFile = QifUtils.isFullFile(chooser.getSelectedFile());

            if (fullFile) {

                // prompt for date format
                final DateFormat dateFormat = getQIFDateFormat();

                class ImportFile extends SwingWorker<Void, Void> {

                    @Override
                    protected Void doInBackground() {
                        UIApplication.getFrame().displayWaitMessage(rb.getString("Message.ImportWait"));
                        QifImport imp = new QifImport();

                        try {
                            imp.doFullParse(chooser.getSelectedFile(), dateFormat);
                        } catch (NoAccountException e) {                           
                            logger.log(Level.SEVERE, "Mistook partial qif file as a full qif file", e);
                        }
                        imp.dumpStats();
                        imp.doFullImport();
                        if (imp.getDuplicateCount() > 0) {
                            String message = imp.getDuplicateCount() + " duplicate transactions were found";
                            logger.info(message);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        UIApplication.getFrame().stopWaitMessage();
                    }
                }

                new ImportFile().execute();
            } else {
                final QifImport imp = new QifImport();

                if (!imp.doPartialParse(chooser.getSelectedFile())) {
                    StaticUIMethods.displayError(rb.getString("Message.Error.ParseTransactions"));
                    return;
                }

                imp.dumpStats();

                if (imp.getParser().accountList.isEmpty()) {
                    StaticUIMethods.displayError(rb.getString("Message.Error.ParseTransactions"));
                    return;
                }
                PartialDialog dlg = new PartialDialog(imp.getParser());
                DialogUtils.addBoundsListener(dlg);
                dlg.setVisible(true);

                if (dlg.isWizardValid()) {
                    imp.doPartialImport(dlg.getAccount());
                    if (imp.getDuplicateCount() > 0) {
                        if (YesNoDialog.showYesNoDialog(UIApplication.getFrame(), new MultiLineLabel(TextResource.getString("DupeTransImport.txt")), rb.getString("Title.DuplicateTransactionsFound"), YesNoDialog.WARNING_MESSAGE)) {
                            Transaction[] t = imp.getDuplicates();
                            for (Transaction element : t) {
                                engine.addTransaction(element);
                            }
                        }
                    }
                }
            }
        }
    }

    private static DateFormat getQIFDateFormat() {

        String DATE_FORMAT = "dateFormat";

        DateFormat dateFormat = DateFormat.US;

        ResourceBundle rb = ResourceUtils.getBundle();
        Preferences pref = Preferences.userNodeForPackage(ImportQifAction.class);

        /* Create the combo for date format selection */
        JComboBox<DateFormat> combo = new JComboBox<>(DateFormat.values());

        combo.setSelectedIndex(pref.getInt(DATE_FORMAT, 0));

        Object[] options = { rb.getString("Button.Ok"), rb.getString("Button.Cancel") };
        int result = JOptionPane.showOptionDialog(UIApplication.getFrame(), combo, rb.getString("Title.SelQifDateFormat"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);

        if (result == JOptionPane.YES_OPTION) {
            dateFormat = (DateFormat) combo.getSelectedItem();
        }

        pref.putInt(DATE_FORMAT, combo.getSelectedIndex());

        return dateFormat;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        importQif();
    }
}
