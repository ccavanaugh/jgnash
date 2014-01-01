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
package jgnash.ui;

import com.jgoodies.forms.factories.Borders;
import com.sun.management.HotSpotDiagnosticMXBean;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;

import jgnash.engine.Engine;
import jgnash.ui.components.MemoryMonitor;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Simple dialog to display info dumped to the console. Makes it easy for end
 * users on Windows to capture errors.
 *
 * @author Craig Cavanaugh
 *
 */
@SuppressWarnings("restriction")
public class ConsoleDialog {

    private static JTextArea console;
    private static JDialog dialog;
    private static boolean init = false;
    private static final Object consoleLock = new Object();
    private static PrintStream outStream;
    private static PrintStream errStream;

    private ConsoleDialog() {
    }

    /* Only need to initialize one time.  After that the output
     * streams are static and will only pipe output to the window
     * if it exists.
     */
    private static void init() {
        if (!init) {
            init = true;

            final PrintStream oldOut = System.out;
            final PrintStream oldErr = System.err;
            try {
                outStream = new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        oldOut.write(b, off, len);
                        synchronized (consoleLock) {
                            if (console != null) {
                                console.append(new String(b, off, len, Charset.defaultCharset()));
                            }
                        }
                    }
                }, false, Charset.defaultCharset().name());
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ConsoleDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                errStream = new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        oldErr.write(b, off, len);
                        synchronized (consoleLock) {
                            if (console != null) {
                                console.append(new String(b, off, len, Charset.defaultCharset()));
                            }
                        }
                    }
                }, false, Charset.defaultCharset().name());
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ConsoleDialog.class.getName()).log(Level.SEVERE, null, ex);
            }

            // set both System.out and System.err to that stream
            System.setOut(outStream);
            System.setErr(errStream);

            // capture the engine log
            Engine.getLogger().addHandler(new Handler() {
                @Override
                public void close() throws SecurityException {
                }

                @Override
                public void flush() {
                }

                @Override
                public void publish(final LogRecord record) {
                    // update on the event thread to prevent display corruption
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (consoleLock) {
                                if (console != null) {
                                    console.append(record.getMessage() + "\n");
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    private static void dumpHeap() {

        String base = FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath();
        String filesep = System.getProperty("file.separator");

        File dumpFile = null;

        // no more than 1000 dumps
        for (int i = 1; i < 1000; i++) {
            dumpFile = new File(base + filesep + "jGnashHeapDump" + i + ".bin");

            if (!dumpFile.exists()) {
                break;
            }
        }

        if (dumpFile != null) {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            try {
                HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);

                bean.dumpHeap(dumpFile.getAbsolutePath(), true);
            } catch (IOException e) {
                Logger.getLogger(ConsoleDialog.class.getCanonicalName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public static void show() {
        if (dialog == null) { // only one visible window
            init();

            Resource rb = Resource.get();

            JButton copyButton = new JButton(rb.getString("Button.CopyToClip"));

            copyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (console != null) {
                        console.selectAll();
                        console.copy();
                    }
                }
            });

            JButton gcButton = new JButton(rb.getString("Button.ForceGC"));

            gcButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.gc();
                }
            });

            JButton heapButton = new JButton(rb.getString("Button.CreateHeapDump"));

            heapButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (console != null) {
                        dumpHeap();
                    }
                }
            });

            dialog = new JDialog(UIApplication.getFrame(), Dialog.ModalityType.MODELESS);

            dialog.setTitle(rb.getString("Title.ConsoleWindow"));

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    /* force the shut down to the end of the event thread.
                     * Lets other listeners do their job */
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (consoleLock) {
                                ConsoleDialog.close();
                            }
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

            JPanel buttonPanel = StaticUIMethods.buildRightAlignedBar(heapButton, gcButton, copyButton);
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
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                dialog = null;
                console = null;
            }
        }
    }
}
