/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.convert.imports;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author Craig Cavanaugh
 */
public class ImportFilter {

    public static String[] KNOWN_SCRIPTS = {"/jgnash/imports/tidy.js"};

    private final ScriptEngine engine;
    private final String script;

    public ImportFilter(final String script) {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        this.script = script;
    }

    public String processMemo(final String memo) {

        try (final Reader reader = new InputStreamReader(Object.class.getResourceAsStream(script))) {
            engine.eval(reader);

            final Invocable invocable = (Invocable) engine;

            final Object result = invocable.invokeFunction("processMemo", memo);

            return result.toString();
        } catch (final ScriptException | IOException | NoSuchMethodException e) {
            Logger.getLogger(ImportFilter.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return memo;
    }

    public String processPayee(final String payee) {

        try (final Reader reader = new InputStreamReader(Object.class.getResourceAsStream(script))) {
            engine.eval(reader);

            final Invocable invocable = (Invocable) engine;

            final Object result = invocable.invokeFunction("processPayee", payee);

            return result.toString();
        } catch (final ScriptException | IOException | NoSuchMethodException e) {
            Logger.getLogger(ImportFilter.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return payee;
    }

    public String getDescription() {

        try (final Reader reader = new InputStreamReader(Object.class.getResourceAsStream(script))) {
            engine.eval(reader);

            final Invocable invocable = (Invocable) engine;

            final Object result = invocable.invokeFunction("getDescription", Locale.getDefault());

            return result.toString();
        } catch (final ScriptException | IOException | NoSuchMethodException e) {
            Logger.getLogger(ImportFilter.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return "";
    }
}
