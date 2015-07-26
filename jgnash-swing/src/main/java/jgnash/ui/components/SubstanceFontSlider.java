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
package jgnash.ui.components;

import java.awt.EventQueue;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;

import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.fonts.FontPolicy;
import org.pushingpixels.substance.api.fonts.FontSet;

/**
 * Slider component to manage the global font size of the Substance Look and Feel
 * 
 * @author Craig Cavanaugh
 *
 */
public class SubstanceFontSlider extends JPanel {

    private static final String ADJUSTMENT_KEY = "adjustment";

    private static final String SUBSTANCE_FONT_SET = "Substance";

    private final JLabel fontSizeLabel;

    private static final int baseSize;

    static {
        baseSize = SubstanceLookAndFeel.getFontPolicy().getFontSet(SUBSTANCE_FONT_SET, null).getControlFont().getSize();
    }

    public SubstanceFontSlider() {

        FormLayout layout = new FormLayout("fill:pref, 1dlu, 50dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        setLayout(layout);

        fontSizeLabel = new JLabel();
        fontSizeLabel.setText(baseSize + " pt.");
        builder.append(fontSizeLabel);

        final JSlider slider = new JSlider(-3, 6, 0);
        slider.setFocusable(false);
        slider.setMinorTickSpacing(1);
        slider.setSnapToTicks(true);

        slider.setToolTipText(ResourceUtils.getString("ToolTip.FontSize"));

        slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {

                EventQueue.invokeLater(() -> fontSizeLabel.setText(slider.getValue() + baseSize + " pt."));

                if (!slider.getModel().getValueIsAdjusting()) {
                    final int adjust = slider.getValue();

                    adjustSize(adjust);
                    Preferences.userNodeForPackage(SubstanceFontSlider.class).putInt(ADJUSTMENT_KEY, adjust);
                }
            }
        });

        builder.append(slider);

        int adjust = Preferences.userNodeForPackage(SubstanceFontSlider.class).getInt(ADJUSTMENT_KEY, 0);

        if (adjust != 0) {
            adjustSize(adjust);
        }
    }

    private void adjustSize(final int adjust) {
        EventQueue.invokeLater(() -> {
            SubstanceLookAndFeel.setFontPolicy(null);
            final FontSet substanceCoreFontSet = SubstanceLookAndFeel.getFontPolicy().getFontSet(SUBSTANCE_FONT_SET, null);

            FontPolicy newFontPolicy = (lafName, table) -> new WrapperFontSet(substanceCoreFontSet, adjust);

            SubstanceLookAndFeel.setFontPolicy(newFontPolicy);

            fontSizeLabel.setText(adjust + baseSize + " pt.");
        });
    }

    private static class WrapperFontSet implements FontSet {

        private final int adjust;

        private final FontSet delegate;

        public WrapperFontSet(final FontSet delegate, final int adjust) {
            super();
            this.delegate = delegate;
            this.adjust = adjust;
        }

        private FontUIResource getWrappedFont(FontUIResource systemFont) {
            return new FontUIResource(systemFont.getFontName(), systemFont.getStyle(), systemFont.getSize() + adjust);
        }

        @Override
        public FontUIResource getControlFont() {
            return getWrappedFont(delegate.getControlFont());
        }

        @Override
        public FontUIResource getMenuFont() {
            return getWrappedFont(delegate.getMenuFont());
        }

        @Override
        public FontUIResource getMessageFont() {
            return getWrappedFont(delegate.getMessageFont());
        }

        @Override
        public FontUIResource getSmallFont() {
            return getWrappedFont(delegate.getSmallFont());
        }

        @Override
        public FontUIResource getTitleFont() {
            return getWrappedFont(delegate.getTitleFont());
        }

        @Override
        public FontUIResource getWindowTitleFont() {
            return getWrappedFont(delegate.getWindowTitleFont());
        }
    }

}
