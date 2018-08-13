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
package jgnash.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import jgnash.engine.Engine;
import jgnash.ui.components.MemoryMonitor;
import jgnash.ui.util.DialogUtils;
import jgnash.util.NotNull;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.factories.Borders;

/**
 * Simple dialog to display info dumped to the console. Makes it easy for end
 * users on Windows to capture errors.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("restriction")
public class ConsoleDialog {

    private static JTextArea console;

    private static JDialog dialog;

    private static boolean init = false;

    private static final Object consoleLock = new Object();
    
    private static PrintStream oldOutStream;

    private static PrintStream oldErrStream;

    private static PrintStream outStream;

    private static PrintStream errStream;
    
    private static Handler logHandler;

    public ConsoleDialog() {
    }

    /* Only need to initialize one time.  After that the output
     * streams are static and will only pipe output to the window
     * if it exists.
     */
    private static void init() {
        if (!init) {
            init = true;
            
            oldErrStream = System.err;
            oldOutStream = System.out;

            try {                
                outStream = new PrintStream(new ConsoleStream(oldOutStream), false, Charset.defaultCharset().name());
                System.setOut(outStream);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ConsoleDialog.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {               
                errStream = new PrintStream(new ConsoleStream(oldErrStream), false, Charset.defaultCharset().name());
                System.setErr(errStream);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ConsoleDialog.class.getName()).log(Level.SEVERE, null, ex);
            }

            // capture the engine log
             logHandler = new Handler() {
                @Override
                public void close() throws SecurityException {
                }

                @Override
                public void flush() {
                }

                @Override
                public void publish(final LogRecord record) {
                    // update on the event thread to prevent display corruption
                    EventQueue.invokeLater(() -> {
                        synchronized (consoleLock) {
                            if (console != null) {
                                console.append(record.getMessage() + "\n");
                            }
                        }
                    });
                }
            };
            
            Engine.getLogger().addHandler(logHandler);
        }
    }
    
    public static void show() {
        if (dialog == null) { // only one visible window
            init();

            ResourceBundle rb = ResourceUtils.getBundle();

            JButton copyButton = new JButton(rb.getString("Button.CopyToClip"));

            copyButton.addActionListener(e -> {
                if (console != null) {
                    console.selectAll();
                    console.copy();
                }
            });

            JButton gcButton = new JButton(rb.getString("Button.ForceGC"));

            gcButton.addActionListener(e -> System.gc());
           
            dialog = new JDialog(UIApplication.getFrame(), Dialog.ModalityType.MODELESS);

            dialog.setTitle(rb.getString("Title.Console"));

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    /* force the shut down to the end of the event thread.
                     * Lets other listeners do their job */
                    EventQueue.invokeLater(() -> {
                        synchronized (consoleLock) {
                            ConsoleDialog.close();
                        }
                    });
                }
            });

            synchronized (consoleLock) {
                console = new JTextArea();
                console.setEditable(false);

                // set a mono spaced font to preserve spacing
                console.setFont(new Font("Monospaced", Font.PLAIN, console.getFont().getSize()));
            }

            JPanel panel = new JPanel();
            panel.setBorder(Borders.DIALOG);
            panel.setLayout(new BorderLayout());

            panel.add(new MemoryMonitor(), BorderLayout.NORTH);
            panel.add(new JScrollPane(console), BorderLayout.CENTER);

            JPanel buttonPanel = StaticUIMethods.buildRightAlignedBar(gcButton, copyButton);
            buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

            panel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.getContentPane().add(panel, BorderLayout.CENTER);

            dialog.pack();
            dialog.setMinimumSize(dialog.getSize()); // Minimum size
            dialog.setFocusableWindowState(false);
           
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            DialogUtils.addBoundsListener(dialog);

            dialog.setVisible(true);
        }
    }

    private static void close() {
        synchronized (consoleLock) {
            if (dialog != null) {
            	Engine.getLogger().removeHandler(logHandler); 
            	logHandler = null;
            	
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                dialog = null;
                console = null;
                
                System.setOut(oldOutStream);
                System.setErr(oldErrStream);
                
                outStream.close();
                errStream.close();
                
                oldOutStream = null;
                oldErrStream = null;
                
                outStream = null;
                errStream = null;
                
                init = false;                               
            }
        }
    }

    private static class ConsoleStream extends OutputStream {
        final PrintStream stream;

        ConsoleStream(final PrintStream stream) {
            this.stream = stream;
        }

        @Override
        public void write(int ignored) {

        }

        @Override
        public void write(final @NotNull byte[] b, final int off, final int len) {
            stream.write(b, off, len);
            synchronized (consoleLock) {
                if (console != null) {
                    console.append(new String(b, off, len, Charset.defaultCharset()));
                }
            }
        }
    }
}
