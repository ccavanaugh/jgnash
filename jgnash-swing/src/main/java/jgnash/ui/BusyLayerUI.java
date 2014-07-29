/*
* Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*   - Redistributions of source code must retain the above copyright
*     notice, this list of conditions and the following disclaimer.
*
*   - Redistributions in binary form must reproduce the above copyright
*     notice, this list of conditions and the following disclaimer in the
*     documentation and/or other materials provided with the distribution.
*
*   - Neither the name of Oracle or the names of its
*     contributors may be used to endorse or promote products derived
*     from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
* IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
* THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package jgnash.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.plaf.LayerUI;

/**
 * A busy layer for the UI
 * 
 * @author Craig Cavanaugh
 * @author Klemen Zagar
 */
class BusyLayerUI extends LayerUI<JPanel> implements ActionListener {

    private static final int FPS = 24;
    private static final int TIMER_TICK = 1000 / FPS;

    private boolean isRunning;
    private Timer timer = new Timer(TIMER_TICK, this);
    private int angle;
    private long fadeInStart;
    private long fadeOutStart;
    private final int fadeLimit = 1000;

    @Override
    public void paint(final Graphics g, final JComponent c) {
        int w = c.getWidth();
        int h = c.getHeight();

        // Paint the view.
        super.paint(g, c);

        if (!isRunning) {
            return;
        }

        float fade = 1.0f;
        if (fadeOutStart != 0) {
	        long timeSinceFadeStart = System.currentTimeMillis() - fadeOutStart;
	        if (timeSinceFadeStart > fadeLimit) { 
	        	return;
	        }
	        fade = 1.0f - ((float) timeSinceFadeStart / (float) fadeLimit);
        } else {
	        long timeSinceFadeStart = System.currentTimeMillis() - fadeInStart;
	        if (timeSinceFadeStart < fadeLimit) { 
		        fade = ((float) timeSinceFadeStart / (float) fadeLimit);
	        }
        }

        Graphics2D g2 = (Graphics2D) g.create();

        // Gray it out.
        Composite urComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f * fade));
        g2.fillRect(0, 0, w, h);
        g2.setComposite(urComposite);

        // Paint the wait indicator.
        int s = Math.min(w, h) / 5;

        int cx = w / 2;
        int cy = h / 2;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(s / (float)4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(Color.DARK_GRAY);
        g2.rotate(Math.PI * angle / 180, cx, cy);

        for (int i = 0; i < 8; i++) {
            float scale = (7.0f - i) / 7.0f;
            g2.drawLine(cx + s, cy, cx + s * 2, cy);
            g2.rotate(-Math.PI / 4, cx, cy);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scale * fade));
        }

        g2.dispose();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (isRunning) {
            firePropertyChange("tick", 0, 1);
            angle += 2;
            if (angle >= 360) {
                angle = 0;
            }
            if (fadeOutStart != 0) {
    	        long timeSinceFade = System.currentTimeMillis() - fadeOutStart;
                if (timeSinceFade > fadeLimit) {
                    isRunning = false;
                    timer.stop();
                }
            }
        }
    }

    public void start() {
        if (isRunning) {
            return;
        }

        // Run a thread for animation.
        isRunning = true;
        fadeInStart = System.currentTimeMillis();
        fadeOutStart = 0;

        timer = new Timer(TIMER_TICK, this);
        timer.start();
    }

    public void stop() {
        fadeOutStart = System.currentTimeMillis() - 1;
    }

    @Override
    @SuppressWarnings("rawtypes") 
    public void applyPropertyChange(final PropertyChangeEvent pce, final JLayer l) {
        if ("tick".equals(pce.getPropertyName())) {
            l.repaint();
        }
    }
}
