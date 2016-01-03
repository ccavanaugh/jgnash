/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JProgressBar;

import jgnash.util.ResourceUtils;

/**
 * Simple memory monitor.
 * 
 * @author Craig Cavanaugh
 *
 */
public class MemoryMonitor extends JProgressBar {

    private int total;

    private int used;

    private int oldUsed;

    private static final int diff = 1;

    private final Runtime runtime = Runtime.getRuntime();

    public MemoryMonitor() {
        super();
        setMinimum(0);
        setStringPainted(true);

        setToolTipText(ResourceUtils.getString("ToolTip.MemoryUsage"));

        setString(used + "/" + total + " MB");

        // reduce size of font
        setFont(getFont().deriveFont(getFont().getSize2D() - 1f).deriveFont(Font.PLAIN));

        /* A reusable runnable */
        final Runnable update = () -> {
            setMaximum(total);
            setValue(used);
            setString(used + "/" + total + " MB");
        };

        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                total = (int) (runtime.totalMemory() / 1024000);
                used = total - (int) (runtime.freeMemory() / 1024000);
                if (used < oldUsed - diff || used > oldUsed + diff) {
                    EventQueue.invokeLater(update);
                    oldUsed = used;
                }
            }
        }, 9, 1000);
    }
}
