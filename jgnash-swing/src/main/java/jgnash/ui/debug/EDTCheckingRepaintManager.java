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
package jgnash.ui.debug;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Check for invalid UI updates on the EDT
 *
 * @author Craig Cavanaugh
 */
public final class EDTCheckingRepaintManager extends RepaintManager {

    @Override
    public synchronized void addInvalidComponent(final JComponent c) {
        checkThread();
        super.addInvalidComponent(c);
    }

    @Override
    public synchronized void addDirtyRegion(final JComponent c, final int x, final int y, final int w, final int h) {
        checkThread();
        super.addDirtyRegion(c, x, y, w, h);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private static void checkThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            final Exception exception = new Exception();

            boolean repaint = false;
            boolean fromSwing = false;

            for (StackTraceElement st : exception.getStackTrace()) {

                // javax.print is known to violate EDT rules
                if (st.getClassName().startsWith("com.sun.java.help")) {
                    return;
                }

                // javax.print is known to violate EDT rules
                if (st.getClassName().startsWith("javax.print")) {
                    return;
                }

                // java HTMLEditorKit is known to violate EDT rules
                if (st.getClassName().contains("HTMLEditorKit")) {
                    return;
                }

                // java ImageView is known to violate EDT rules
                if (st.getClassName().contains("text.html.ImageView")) {
                    return;
                }

                // java ImageView is known to violate EDT rules
                if (st.getClassName().contains("SubstanceListUI")) {
                    return;
                }

                if (repaint && st.getClassName().startsWith("javax.swing.")) {
                    fromSwing = true;
                }

                if ("repaint".equals(st.getMethodName())) {
                    repaint = true;
                }
            }
            if (repaint && !fromSwing) {
                //no problems here, since repaint() is thread safe
                return;
            }            
            
            Logger.getLogger(EDTCheckingRepaintManager.class.getName()).log(Level.SEVERE, exception.getLocalizedMessage(), exception);
        }
    }
}
