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
package jgnash.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jgnash.ui.components.VerticalTextIcon;

import org.jdesktop.swingx.JXTitledPanel;

/**
 * A Panel that holds all the views and uses a card layout to display the panels.
 * 
 * @author Craig Cavanaugh
 */
class MainViewPanel extends JPanel implements ActionListener {

    private JPanel contentPanel;

    private CardLayout cardLayout;

    private ButtonPanel buttonPanel;

    private Component last = null;

    private final transient Preferences pref = Preferences.userNodeForPackage(MainViewPanel.class);

    private static final String LAST_VIEW = "lastview";

    public MainViewPanel() {
        super();
        init();
    }

    private void init() {

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        buttonPanel = new ButtonPanel();

        layoutPanel();
    }

    private void layoutPanel() {
        FormLayout layout = new FormLayout("min, $lcgap, fill:min:g", "fill:p:g");
        CellConstraints cc = new CellConstraints();

        setLayout(layout);
        add(buttonPanel, cc.xy(1, 1));
        add(contentPanel, cc.xy(3, 1));
    }

    void addView(final Container component, final String description, final String toolTip) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(component);

        JXTitledPanel p = new JXTitledPanel(toolTip, component);

        component.setName(description);
        p.setName(description);

        JButton button = new Button(component, new VerticalTextIcon(description, false));

        button.addActionListener(this);
        button.setActionCommand(description);
        button.setName(description);

        if (toolTip != null) {
            button.setToolTipText(toolTip);
        } else {
            button.setToolTipText(description);
        }

        buttonPanel.addButton(button);

        if (contentPanel.getComponentCount() == 0) {
            last = component;
        }

        contentPanel.add(p, description);

        String lastComponent = pref.get(LAST_VIEW, "");

        if (lastComponent.equals(description)) {
            cardLayout.show(contentPanel, description);
            last = component;
        }
    }

    @Override
    /** Override to clear only the button panel and the content panel of their contents
     */
    public void removeAll() {
        last = null;
        buttonPanel.removeAll();
        contentPanel.removeAll();
    }

    /**
     * Invoked when an action occurs.
     * 
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (last != ((MainViewPanel.Button) e.getSource()).getComponent()) {
            last = ((MainViewPanel.Button) e.getSource()).getComponent();

            /* Display the selected component */
            cardLayout.show(contentPanel, e.getActionCommand());

            /*
             * A button has been pressed. Notify any listeners that selection
             * has changed
             */
            fireActionPerformed(new ActionEvent(MainViewPanel.this, ActionEvent.ACTION_PERFORMED, e.getActionCommand(), e.getWhen(), e.getModifiers()));

            // save the action command for the selected view
            pref.put(LAST_VIEW, e.getActionCommand());
        }
    }

    /**
     * Returns the active component
     * 
     * @return the visible component
     */
    public Component getVisibleComponent() {
        return last;
    }

    private static Insets margin = new Insets(10, 1, 10, 1);

    private static class Button extends JButton {

        private final Component component;

        private final Icon icon;

        Button(Component component, Icon icon) {
            super(icon);
            this.icon = icon;
            this.component = component;
            setHorizontalTextPosition(SwingConstants.CENTER);
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setMargin(margin);
            setFocusPainted(false);
        }

        public Component getComponent() {
            return component;
        }

        @Override
        public void updateUI() {
            if (ThemeManager.isLookAndFeelOSX()) {
                margin = new Insets(20, 6, 20, 6);
            } else {
                margin = new Insets(10, 1, 10, 1);
            }

            setMargin(margin);

            if (icon instanceof VerticalTextIcon) {
                ((VerticalTextIcon) icon).updateUI();
            }
            super.updateUI();
        }
    }

    /**
     * Adds an {@code ActionListener} to the button.
     * 
     * @param l the {@code ActionListener} to be added
     */
    void addActionListener(final ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

//    /**
//     * Removes an <code>ActionListener</code> from the panel
//     *
//     * @param l the listener to be removed
//     */
//    public void removeActionListener(final ActionListener l) {
//        listenerList.remove(ActionListener.class, l);
//    }

    private void fireActionPerformed(final ActionEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                // Lazily create the event:
                if (e == null) {
                    String actionCommand = event.getActionCommand();
                    if (actionCommand == null) {
                        actionCommand = "";
                    }
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand, event.getWhen(), event.getModifiers());
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    private static final class ButtonPanel extends JPanel {

        private FormLayout layout = null;

        public ButtonPanel() {
            super();
            setLayout();
        }

        void addButton(final JButton button) {

            CellConstraints cc = new CellConstraints();

            if (layout.getRowCount() > 0) {
                layout.appendRow(RowSpec.decode("$ug"));
            }

            layout.appendRow(RowSpec.decode("min"));
            add(button, cc.xy(1, layout.getRowCount()));
        }

        @Override
        public void removeAll() {
            super.removeAll();
            setLayout();
        }

        private void setLayout() {
            layout = new FormLayout("[min,15dlu]", "");
            setLayout(layout);
        }
    }
}
