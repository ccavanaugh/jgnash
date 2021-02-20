/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.app;

import jgnash.bootloader.BootLoader;
import jgnash.bootloader.BootLoaderDialog;

import javax.swing.JOptionPane;

import java.awt.EventQueue;

import picocli.CommandLine;

import static jgnash.util.LogUtil.logSevere;

/**
 * Bootstraps a modular jGnashFx by downloading platform specific OpenJFX libraries and then launching the application
 *
 * @author Craig Cavanaugh
 */
public class jGnash {

    public static void main(final String[] args) {

        boolean bypassBootLoader = false;

        final CommandLine commandLine = new CommandLine(new jGnashFx.CommandLineOptions());
        commandLine.setToggleBooleanFlags(false);
        commandLine.setUsageHelpWidth(80);

        try {
            final CommandLine.ParseResult pr = commandLine.parseArgs(args);
            final jGnashFx.CommandLineOptions options = commandLine.getCommand();

            if (CommandLine.printHelpIfRequested(pr)) {
                System.exit(0);
            }

            bypassBootLoader = options.bypassBootloader;
        } catch (final CommandLine.UnmatchedArgumentException uae) {
            commandLine.usage(System.err, CommandLine.Help.Ansi.AUTO);
            System.exit(1);
        } catch (final Exception e) {
            logSevere(jGnash.class, e);
            System.exit(1);
        }

        if (BootLoader.getOS() != null) {
            if (bypassBootLoader || BootLoader.doFilesExist()) {
                launch(args);
            } else {

                EventQueue.invokeLater(() -> {
                    final BootLoaderDialog d = new BootLoaderDialog();
                    d.setVisible(true);

                    final Thread thread = new Thread(() -> {
                        if (!BootLoader.downloadFiles(d.getActiveFileConsumer(), d.getPercentCompleteConsumer())) {
                            System.exit(-1);
                        }
                    });

                    thread.start();
                });
            }

        } else {
            JOptionPane.showMessageDialog(null, "Unsupported operating system", "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(BootLoader.FAILED_EXIT);
        }
    }

    private static void launch(final String[] args) {
        jGnashFx.main(args);
    }
}
