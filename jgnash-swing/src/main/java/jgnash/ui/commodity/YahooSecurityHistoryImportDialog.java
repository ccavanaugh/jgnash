/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.security.SecurityUpdateFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.components.CheckListCellRenderer;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.ToggleSelectionModel;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import static jgnash.util.Arrays.intListToArray;

/**
 * Dialog that lets the user download and import security history from Yahoo
 * 
 * @author Craig Cavanaugh
 */
public class YahooSecurityHistoryImportDialog extends JDialog implements ActionListener {
    private final Resource rb = Resource.get();

    private final DatePanel startField = new DatePanel();

    private final DatePanel endField = new DatePanel();

    private final JButton okButton = new JButton(rb.getString("Button.Ok"));

    private final JButton cancelButton = new JButton(rb.getString("Button.Cancel"));
    
    private final JButton selectAllButton = new JButton(rb.getString("Button.SelectAll"));
    
    private final JButton invertAllButton = new JButton(rb.getString("Button.InvertSelection"));
    
    private final JButton clearAllButton = new JButton(rb.getString("Button.ClearAll"));

    private final JProgressBar bar = new JProgressBar();

    private final JList<SecurityNode> securityList = new JList<>();

    private final Calendar cal = Calendar.getInstance();

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private ImportRun run;

    /**
     * Creates the dialog for importing security history from Yahoo
     */
    public YahooSecurityHistoryImportDialog() {
        super();
        setTitle(rb.getString("Title.HistoryImport"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));

        cal.setTime(new Date());
        cal.add(Calendar.MONDAY, -1);
        startField.setDate(cal.getTime());

        List<SecurityNode> list = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurities();

        Iterator<SecurityNode> i = list.iterator();

        while (i.hasNext()) {
            if (i.next().getQuoteSource() == QuoteSource.NONE) {
                i.remove();
            }
        }

        securityList.setModel(new SortedListModel<>(list));
        securityList.setSelectionModel(new ToggleSelectionModel());
        securityList.setCellRenderer(new CheckListCellRenderer<>(securityList.getCellRenderer()));

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
    void closeDialog() {
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

        Date start = startField.getDate();
        Date end = endField.getDate();

        int[] list = securityList.getSelectedIndices();
        SecurityNode[] nodes = new SecurityNode[list.length];

        for (int i = 0; i < list.length; i++) {
            nodes[i] = securityList.getModel().getElementAt(list[i]);
        }

        // create the runnable and start the thread
        run = new ImportRun(start, end, nodes);
        new Thread(run, "doImport").start();
    }

    /**
     * Invoked when an action occurs.
     * 
     * @param e
     *            action event
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

        public static final String RESPONSE_HEADER = "Date,Open,High,Low,Close,Volume,Adj Close";

        private volatile Object lock = new Object();

        private Date start;

        private Date end;

        private final SecurityNode[] sNodes;

        private String a;

        private String b;

        private String c;

        private String d;

        private String e;

        private String f;

        ImportRun(final Date start, final Date end, final SecurityNode[] sNodes) {
            this.start = start;
            this.end = end;
            this.sNodes = sNodes;
        }

        // TODO Migrate this method into the core for reuse
        private void parse(final SecurityNode sNode) {
            String s = sNode.getSymbol().toLowerCase();

            // http://ichart.finance.yahoo.com/table.csv?s=AMD&d=1&e=14&f=2007&g=d&a=2&b=21&c=1983&ignore=.csv << new URL 2.14.07

            StringBuilder r = new StringBuilder("http://ichart.finance.yahoo.com/table.csv?a=");
            r.append(a).append("&b=").append(b).append("&c=").append(c);
            r.append("&d=").append(d).append("&e=").append(e);
            r.append("&f=").append(f).append("&s=").append(s);
            r.append("&y=0&g=d&ignore=.csv");

            URLConnection connection = null;           

            try {
                /* Yahoo uses English locale for date format... force the locale */
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                
                connection = new URL(r.toString()).openConnection();

                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                    String l = in.readLine();

                    // make sure that we have valid data format.
                    if (!RESPONSE_HEADER.equals(l)) {
                        closeDialog();
                        return;
                    }

                    //Date,Open,High,Low,Close,Volume,Adj Close
                    //2007-02-13,14.75,14.86,14.47,14.60,17824500,14.60

                    l = in.readLine();
                    while (l != null && lock != null) {
                        
                        if (l.charAt(0) != '<') { // may have comments in file                                                    
                            String[] fields = COMMA_DELIMITER_PATTERN.split(l);
                            Date date = df.parse(fields[0]);
                            BigDecimal high = new BigDecimal(fields[2]);
                            BigDecimal low = new BigDecimal(fields[3]);
                            BigDecimal close = new BigDecimal(fields[4]);
                            long volume = Long.parseLong(fields[5]);

                            SecurityHistoryNode node = new SecurityHistoryNode();
                            node.setDate(date);
                            node.setPrice(close);
                            node.setVolume(volume);
                            node.setHigh(high);
                            node.setLow(low);

                            EngineFactory.getEngine(EngineFactory.DEFAULT).addSecurityHistory(sNode, node);
                        }

                        l = in.readLine();
                    }
                }               

                String message = rb.getString("Message.UpdatedPrice", sNode.getSymbol());
                Logger.getLogger(SecurityUpdateFactory.class.getName()).info(message);

            } catch (IOException | ParseException | NumberFormatException ex) {
                Logger.getLogger(YahooSecurityHistoryImportDialog.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (connection != null) {
                    if (connection instanceof HttpURLConnection) {
                        ((HttpURLConnection) connection).disconnect();                        
                    }
                }
            }
        }

        @Override
        public void run() {
            // Mistake proof and handle reversed dates gracefully
            if (!DateUtils.before(start, end)) {
                Date t = start;
                start = end;
                end = t;
            }

            cal.setTime(start);
            a = Integer.toString(cal.get(Calendar.MONTH));
            b = Integer.toString(cal.get(Calendar.DATE));
            c = Integer.toString(cal.get(Calendar.YEAR));

            cal.setTime(end);
            d = Integer.toString(cal.get(Calendar.MONTH));
            e = Integer.toString(cal.get(Calendar.DATE));
            f = Integer.toString(cal.get(Calendar.YEAR));

            // run the import in sequence
            for (SecurityNode node : sNodes) {
                if (lock != null) { // continue?
                    parse(node);
                }
            }
            closeDialog();
        }

        void stop() {
            lock = null;
        }
    }
}