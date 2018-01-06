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
package jgnash.ui.commodity;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityNode;
import jgnash.net.security.UpdateFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.components.CheckListCellRenderer;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.IconUtils;
import jgnash.ui.util.ToggleSelectionModel;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import static jgnash.util.CollectionUtils.intListToArray;

/**
 * Dialog that lets the user download and import security history from Yahoo
 *
 * @author Craig Cavanaugh
 */
public class YahooSecurityHistoryImportDialog extends JDialog implements ActionListener {
    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final DatePanel startField = new DatePanel();

    private final DatePanel endField = new DatePanel();

    private final JButton okButton = new JButton(rb.getString("Button.Ok"));

    private final JButton cancelButton = new JButton(rb.getString("Button.Cancel"));

    private final JButton selectAllButton = new JButton(rb.getString("Button.SelectAll"));

    private final JButton invertAllButton = new JButton(rb.getString("Button.InvertSelection"));

    private final JButton clearAllButton = new JButton(rb.getString("Button.ClearAll"));

    private final JProgressBar bar = new JProgressBar();

    private final JList<SecurityNode> securityList = new JList<>();

    private ImportRun run;

    /**
     * Creates the dialog for importing security history from Yahoo
     */
    public YahooSecurityHistoryImportDialog() {
        super();
        setTitle(rb.getString("Title.HistoryImport"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setIconImage(IconUtils.getImage("/jgnash/resource/gnome-money.png"));

        startField.setDate(LocalDate.now().minusMonths(1));

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            List<SecurityNode> list = engine.getSecurities();

            list.removeIf(securityNode -> securityNode.getQuoteSource() == QuoteSource.NONE);

            securityList.setModel(new SortedListModel<>(list));
            securityList.setSelectionModel(new ToggleSelectionModel());
            securityList.setCellRenderer(new CheckListCellRenderer<>(securityList.getCellRenderer()));
        }


        layoutMainPanel();

        setMinimumSize(getSize());

        registerListeners();
    }

    private void registerListeners() {
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        selectAllButton.addActionListener(this);
        clearAllButton.addActionListener(this);
        invertAllButton.addActionListener(this);

        DialogUtils.addBoundsListener(this);
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("r:p, $lcgap, 48dlu:g, $ugap, r:p, $lcgap, 48dlu:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        JScrollPane scrollPane = new JScrollPane(securityList);
        scrollPane.setAutoscrolls(true);

        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildLeftAlignedBar(selectAllButton, clearAllButton, invertAllButton), 7);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow("f:p:g");
        builder.append(rb.getString("Label.Security"), scrollPane, 5);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(bar, 7);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 7);

        getContentPane().add(builder.getPanel());
        pack();
    }

    private void doImport() {
        bar.setIndeterminate(true);
        okButton.setEnabled(false); // do not allow another start

        final LocalDate start = startField.getLocalDate();
        final LocalDate end = endField.getLocalDate();

        int[] list = securityList.getSelectedIndices();
        SecurityNode[] nodes = new SecurityNode[list.length];

        for (int i = 0; i < list.length; i++) {
            nodes[i] = securityList.getModel().getElementAt(list[i]);
        }

        // create the runnable and start the thread
        run = new ImportRun(nodes, start, end);
        new Thread(run, "doImport").start();
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            doImport();
        } else if (e.getSource() == cancelButton) {
            if (run != null) {
                run.stop();
            }
            closeDialog();
        } else if (e.getSource() == selectAllButton) {
            securityList.setSelectionInterval(0, securityList.getModel().getSize() - 1);
        } else if (e.getSource() == clearAllButton) {
            securityList.clearSelection();
        } else if (e.getSource() == invertAllButton) {
            invertSelection();
        }
    }

    private void invertSelection() {
        int selected[] = securityList.getSelectedIndices();

        List<Integer> invertedSelectionIndices = new ArrayList<>();

        int count = securityList.getModel().getSize();

        for (int i = 0; i < count; i++) {
            if (Arrays.binarySearch(selected, i) < 0) {
                invertedSelectionIndices.add(i);
            }
        }

        securityList.setSelectedIndices(intListToArray(invertedSelectionIndices));
    }

    /**
     * This class does all the work for importing the data
     */
    private class ImportRun implements Runnable {

        private volatile boolean run = true;

        private final LocalDate start;

        private final LocalDate end;

        private final SecurityNode[] sNodes;

        ImportRun(final SecurityNode[] sNodes, final LocalDate start, final LocalDate end) {
            this.start = start;
            this.end = end;
            this.sNodes = sNodes;
        }

        @Override
        public void run() {

            try {
                // run the import in sequence
                for (final SecurityNode node : sNodes) {
                    if (run) { // continue?
                        if (!UpdateFactory.importHistory(node, start, end)) {
                            StaticUIMethods.displayWarning(ResourceUtils.getString("Message.Error.SecurityUpdate", node.getSymbol()));
                        }
                    }
                }
            } finally {
                closeDialog();
            }
        }

        void stop() {
            run = false;
        }
    }
}