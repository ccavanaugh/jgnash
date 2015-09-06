package jgnash.ui.components;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

import jgnash.ui.ThemeManager;

/**
 * Mash-up of various bits and pieces found describing construction of an indeterminate check box.
 *
 * No license or copyright is claimed for this control
 *
 * derived from: http://svn.apache.org/repos/asf/jmeter/trunk/src/core/org/apache/jmeter/gui/util/TristateCheckBox.java
 * derived from: http://www.javaspecialists.eu/archive/Issue145.html
 * derived from: http://www.coderanch.com/t/342563/GUI/java/TriState-CheckBox
 * derived from: https://github.com/Sciss/SwingOSC/blob/work/src/main/java/de/sciss/swingosc/NimbusFocusBorder.java
 *
 * @author Craig Cavanaugh
 */
public final class IndeterminateCheckBox extends JCheckBox {

    public IndeterminateCheckBox(final String text) {
        this(text, null, SelectionState.DESELECTED);
    }

    public IndeterminateCheckBox(final String text, final Icon icon, final SelectionState initial) {
        this(text, icon, initial, false);
    }

    // For testing only at present
    IndeterminateCheckBox(final String text, final Icon icon, final SelectionState initial, final boolean original) {
        super(text, icon);

        setBorderPaintedFlat(true);

        //Set default single model
        setModel(new ButtonModelEx(initial, this, original));

        // override action behaviour
        super.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                IndeterminateCheckBox.this.iterateState();
            }
        });

        final ActionMap actions = new ActionMapUIResource();

        actions.put("pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IndeterminateCheckBox.this.iterateState();
            }
        });
        actions.put("released", null);

        SwingUtilities.replaceUIActionMap(this, actions);
    }

    // Next two methods implement new API by delegation to model
    public void setIndeterminate(final boolean value) {
        if (value) {
            ((ButtonModelEx)getModel()).setIndeterminate();
        } else {
            getModel().setSelected(true);
        }
    }

    public boolean isIndeterminate() {
        return ((ButtonModelEx)getModel()).isIndeterminate();
    }

    //Overrides superclass method
    @Override
    public void setModel(final ButtonModel newModel) {
        super.setModel(newModel);
        //Listen for enable changes
        if (model instanceof ButtonModelEx) {
            model.addChangeListener(new IndeterminateChangeListener());
        }
    }

    //Empty override of superclass method
    @Override
    public synchronized void addMouseListener(final MouseListener l) {
    }

    // Mostly delegates to model
    private void iterateState() {
        //Maybe do nothing at all?
        if (!getModel().isEnabled()) {
            return;
        }

        grabFocus();

        // Iterate state
        ((ButtonModelEx)getModel()).iterateState();

        // Fire ActionEvent
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent) currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent) currentEvent).getModifiers();
        }

        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText(), System.currentTimeMillis(),
                modifiers));
    }

    private static class ButtonModelEx extends ToggleButtonModel {

        private SelectionState state = SelectionState.DESELECTED;
        private final IndeterminateCheckBox indeterminateCheckBox;
        private final Icon icon;
        private final boolean original;

        public ButtonModelEx(final SelectionState initialState, final IndeterminateCheckBox indeterminateCheckBox,
                             final boolean original) {
            setState(initialState);
            this.indeterminateCheckBox = indeterminateCheckBox;
            icon = new CheckBoxIcon();
            this.original = original;
        }

        public void setIndeterminate() {
            setState(SelectionState.INDETERMINATE);
        }

        public boolean isIndeterminate() {
            return state == SelectionState.INDETERMINATE;
        }

        // Overrides of superclass methods
        @Override
        public void setEnabled(final boolean enabled) {
            super.setEnabled(enabled);

            updateIcons(); // Restore state display
        }

        @Override
        public void setSelected(final boolean selected) {
            setState(selected ? SelectionState.SELECTED : SelectionState.DESELECTED);
        }

        // Empty overrides of superclass methods
        @Override
        public void setArmed(boolean b) {
        }

        @Override
        public void setPressed(boolean b) {
        }

        void iterateState() {
            setState(state.next());
        }

        private void setState(final SelectionState state) {
            //Set internal state
            this.state = state;
            updateIcons();
            if (state == SelectionState.INDETERMINATE && isEnabled()) {
                // force the events to fire

                // Send ChangeEvent
                fireStateChanged();

                // Send ItemEvent
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, ItemEvent.DESELECTED));
            }
        }

        private void updateIcons() {
            super.setSelected(state != SelectionState.DESELECTED);
            if (original) {
                super.setArmed(state == SelectionState.INDETERMINATE);
            } else {
                if (state == SelectionState.INDETERMINATE) {
                    indeterminateCheckBox.setIcon(icon);
                    indeterminateCheckBox.setSelectedIcon(icon);
                    indeterminateCheckBox.setDisabledSelectedIcon(icon);
                } else { // reset
                    if (indeterminateCheckBox != null) {
                        indeterminateCheckBox.setIcon(null);
                        indeterminateCheckBox.setSelectedIcon(null);
                        indeterminateCheckBox.setDisabledSelectedIcon(null);
                    }
                }
            }
            super.setPressed(state == SelectionState.INDETERMINATE);
        }

        /*public SelectionState getState() {
            return state;
        }*/
    }

    private static class CheckBoxIcon implements Icon, UIResource {

        private final Icon icon = (Icon) UIManager.getLookAndFeelDefaults().get("CheckBox.icon");
        private final Color lineColor = UIManager.getColor("controlText");

        private final int iconHeight = icon.getIconHeight();
        private final int iconWidth = icon.getIconWidth();

        private final NimbusFocusBorder nimbusFocusBorder = new NimbusFocusBorder(2);

        @Override
        @SuppressWarnings("unchecked")
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            JCheckBox cb = (JCheckBox) c;
            javax.swing.ButtonModel model = cb.getModel();

            if (ThemeManager.isLookAndFeelNimbus()) {
                icon.paintIcon(c, g, x, y);

                if (c.hasFocus()) {
                    nimbusFocusBorder.paintBorder(g, x, y, iconWidth, iconHeight);
                }

                drawIndeterminateNimbusLine(g, x, y);
            } else {
                if (model.isEnabled()) {
                    if (model.isPressed() && model.isArmed()) {
                        g.setColor(MetalLookAndFeel.getControlShadow());
                        g.fillRect(x, y, iconWidth - 1, iconHeight - 1);
                        drawPressed3DBorder(g, x, y, iconWidth, iconHeight);
                    } else {
                        drawFlush3DBorder(g, x, y, iconWidth, iconHeight);
                    }
                    g.setColor(MetalLookAndFeel.getControlInfo());
                } else {
                    g.setColor(MetalLookAndFeel.getControlShadow());
                    g.drawRect(x, y, iconWidth - 1, iconHeight - 1);
                }

                drawIndeterminateLine(g, x, y);
            }
        }

        private void drawIndeterminateLine(final Graphics g, final int x, final int y) {
            g.setColor(lineColor);
            final int left = x + 2, right = x + (iconWidth - 4);
            int height = y + iconHeight / 2;
            g.drawLine(left, height, right, height);
            g.drawLine(left, height - 1, right, height - 1);
        }

        private void drawIndeterminateNimbusLine(final Graphics g, final int x, final int y) {
            g.setColor(lineColor);
            final int left = x + 5, right = x + (iconWidth - 6);
            int height = y + iconHeight / 2;
            g.drawLine(left, height, right, height);
            g.drawLine(left, height - 1, right, height - 1);
        }

        private static void drawFlush3DBorder(final Graphics g, final int x, final int y, final int w, final int h) {
            g.translate(x, y);
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(0, 0, w - 2, h - 2);
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawRect(1, 1, w - 2, h - 2);
            g.setColor(MetalLookAndFeel.getControl());
            g.drawLine(0, h - 1, 1, h - 2);
            g.drawLine(w - 1, 0, w - 2, 1);
            g.translate(-x, -y);
        }

        private static void drawPressed3DBorder(final Graphics g, final int x, final int y, final int w, final int h) {
            g.translate(x, y);
            drawFlush3DBorder(g, 0, 0, w, h);
            g.setColor(MetalLookAndFeel.getControlShadow());
            g.drawLine(1, 1, 1, h - 2);
            g.drawLine(1, 1, w - 2, 1);
            g.translate(-x, -y);
        }

        @Override
        public int getIconWidth() {
            return iconWidth;
        }

        @Override
        public int getIconHeight() {
            return iconHeight;
        }
    }

    public enum SelectionState {
        INDETERMINATE {
            @Override
            public SelectionState next() {
                return SELECTED;
            }
        },
        SELECTED {
            @Override
            public SelectionState next() {
                return DESELECTED;
            }
        },
        DESELECTED {
            @Override
            public SelectionState next() {
                return INDETERMINATE;
            }
        };

        public abstract SelectionState next();
    }

    // Listener on model changes to maintain correct focusability
    private final class IndeterminateChangeListener implements ChangeListener {

        @Override
        public void stateChanged(final ChangeEvent e) {
            IndeterminateCheckBox.this.setFocusable(getModel().isEnabled());
        }
    }

    private static class NimbusFocusBorder {

        private final RoundRectangle2D rect = new RoundRectangle2D.Float();
        private final Area area = new Area();

        private final float insideRadius;
        private final float outsideRadius;

        private final Color focusColor = UIManager.getLookAndFeelDefaults().getColor("nimbusFocus");

        public NimbusFocusBorder(final float radius) {
            insideRadius = radius * 2;
            outsideRadius = insideRadius + 2.8f;
        }

        public void paintBorder(final Graphics g, final int x, final int y, final int w, final int h) {
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(focusColor);
            rect.setRoundRect(x + 0.6, y + 0.6, w - 1.2, h - 1.2, outsideRadius, outsideRadius);
            area.reset();
            area.add(new Area(rect));
            rect.setRoundRect(x + 2, y + 2, w - 4, h - 4, insideRadius, insideRadius);
            area.subtract(new Area(rect));
            ((Graphics2D)g).fill(area);
        }
    }
}
