/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

package jgnash.ui.report.jasper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import jgnash.ui.UIApplication;
import jgnash.util.Resource;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporterParameter;
import net.sf.jasperreports.view.JRSaveContributor;
import net.sf.jasperreports.view.save.JRCsvSaveContributor;
import net.sf.jasperreports.view.save.JROdtSaveContributor;
import net.sf.jasperreports.view.save.JRPdfSaveContributor;
import net.sf.jasperreports.view.save.JRSingleSheetXlsSaveContributor;

/**
 * Panel to display reports created using DynamicJasper
 * 
 * @author Craig Cavanaugh
 *
 */
class DynamicJasperReportPanel extends JPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(DynamicJasperReportPanel.class.getName());

    /**
     * The DPI of the generated report.
     */
    private static final int REPORT_RESOLUTION = 72;

    private static final String NET_SF_JASPERREPORTS_VIEW_VIEWER = "net/sf/jasperreports/view/viewer";

    private static final float MIN_ZOOM = 0.5f;

    private static final float MAX_ZOOM = 10f;

    private static final int ZOOM[] = { 50, 75, 100, 125, 150, 175, 200 };

    private static final int DEFAULT_ZOOM_INDEX = 2;

    private static final String LAST_DIRECTORY = "lastDirectory";

    private static final String LAST_CONTRIBUTOR = "lastContributor";

    private JasperPrint jPrint = null;

    private int pageIndex = 0;

    private float zoom = 0f;

    private JRGraphics2DExporter exporter = null;

    /**
     * the screen resolution.
     */
    private int screenResolution = REPORT_RESOLUTION;

    /**
     * the zoom ratio adjusted to the screen resolution.
     */
    private float realZoom = 0f;

    private DecimalFormat zoomFormat = new DecimalFormat("#.#");

    private ResourceBundle resourceBundle = null;

    protected JToggleButton actualSizeButton;

    protected JButton firstButton;

    protected JToggleButton fitPageButton;

    protected JToggleButton fitWidthButton;

    protected JButton lastButton;

    protected JButton nextButton;

    private JButton pageSetupButton;

    protected JButton previousButton;

    protected JButton printButton;

    protected JButton saveButton;

    protected JButton zoomInButton;

    protected JButton zoomOutButton;

    protected JButton helpButton;

    protected JComboBox<String> zoomComboBox;

    protected JComboBox<Integer> fontSizeComboBox;

    private JPanel scrollPanePanel;

    private JPanel spaceHoldPanel;

    private JPanel mainPanel;

    private JPanel pageGluePanel;

    private List<JRSaveContributor> saveContributors = new ArrayList<>();

    private DynamicJasperReport report;

    private PageRenderer pageRenderer;

    // reference to the parent frame to control wait message
    private DynamicJasperReportFrame frame;

    protected DynamicJasperReportPanel(final DynamicJasperReportFrame frame, final DynamicJasperReport report) {

        screenResolution = Toolkit.getDefaultToolkit().getScreenResolution();
        resourceBundle = ResourceBundle.getBundle(NET_SF_JASPERREPORTS_VIEW_VIEWER);

        this.report = report;
        this.frame = frame;

       setViewer();

        initializeUI();

        initSaveContributors();

        new ReportWorker().execute();
    }
    
    private void setViewer() {
       report.setViewer(this); // link the report to this viewer 
    }

    void clear() {
        emptyContainer(this);
        jPrint = null;
    }

    private String getBundleString(String key) {
        return resourceBundle.getString(key);
    }

    private void initSaveContributors() {
        saveContributors.add(new JRPdfSaveContributor(Locale.getDefault(), resourceBundle));
        saveContributors.add(new JROdtSaveContributor(Locale.getDefault(), resourceBundle));
        saveContributors.add(new JRCsvSaveContributor(Locale.getDefault(), resourceBundle));
        saveContributors.add(new JRSingleSheetXlsSaveContributor(Locale.getDefault(), resourceBundle));           
    }

    private void initializeUI() {
        GridBagConstraints gridBagConstraints;

        JToolBar toolBar = new JToolBar();

        firstButton = new JButton();
        previousButton = new JButton();
        nextButton = new JButton();
        lastButton = new JButton();

        actualSizeButton = new JToggleButton();
        fitPageButton = new JToggleButton();
        fitWidthButton = new JToggleButton();

        zoomInButton = new JButton();

        DefaultComboBoxModel<String> zoomModel = new DefaultComboBoxModel<>();
        for (int z : ZOOM) {
            zoomModel.addElement(z + "%");
        }
        zoomComboBox = new JComboBox<>(zoomModel);

        DefaultComboBoxModel<Integer> fontModel = new DefaultComboBoxModel<>();
        for (int i = 0; i <= 10; i++) {
            fontModel.addElement(5 + i);
        }
        fontSizeComboBox = new JComboBox<>(fontModel);
        fontSizeComboBox.setToolTipText(Resource.get().getString("ToolTip.FontSize"));
        fontSizeComboBox.setSelectedItem(report.getBaseFontSize());
        fontSizeComboBox.addActionListener(this);

        zoomOutButton = new JButton();
        mainPanel = new JPanel();

        JScrollPane scrollPane = new JScrollPane();

        scrollPanePanel = new JPanel();
        pageGluePanel = new JPanel();
        JPanel pagePanel = new JPanel();

        JPanel shadowPanel1 = new JPanel();
        JPanel shadowPanel2 = new JPanel();
        JPanel shadowPanel3 = new JPanel();
        JPanel shadowPanel4 = new JPanel();
        JPanel shadowPanel5 = new JPanel();

        setLayout(new BorderLayout());

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        saveButton = new JButton(new ImageIcon(getClass().getResource("/jgnash/resource/document-save-as.png")));
        saveButton.setToolTipText(getBundleString("save"));
        saveButton.setFocusable(false);
        saveButton.addActionListener(this);
        toolBar.add(saveButton);

        printButton = new JButton(new ImageIcon(getClass().getResource("/jgnash/resource/document-print.png")));
        printButton.setToolTipText(getBundleString("print"));
        printButton.setFocusable(false);
        printButton.addActionListener(this);
        toolBar.add(printButton);

        toolBar.add(new JToolBar.Separator());

        pageSetupButton = new JButton(new ImageIcon(getClass().getResource("/jgnash/resource/document-properties.png")));
        pageSetupButton.setToolTipText(Resource.get().getString("ToolTip.PageSetup"));
        pageSetupButton.setFocusable(false);
        pageSetupButton.addActionListener(this);
        toolBar.add(pageSetupButton);

        fontSizeComboBox.setMaximumSize(new Dimension(50, pageSetupButton.getMinimumSize().height));
        fontSizeComboBox.setMinimumSize(new Dimension(50, pageSetupButton.getMinimumSize().height));
        fontSizeComboBox.setPreferredSize(new Dimension(50, pageSetupButton.getPreferredSize().height));
        toolBar.add(fontSizeComboBox);

        toolBar.add(new JToolBar.Separator());

        firstButton.setIcon(new ImageIcon(getClass().getResource("/jgnash/resource/go-first.png")));
        firstButton.setToolTipText(getBundleString("first.page"));
        firstButton.setFocusable(false);
        firstButton.setEnabled(false);
        firstButton.addActionListener(this);
        toolBar.add(firstButton);

        previousButton.setIcon(new ImageIcon(getClass().getResource("/jgnash/resource/go-previous.png")));
        previousButton.setToolTipText(getBundleString("previous.page"));
        previousButton.setFocusable(false);
        previousButton.setEnabled(false);
        previousButton.addActionListener(this);
        toolBar.add(previousButton);

        nextButton.setIcon(new ImageIcon(getClass().getResource("/jgnash/resource/go-next.png")));
        nextButton.setToolTipText(getBundleString("next.page"));
        nextButton.setFocusable(false);
        nextButton.setEnabled(false);
        nextButton.addActionListener(this);
        toolBar.add(nextButton);

        lastButton.setIcon(new ImageIcon(getClass().getResource("/jgnash/resource/go-last.png")));
        lastButton.setToolTipText(getBundleString("last.page"));
        lastButton.setFocusable(false);
        lastButton.setEnabled(false);
        lastButton.addActionListener(this);
        toolBar.add(lastButton);

        toolBar.add(new JToolBar.Separator());

        actualSizeButton.setIcon(new ImageIcon(getClass().getResource("/net/sf/jasperreports/view/images/actualsize.GIF")));
        actualSizeButton.setToolTipText(getBundleString("actual.size"));
        actualSizeButton.setFocusable(false);
        actualSizeButton.addActionListener(this);
        toolBar.add(actualSizeButton);

        fitPageButton.setIcon(new ImageIcon(getClass().getResource("/net/sf/jasperreports/view/images/fitpage.GIF")));
        fitPageButton.setToolTipText(getBundleString("fit.page"));
        fitPageButton.setFocusable(false);
        fitPageButton.addActionListener(this);
        toolBar.add(fitPageButton);

        fitWidthButton.setIcon(new ImageIcon(getClass().getResource("/net/sf/jasperreports/view/images/fitwidth.GIF")));
        fitWidthButton.setToolTipText(getBundleString("fit.width"));
        fitWidthButton.setFocusable(false);
        fitWidthButton.addActionListener(this);
        toolBar.add(fitWidthButton);

        toolBar.add(new JToolBar.Separator());

        zoomInButton.setIcon(new ImageIcon(getClass().getResource("/jgnash/resource/zoom-in.png")));
        zoomInButton.setToolTipText(getBundleString("zoom.in"));
        zoomInButton.setFocusable(false);
        zoomInButton.addActionListener(this);
        toolBar.add(zoomInButton);

        zoomComboBox.setEditable(true);
        zoomComboBox.setToolTipText(getBundleString("zoom.ratio"));
        zoomComboBox.setMaximumSize(new Dimension(90, zoomInButton.getMinimumSize().height));
        zoomComboBox.setMinimumSize(new Dimension(90, zoomInButton.getMinimumSize().height));
        zoomComboBox.setPreferredSize(new Dimension(90, zoomInButton.getPreferredSize().height));
        zoomComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent evt) {
                zoomStateChange();
            }
        });
        zoomComboBox.addActionListener(this);
        toolBar.add(zoomComboBox);

        zoomOutButton.setIcon(new ImageIcon(getClass().getResource("/jgnash/resource/zoom-out.png")));
        zoomOutButton.setToolTipText(getBundleString("zoom.out"));
        zoomOutButton.setFocusable(false);
        zoomOutButton.addActionListener(this);
        toolBar.add(zoomOutButton);

        helpButton = new JButton(new ImageIcon(getClass().getResource("/jgnash/resource/help-browser.png")));
        helpButton.setToolTipText(Resource.get().getString("ToolTip.Help"));
        helpButton.setFocusable(false);
        helpButton.addActionListener(this);

        toolBar.add(new JToolBar.Separator());
        toolBar.add(helpButton);

        JPanel reportController = report.getReportController();

        if (reportController != null) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(toolBar, BorderLayout.NORTH);
            panel.add(reportController, BorderLayout.CENTER);
            add(panel, BorderLayout.NORTH);
        } else {
            add(toolBar, BorderLayout.NORTH);
        }

        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(final ComponentEvent evt) {
                pnlMainComponentResized();
            }
        });
        mainPanel.setLayout(new BorderLayout());

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        scrollPanePanel.setLayout(new GridBagLayout());

        pageGluePanel.setLayout(new BorderLayout());

        pagePanel.setLayout(new GridBagLayout());

        spaceHoldPanel = new JPanel();
        spaceHoldPanel.setMinimumSize(new Dimension(5, 5));
        spaceHoldPanel.setOpaque(false);
        spaceHoldPanel.setPreferredSize(new Dimension(5, 5));
        spaceHoldPanel.setLayout(null);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        pagePanel.add(spaceHoldPanel, gridBagConstraints);

        shadowPanel1.setBackground(java.awt.Color.gray);
        shadowPanel1.setMinimumSize(new Dimension(5, 5));
        shadowPanel1.setPreferredSize(new Dimension(5, 5));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        pagePanel.add(shadowPanel1, gridBagConstraints);

        shadowPanel2.setMinimumSize(new Dimension(5, 5));
        shadowPanel2.setPreferredSize(new Dimension(5, 5));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        pagePanel.add(shadowPanel2, gridBagConstraints);

        shadowPanel3.setBackground(Color.gray);
        shadowPanel3.setMinimumSize(new Dimension(5, 5));
        shadowPanel3.setPreferredSize(new Dimension(5, 5));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        pagePanel.add(shadowPanel3, gridBagConstraints);

        shadowPanel4.setBackground(Color.gray);
        shadowPanel4.setMinimumSize(new Dimension(5, 5));
        shadowPanel4.setPreferredSize(new Dimension(5, 5));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        pagePanel.add(shadowPanel4, gridBagConstraints);

        shadowPanel5.setMinimumSize(new Dimension(5, 5));
        shadowPanel5.setPreferredSize(new Dimension(5, 5));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        pagePanel.add(shadowPanel5, gridBagConstraints);

        pageRenderer = new PageRenderer(this);
        pageRenderer.setBackground(Color.white);
        pageRenderer.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
        pageRenderer.setOpaque(true);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pagePanel.add(pageRenderer, gridBagConstraints);

        pageGluePanel.add(pagePanel, BorderLayout.CENTER);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        scrollPanePanel.add(pageGluePanel, gridBagConstraints);

        scrollPane.setViewportView(scrollPanePanel);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        zoomComboBox.setSelectedIndex(DEFAULT_ZOOM_INDEX);
    }

    private void zoomStateChange() {
        actualSizeButton.setSelected(false);
        fitPageButton.setSelected(false);
        fitWidthButton.setSelected(false);
    }

    private void pnlMainComponentResized() {
        if (fitPageButton.isSelected()) {
            fitPage();
            fitPageButton.setSelected(true);
        } else if (fitWidthButton.isSelected()) {
            setRealZoomRatio(((float) scrollPanePanel.getVisibleRect().getWidth() - 20f) / jPrint.getPageWidth());
            fitWidthButton.setSelected(true);
        }
    }

    private void actualSizeAction() {

        if (actualSizeButton.isSelected()) {
            fitPageButton.setSelected(false);
            fitWidthButton.setSelected(false);
            zoomComboBox.setSelectedIndex(-1);
            setZoomRatio(1);
            actualSizeButton.setSelected(true);
        }
    }

    private void fitWidthAction() {
        if (fitWidthButton.isSelected()) {
            actualSizeButton.setSelected(false);
            fitPageButton.setSelected(false);
            zoomComboBox.setSelectedIndex(-1);
            setRealZoomRatio(((float) scrollPanePanel.getVisibleRect().getWidth() - 20f) / jPrint.getPageWidth());
            fitWidthButton.setSelected(true);
        }
    }

    private void fitPageAction() {
        if (fitPageButton.isSelected()) {
            actualSizeButton.setSelected(false);
            fitWidthButton.setSelected(false);
            zoomComboBox.setSelectedIndex(-1);
            fitPage();
            fitPageButton.setSelected(true);
        }
    }

    private void saveAction() {
        Preferences p = Preferences.userNodeForPackage(DynamicJasperReportPanel.class);

        JFileChooser fileChooser = new JFileChooser();

        for (JRSaveContributor saveContributor : saveContributors) {
            fileChooser.addChoosableFileFilter(saveContributor);
        }

        // restore the last save format
        if (p.get(LAST_CONTRIBUTOR, null) != null) {
            String last = p.get(LAST_CONTRIBUTOR, null);

            for (JRSaveContributor saveContributor : saveContributors) {
                if (saveContributor.getDescription().equals(last)) {
                    fileChooser.setFileFilter(saveContributor);
                    break;
                }
            }
        } else if (!saveContributors.isEmpty()) {
            fileChooser.setFileFilter(saveContributors.get(0));
        }

        if (p.get(LAST_DIRECTORY, null) != null) {
            fileChooser.setCurrentDirectory(new File(p.get(LAST_DIRECTORY, null)));
        }

        int retValue = fileChooser.showSaveDialog(this);

        if (retValue == JFileChooser.APPROVE_OPTION) {
            FileFilter fileFilter = fileChooser.getFileFilter();
            File file = fileChooser.getSelectedFile();

            p.put(LAST_DIRECTORY, file.getParent());

            JRSaveContributor contributor = null;

            if (fileFilter instanceof JRSaveContributor) { // save format chosen from the list
                contributor = (JRSaveContributor) fileFilter;
            } else {
                for (JRSaveContributor saveContributor : saveContributors) { // need to determine the best match
                    if (saveContributor.accept(file)) {
                        contributor = saveContributor;
                        break;
                    }
                }

                if (contributor == null) {
                    JOptionPane.showMessageDialog(this, getBundleString("error.saving"));
                }
            }

            if (contributor != null) {
                p.put(LAST_CONTRIBUTOR, contributor.getDescription());

                try {
                    
                    if (contributor instanceof JRSingleSheetXlsSaveContributor) {
                        logger.info("Formatting for xls file");
                        JasperPrint print = report.createJasperPrint(true);
                        contributor.save(print, file);
                    } else if (contributor instanceof JRCsvSaveContributor) {
                        logger.info("Formatting for csv file");
                        JasperPrint print = report.createJasperPrint(true);
                        contributor.save(print, file);
                    } else {
                        contributor.save(jPrint, file);
                    }

                } catch (JRException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(this, getBundleString("error.saving"));
                }
            }
        }
    }

    private void printAction() {

        class PrintWorker extends SwingWorker<Void, Void> {

            @Override
            public Void doInBackground() {

                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            JasperPrintManager.printReport(jPrint, true);
                        } catch (JRException ex) {
                            logger.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    }
                });

                return null;
            }

            @Override
            protected void done() {
                try {
                    DynamicJasperReportPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    printButton.setEnabled(true);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                printButton.setEnabled(false);
                DynamicJasperReportPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                new PrintWorker().execute();
            }
        });
    }

    private void lastPageAction() {
        setPageIndex(jPrint.getPages().size() - 1);
        refreshPage();
    }

    private void nextPageAction() {
        setPageIndex(pageIndex + 1);
        refreshPage();
    }

    private void previousPageAction() {
        setPageIndex(pageIndex - 1);
        refreshPage();
    }

    private void firstPageAction() {
        setPageIndex(0);
        refreshPage();
    }

    private void forceRefresh() {
        if (jPrint != null) {
            refreshPage();
            frame.setReportName(report.getReportName());
            return;
        }

        zoom = 0; //force pageRefresh()
        realZoom = 0f;
        setZoomRatio(1);
    }

    private void zoomInAction() {
        actualSizeButton.setSelected(false);
        fitPageButton.setSelected(false);
        fitWidthButton.setSelected(false);

        int newZoomInt = (int) (100 * getZoomRatio());
        int index = Arrays.binarySearch(ZOOM, newZoomInt);
        if (index < 0) {
            setZoomRatio(ZOOM[-index - 1] / 100f);
        } else if (index < zoomComboBox.getModel().getSize() - 1) {
            setZoomRatio(ZOOM[index + 1] / 100f);
        }
    }

    private void zoomOutAction() {
        actualSizeButton.setSelected(false);
        fitPageButton.setSelected(false);
        fitWidthButton.setSelected(false);

        int newZoomInt = (int) (100 * getZoomRatio());
        int index = Arrays.binarySearch(ZOOM, newZoomInt);
        if (index > 0) {
            setZoomRatio(ZOOM[index - 1] / 100f);
        } else if (index < -1) {
            setZoomRatio(ZOOM[-index - 2] / 100f);
        }
    }

    private void zoomAction() {

        float newZoom = getZoomRatio();

        if (newZoom < MIN_ZOOM) {
            newZoom = MIN_ZOOM;
        }

        if (newZoom > MAX_ZOOM) {
            newZoom = MAX_ZOOM;
        }

        setZoomRatio(newZoom);
    }

    private void pageSetupAction() {
        PageFormat oldFormat = report.getPageFormat();
        PrinterJob job = PrinterJob.getPrinterJob();

        PageFormat format = job.pageDialog(oldFormat);

        if (format != oldFormat) {
            report.setPageFormat(format);
            refreshReport();
        }
    }

    protected void refreshReport() {
        new ReportWorker().execute();
    }

    private void setPageIndex(int index) {
        if (jPrint != null && jPrint.getPages() != null && !jPrint.getPages().isEmpty()) {
            if (index >= 0 && index < jPrint.getPages().size()) {
                pageIndex = index;
                firstButton.setEnabled((pageIndex > 0));
                previousButton.setEnabled((pageIndex > 0));
                nextButton.setEnabled((pageIndex < jPrint.getPages().size() - 1));
                lastButton.setEnabled((pageIndex < jPrint.getPages().size() - 1));
                frame.setStatus(MessageFormat.format(getBundleString("page"), pageIndex + 1, jPrint.getPages().size()));
            }
        } else {
            firstButton.setEnabled(false);
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            lastButton.setEnabled(false);

            frame.setStatus("");
        }
    }

    private void loadReport(final JasperPrint jasperPrint) {
        jPrint = jasperPrint;
        setPageIndex(0);
    }

    private void refreshPage() {
        if (jPrint == null || jPrint.getPages() == null || jPrint.getPages().isEmpty()) {
            pageGluePanel.setVisible(false);
            saveButton.setEnabled(false);
            printButton.setEnabled(false);
            actualSizeButton.setEnabled(false);
            fitPageButton.setEnabled(false);
            fitWidthButton.setEnabled(false);
            zoomInButton.setEnabled(false);
            zoomOutButton.setEnabled(false);
            zoomComboBox.setEnabled(false);

            if (jPrint != null) {
                JOptionPane.showMessageDialog(this, getBundleString("no.pages"));
            }

            return;
        }

        pageGluePanel.setVisible(true);
        saveButton.setEnabled(true);
        printButton.setEnabled(true);
        actualSizeButton.setEnabled(true);
        fitPageButton.setEnabled(true);
        fitWidthButton.setEnabled(true);
        zoomInButton.setEnabled(zoom < MAX_ZOOM);
        zoomOutButton.setEnabled(zoom > MIN_ZOOM);
        zoomComboBox.setEnabled(true);

        Dimension dim = new Dimension((int) (jPrint.getPageWidth() * realZoom) + 8, // 2 from border, 5 from shadow and 1 extra pixel for image
        (int) (jPrint.getPageHeight() * realZoom) + 8);

        pageGluePanel.setMaximumSize(dim);
        pageGluePanel.setMinimumSize(dim);
        pageGluePanel.setPreferredSize(dim);

        spaceHoldPanel.removeAll();

        pageRenderer.setIcon(null);

        mainPanel.validate();
        mainPanel.repaint();
    }

    private void emptyContainer(final Container container) {
        Component[] components = container.getComponents();

        if (components != null) {
            for (Component component1 : components) {
                if (component1 instanceof Container) {
                    emptyContainer((Container) component1);
                }
            }
        }

        container.removeAll();
    }

    private float getZoomRatio() {
        float newZoom;

        try {
            newZoom = zoomFormat.parse((String) zoomComboBox.getEditor().getItem()).floatValue() / 100f;
        } catch (ParseException e) {
            newZoom = zoom;
        }

        return newZoom;
    }

    private void setZoomRatio(final float newZoom) {
        if (newZoom > 0) {
            zoomComboBox.getEditor().setItem(zoomFormat.format(newZoom * 100) + "%");

            if (zoom != newZoom) {
                zoom = newZoom;
                realZoom = zoom * screenResolution / REPORT_RESOLUTION;

                refreshPage();
            }
        }
    }

    private void setRealZoomRatio(final float newZoom) {
        if (newZoom > 0 && realZoom != newZoom) {
            zoom = newZoom * REPORT_RESOLUTION / screenResolution;
            realZoom = newZoom;

            zoomComboBox.getEditor().setItem(zoomFormat.format(zoom * 100) + "%");

            refreshPage();
        }
    }

    private JRGraphics2DExporter getGraphics2DExporter() throws JRException {
        return new JRGraphics2DExporter();
    }

    private void paintPage(final Graphics2D g) {

        if (jPrint == null) { // don't paint unless jPrint is not null
            return;
        }

        try {
            if (exporter == null) {
                exporter = getGraphics2DExporter();
            } else {
                exporter.reset();
            }

            exporter.setParameter(JRExporterParameter.JASPER_PRINT, jPrint);
            exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, g.create());
            exporter.setParameter(JRExporterParameter.PAGE_INDEX, Integer.valueOf(pageIndex));
            exporter.setParameter(JRGraphics2DExporterParameter.ZOOM_RATIO, Float.valueOf(realZoom));
            exporter.setParameter(JRExporterParameter.OFFSET_X, 1); //pageRenderer border
            exporter.setParameter(JRExporterParameter.OFFSET_Y, 1);
            exporter.exportReport();
        } catch (Exception ex) {

            logger.log(Level.SEVERE, ex.getMessage(), ex);

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    JOptionPane.showMessageDialog(DynamicJasperReportPanel.this, getBundleString("error.displaying"));
                }
            });
        }

    }

    private void fitPage() {
        float heightRatio = ((float) scrollPanePanel.getVisibleRect().getHeight() - 20f) / jPrint.getPageHeight();
        float widthRatio = ((float) scrollPanePanel.getVisibleRect().getWidth() - 20f) / jPrint.getPageWidth();
        setRealZoomRatio(heightRatio < widthRatio ? heightRatio : widthRatio);
    }

    private void fontAction() {
        int oldSize = report.getBaseFontSize();
        int newSize = (Integer) fontSizeComboBox.getSelectedItem();

        if (oldSize != newSize) {
            report.setBaseFontSize(newSize);
            refreshReport();
        }
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (e.getSource() == saveButton) {
                    saveAction();
                } else if (e.getSource() == printButton) {
                    printAction();
                } else if (e.getSource() == pageSetupButton) {
                    pageSetupAction();
                } else if (e.getSource() == firstButton) {
                    firstPageAction();
                } else if (e.getSource() == previousButton) {
                    previousPageAction();
                } else if (e.getSource() == nextButton) {
                    nextPageAction();
                } else if (e.getSource() == lastButton) {
                    lastPageAction();
                } else if (e.getSource() == actualSizeButton) {
                    actualSizeAction();
                } else if (e.getSource() == fitPageButton) {
                    fitPageAction();
                } else if (e.getSource() == fitWidthButton) {
                    fitWidthAction();
                } else if (e.getSource() == zoomInButton) {
                    zoomInAction();
                } else if (e.getSource() == zoomOutButton) {
                    zoomOutAction();
                } else if (e.getSource() == zoomComboBox) {
                    zoomAction();
                } else if (e.getSource() == fontSizeComboBox) {
                    fontAction();
                } else if (e.getSource() == helpButton) {
                    UIApplication.showHelp(UIApplication.REPORTS_ID);
                }
            }
        });
    }

    private static class PageRenderer extends JLabel {

        private DynamicJasperReportPanel viewer = null;

        protected PageRenderer(final DynamicJasperReportPanel viewer) {
            this.viewer = viewer;
        }

        @Override
        public void paintComponent(final Graphics g) {
            viewer.paintPage((Graphics2D) g.create());
        }
    }

    private class ReportWorker extends SwingWorker<JasperPrint, Object> {

        @Override
        public JasperPrint doInBackground() {
            frame.displayWaitMessage(Resource.get().getString("Message.PleaseWait"));
            return report.createJasperPrint(false);
        }

        @Override
        protected void done() {
            try {
                loadReport(get());
                forceRefresh();
                frame.stopWaitMessage();
            } catch (InterruptedException | ExecutionException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

}
