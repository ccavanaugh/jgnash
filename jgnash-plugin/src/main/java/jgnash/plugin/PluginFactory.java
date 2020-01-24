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
package jgnash.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.resource.util.OS;

/**
 * Plugin Factory methods.
 *
 * @author Leif-Erik Dörr
 * @author Craig Cavanaugh
 */
public final class PluginFactory {

    private final static String PLUGIN_DIRECTORY_NAME = "plugins";

    private final static BigDecimal INTERFACE_VERSION = new BigDecimal("2.25");

    private static final String PLUGIN_PATH_MESSAGE = "Plugin path: {0}";

    private static final Logger logger = Logger.getLogger(PluginFactory.class.getName());

    private static final List<Plugin> plugins = new ArrayList<>();

    private static final AtomicBoolean pluginsStarted = new AtomicBoolean(false);

    private static final AtomicBoolean pluginsLoaded = new AtomicBoolean(false);

    private static final String separator = System.getProperty("file.separator");

    private PluginFactory() {
        // Utility class
    }

    public static List<Plugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    private static synchronized String getDefaultPluginDirectory() {

        String pluginDirectory = PluginFactory.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        // decode to correctly handle spaces, etc. in the returned path
        try {
            pluginDirectory = URLDecoder.decode(pluginDirectory, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        // starting path will be the lib directory because that is where jgnash-core lives.

        pluginDirectory = new File(pluginDirectory).getParentFile().getParent();
        pluginDirectory += separator + PLUGIN_DIRECTORY_NAME + separator;

        logger.log(Level.INFO, PLUGIN_PATH_MESSAGE, pluginDirectory);

        return pluginDirectory;
    }

    private static synchronized String getUserPluginDirectory() {

        String pluginDirectory = System.getProperty("user.home");

        // decode to correctly handle spaces, etc. in the returned path
        try {
            pluginDirectory = URLDecoder.decode(pluginDirectory, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        if (OS.isSystemWindows()) {
            pluginDirectory += separator + "AppData" + separator + "Local" + separator
                    + "jgnash" + separator + PLUGIN_DIRECTORY_NAME + separator;
        } else { // unix, osx
            pluginDirectory += separator + ".jgnash" + separator + PLUGIN_DIRECTORY_NAME + separator;
        }

        logger.log(Level.INFO, PLUGIN_PATH_MESSAGE, pluginDirectory);

        return pluginDirectory;
    }

    public static void startPlugins(final Plugin.PluginPlatform pluginPlatform) {
        if (!pluginsStarted.getAndSet(true)) {
            for (final Plugin plugin : plugins) {
                logger.log(Level.INFO, "Starting plugin: {0}", plugin.getName());
                plugin.start(pluginPlatform);
            }
        }
    }

    public static void stopPlugins() {
        if (pluginsStarted.getAndSet(false)) {
            for (final Plugin plugin : plugins) {
                logger.log(Level.INFO, "Stopping plugin: {0}", plugin.getName());
                plugin.stop();
            }
        }
    }

    /**
     * Loads Plugins.
     *
     * @param predicate Predicate allows filtering and control of loading plugins
     */
    public static void loadPlugins(final Predicate<Plugin> predicate) {
        if (!pluginsLoaded.getAndSet(true)) {
            final List<String> paths = getPluginPaths();

            if (!paths.isEmpty()) {
                for (final String plugin : paths) {
                    try {
                        final Plugin p = loadPlugin(plugin);
                        if (p != null) {
                            if (predicate.test(p)) {
                                plugins.add(p);
                            }
                        }
                    } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException
                            | IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                logger.info("Did not find any plugins");
            }
        }
    }

    private static List<String> getPluginPaths() {
        final List<String> paths = new ArrayList<>();

        getPluginPaths(getDefaultPluginDirectory(), paths);
        getPluginPaths(getUserPluginDirectory(), paths);

        return paths;
    }

    private static void getPluginPaths(final String root, final List<String> paths) {
        final String[] files = new File(root).list(new PluginFilenameFilter());

        if (files != null) {
            for (final String file : files) {
                paths.add(root + file);
            }
        }
    }

    @SuppressWarnings("resource")
	private static Plugin loadPlugin(final String jarFileName) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException {
       
        final JarURLClassLoader classLoader = new JarURLClassLoader(new URL("file:///" + jarFileName));
        
        // Add a shutdown hook to properly close the classLoader.  It needs to remain open for the duration of 
        // the application otherwise the plugin will not be able to load any needed classes.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                classLoader.close();
            } catch (final IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }));

        final String pluginActivator = classLoader.getActivator();

        Plugin plugin = null;

        try {

            if (pluginActivator != null) {
                final Object object = classLoader.loadClass(pluginActivator).getDeclaredConstructor().newInstance();

                if (object instanceof Plugin) {
                    plugin = (Plugin) object;
                }
            } else {
                logger.log(Level.SEVERE, "''{0}'' Plugin Interface was not implemented", jarFileName);
            }
        } catch (final NoClassDefFoundError | NoSuchMethodException | InvocationTargetException e) {
            // This is expected when a Swing instance tries to load a Fx instance of a plugin
            logger.log(Level.INFO, "Plugin type was not compatible; not loaded: " + jarFileName);
        }

        return plugin;
    }

    private static class JarURLClassLoader extends URLClassLoader {

        private static final String PLUGIN_ACTIVATOR = "Plugin-Activator";

        private static final String PLUGIN_VERSION = "Plugin-Version";

        JarURLClassLoader(final URL url) {
            super(new URL[]{url});
        }

        String getActivator() throws IOException {

            String activator = null;

            final URL u = new URL("jar", "", getURLs()[0] + "!/");
            final JarURLConnection uc = (JarURLConnection) u.openConnection();
            final Attributes attr = uc.getMainAttributes();

            if (attr != null) {

                BigDecimal version = null;
                try {
                    String value = attr.getValue(PLUGIN_VERSION);

                    if (value != null) {
                        version = new BigDecimal(attr.getValue(PLUGIN_VERSION));
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }

                if (version != null && INTERFACE_VERSION.compareTo(version) == 0) {
                    activator = attr.getValue(PLUGIN_ACTIVATOR);
                } else {
                    logger.log(Level.WARNING, "Plugin version not compatible; not loaded: "
                            + attr.getValue(PLUGIN_ACTIVATOR));
                }
            }

            return activator;
        }
    }

    private static class PluginFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".jar");
        }
    }
}
