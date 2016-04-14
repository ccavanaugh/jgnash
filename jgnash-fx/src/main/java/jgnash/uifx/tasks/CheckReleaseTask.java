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
package jgnash.uifx.tasks;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.concurrent.Task;

import jgnash.uifx.StaticUIMethods;
import jgnash.util.ResourceUtils;
import jgnash.util.Version;

/**
 * Simple Task to check and display a message about new jGnash releases
 *
 * @author Craig Cavanaugh
 */
public class CheckReleaseTask extends Task<Boolean> {

    @Override
    protected Boolean call() throws Exception {
        return Version.isReleaseCurrent();
    }

    @Override protected void succeeded() {

        try {
            if (!get()) {
                StaticUIMethods.displayMessage(ResourceUtils.getString("Message.NewVersion"));
            }
        } catch (InterruptedException | ExecutionException e) {
            Logger.getLogger(CheckReleaseTask.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
