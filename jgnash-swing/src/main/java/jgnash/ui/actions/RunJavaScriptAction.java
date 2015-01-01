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
package jgnash.ui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JFileChooser;

import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;

/**
 * UI Action to open the currencies dialog
 *
 * @author Craig Cavanaugh
 */
@Action("javascript-command")
public class RunJavaScriptAction extends AbstractEnabledAction {

    private static final String JAVASCRIPT_DIR = "javascriptdir";

    @Override
    public void actionPerformed(final ActionEvent e) {

        final Preferences pref = Preferences.userNodeForPackage(RunJavaScriptAction.class);

        JFileChooser chooser = new JFileChooser(pref.get(JAVASCRIPT_DIR, null));
        chooser.setMultiSelectionEnabled(false);

        if (chooser.showOpenDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(JAVASCRIPT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            final String file = chooser.getSelectedFile().getAbsolutePath();

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try (final Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        new ScriptEngineManager().getEngineByName("JavaScript").eval(reader);
                    } catch (IOException | ScriptException e) {
                        Logger.getLogger(RunJavaScriptAction.class.getName()).log(Level.SEVERE, e.toString(), e);
                    }
                }
            });
        }
    }
}
