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
package jgnash.ui.components;

import java.awt.Graphics;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import jgnash.util.OS;

/**
 * JButtons do not respond to look and feel updates correctly.  This
 * is an extended JButton to handle Look and Feel problems.  It also configures
 * itself for a rollover toolbar correctly.
 *
 * @author Craig Cavanaugh
 */
public class RollOverButton extends JButton {

    public RollOverButton(String text, Icon icon) {
        super(text, icon);
        configure();
    }

    public RollOverButton(Action a) {
        super(a);
        configure();
    }

    private void configure() {
        setRequestFocusEnabled(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        
        if (OS.isSystemOSX()) {        
            putClientProperty("JButton.buttonType", "textured");
        }
    }

    @Override
    protected void paintBorder(final Graphics g) {
        if (model.isRollover()) {
            super.paintBorder(g);
        }
    }
}