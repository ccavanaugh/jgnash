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
package jgnash.ui.splash;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLDocument;

import jgnash.ui.util.HTMLResource;
import jgnash.util.Resource;

import org.jfree.ui.about.SystemPropertiesPanel;

/**
 * This is the jGnash About Dialog.
 * 
 * @author Craig Cavanaugh
 *
 */
public class AboutDialog extends JDialog implements ActionListener {

    private final Resource rb = Resource.get();

    private JTabbedPane tabbedPane;

    private final boolean acceptLicense;

    private JCheckBox acceptBox;

    private JButton closeButton;

    public static void showDialog(final Frame parent) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JDialog dlg = new AboutDialog(parent, false);
                dlg.setVisible(true);
            }
        });
    }

    public static boolean showAcceptLicenseDialog() {
        AboutDialog dlg = new AboutDialog(null, true);
        dlg.setVisible(true);
        return dlg.isAccepted();
    }

    private AboutDialog(final Frame parent, final boolean acceptLicense) {
        super(parent, true);

        applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

        this.acceptLicense = acceptLicense;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();

        // Get the system resolution
        Dimension res = Toolkit.getDefaultToolkit().getScreenSize();

        // make sure the dialog is not too big
        Dimension size = new Dimension(Math.min(600, res.width - 80), Math.min(600, res.height - 80));

        setSize(size);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        tabbedPane = new javax.swing.JTabbedPane();
        setTitle(rb.getString("Title.About"));

        addHTMLTab(rb.getString("Menu.About.Name"), "notice.html");
        addHTMLTab(rb.getString("Tab.Credits"), "credits.html");
        JComponent defaultTab = addHTMLTab("jGnash License", "jgnash-license.html");
        addHTMLTab(rb.getString("Tab.GPLLicense"), "gpl-license.html");
        addHTMLTab(rb.getString("Tab.LGPLLicense"), "lgpl.html");
        addHTMLTab("Apache License", "apache-license.html");
        addHTMLTab("JGoodies Common License", "jgoodies-common-license.html");
        addHTMLTab("JGoodies Forms License", "jgoodies-forms-license.html");
        addHTMLTab("JGoodies Looks License", "jgoodies-looks-license.html");
        addHTMLTab("Tango Icons License", "sharealike-license.html");
        addHTMLTab("XStream License", "xstream-license.html");
        addHTMLTab("Substance License", "substance-license.html");
        addHTMLTab("JXLayer License", "jxlayer-license.html");

        tabbedPane.addTab(rb.getString("Tab.SysInfo"), new SystemPropertiesPanel());

        tabbedPane.setSelectedComponent(defaultTab);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("200dlu:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.appendRow(RowSpec.decode("fill:200dlu:g"));
        builder.append(tabbedPane);

        if (acceptLicense) {
            acceptBox = new JCheckBox(rb.getString("Message.AcceptLicense"));
            closeButton = new JButton(rb.getString("Button.Close"));
            closeButton.addActionListener(this);

            builder.nextLine();
            builder.appendUnrelatedComponentsGapRow();
            builder.nextLine();
            builder.append(acceptBox);
            builder.nextLine();
            builder.appendUnrelatedComponentsGapRow();
            builder.nextLine();
            builder.append(ButtonBarFactory.buildCloseBar(closeButton));
        }

        getContentPane().add(builder.getPanel());
        pack();
    }

    private JComponent addHTMLTab(final String name, final String url) {

        try {
            URL noticeURL = HTMLResource.getURL(url);
            JEditorPane p = new JEditorPane(noticeURL);

            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument) p.getDocument()).getStyleSheet().addRule(bodyRule);

            p.setEditable(false);
            p.setAutoscrolls(true);

            JScrollPane pane = new JScrollPane(p);

            tabbedPane.add(name, pane);

            return pane;
        } catch (final Exception e) {
            Logger.getLogger(AboutDialog.class.getName()).log(Level.SEVERE, e.toString(), e);
        }

        return null;
    }

    private boolean isAccepted() {
        return acceptBox.isSelected();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(AboutDialog.this, WindowEvent.WINDOW_CLOSING));
        }
    }
}
