/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2021 Craig Cavanaugh
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
package jgnash.convert.importat;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.util.EncodeDecode;
import jgnash.util.FileUtils;
import jgnash.util.NotNull;
import jgnash.resource.util.OS;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static jgnash.util.FileUtils.SEPARATOR;

/**
 * Transaction Import filter.
 * <p>
 * This class is responsible for calling the supplied javascript file.
 *
 * @author Craig Cavanaugh
 */
public class ImportFilter {

    private final static String ENABLED_FILTERS = "enabledFilters";

    private final static String IMPORT_SCRIPT_DIRECTORY_NAME = "importScripts";

    private static final String JS_REGEX_PATTERN = ".*.js";

    private static final Logger logger = Logger.getLogger(ImportFilter.class.getName());

    private static final String[] KNOWN_SCRIPTS = {"/jgnash/convert/scripts/tidy.js"};

    private final ScriptEngine scriptEngine;

    private final String script;

    ImportFilter(final String script) {
        scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        this.script = script;
        evalScript();
    }

    public static List<ImportFilter> getImportFilters() {
        final List<ImportFilter> importFilterList = new ArrayList<>();

        // known filters first
        for (String knownScript : KNOWN_SCRIPTS) {
            importFilterList.add(new ImportFilter(knownScript));
        }

        for (final Path path : FileUtils.getDirectoryListing(getUserImportScriptDirectory(), JS_REGEX_PATTERN)) {
            importFilterList.add(new ImportFilter(path.toString()));
        }

        final String activeDatabase = EngineFactory.getActiveDatabase();
        if (activeDatabase != null && !activeDatabase.startsWith(EngineFactory.REMOTE_PREFIX)) {
            for (final Path path : FileUtils.getDirectoryListing(getBaseFileImportScriptDirectory(Paths.get(activeDatabase)), JS_REGEX_PATTERN)) {
                importFilterList.add(new ImportFilter(path.toString()));
            }
        }

        return importFilterList;
    }

    public static List<ImportFilter> getEnabledImportFilters() {
        List<ImportFilter> filterList = new ArrayList<>();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final String string : EncodeDecode.decodeStringCollection(engine.getPreference(ENABLED_FILTERS))) {
            filterList.add(new ImportFilter(string));
        }

        return filterList;
    }

    public static void saveEnabledImportFilters(final List<ImportFilter> filters) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (filters != null && filters.size() > 0) {
            final List<String> scripts = filters.stream().map(ImportFilter::getScript).collect(Collectors.toList());
            engine.setPreference(ENABLED_FILTERS, EncodeDecode.encodeStringCollection(scripts));
        } else {
            engine.setPreference(ENABLED_FILTERS, null);
        }
    }

    private static Path getUserImportScriptDirectory() {

        String scriptDirectory = System.getProperty("user.home");

        // decode to correctly handle spaces, etc. in the returned path
        try {
            scriptDirectory = URLDecoder.decode(scriptDirectory, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }

        if (OS.isSystemWindows()) {
            scriptDirectory += SEPARATOR + "AppData" + SEPARATOR + "Local" + SEPARATOR
                    + "jgnash" + SEPARATOR + IMPORT_SCRIPT_DIRECTORY_NAME;
        } else { // unix, osx
            scriptDirectory += SEPARATOR + ".jgnash" + SEPARATOR + IMPORT_SCRIPT_DIRECTORY_NAME;
        }

        logger.log(Level.INFO, "Import Script path: {0}", scriptDirectory);


        return Paths.get(scriptDirectory);
    }

    private static Path getBaseFileImportScriptDirectory(@NotNull final Path baseFile) {
        if (baseFile.getParent() != null) {
            return Paths.get(baseFile.getParent() + SEPARATOR + IMPORT_SCRIPT_DIRECTORY_NAME);
        }

        return null;
    }

    public String getScript() {
        return script;
    }

    private void evalScript() {
        try (final Reader reader = getReader()) {
            scriptEngine.eval(reader);
        } catch (final ScriptException | IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public String processMemo(final String memo) {
        try {
            final Invocable invocable = (Invocable) scriptEngine;

            final Object result = invocable.invokeFunction("processMemo", memo);

            return result.toString();
        } catch (final ScriptException | NoSuchMethodException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return memo;
    }

    public String processPayee(final String payee) {
        try {
            final Invocable invocable = (Invocable) scriptEngine;

            final Object result = invocable.invokeFunction("processPayee", payee);

            return result.toString();
        } catch (final ScriptException | NoSuchMethodException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return payee;
    }

    public String getDescription() {
        try {
            final Invocable invocable = (Invocable) scriptEngine;

            final Object result = invocable.invokeFunction("getDescription", Locale.getDefault());

            return result.toString();
        } catch (final ScriptException | NoSuchMethodException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return "";
    }

    public void acceptTransaction(final ImportTransaction importTransaction) {
        try {
            final Invocable invocable = (Invocable) scriptEngine;

            invocable.invokeFunction("acceptTransaction", importTransaction);

        } catch (final ScriptException | NoSuchMethodException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private Reader getReader() throws IOException {
        if (Files.exists(Paths.get(script))) {
            return Files.newBufferedReader(Paths.get(script));
        }

        return new InputStreamReader(
                Objects.requireNonNull(ImportFilter.class.getResourceAsStream(script)), StandardCharsets.UTF_8);
    }
}
