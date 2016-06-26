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
package jgnash.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin Factory methods.
 *
 * @author Leif-Erik Dörr
 * @author Craig Cavanaugh
 */
public final class PluginFactory {

    private static String pluginDirectory = null;
    private final static String PLUGIN_DIRECTORY_NAME = "plugins";
    private final static BigDecimal INTERFACE_VERSION = new BigDecimal("2.5");
    private static final Logger logger = Logger.getLogger(PluginFactory.class.getName());

    private static final List<Plugin> plugins = new ArrayList<>();
    private static boolean pluginsStarted = false;
    private static boolean pluginsLoaded = false;

    static {
        pluginDirectory = getPluginDirectory();
    }

    private PluginFactory() {
        // Utility class
    }

    public static List<Plugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    private static synchronized String getPluginDirectory() {
        if (pluginDirectory == null) {
            pluginDirectory = PluginFactory.class.getProtectionDomain().getCodeSource().getLocation().getPath();

            // decode to correctly handle spaces, etc. in the returned path
            try {
                pluginDirectory = URLDecoder.decode(pluginDirectory, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException ex) {
                logger.log(Level.SEVERE, null, ex);
            }

            pluginDirectory = new File(pluginDirectory).getParent();
            pluginDirectory += File.separator + PLUGIN_DIRECTORY_NAME + File.separator;

            logger.log(Level.INFO, "Plugin path: {0}", pluginDirectory);
        }

        return pluginDirectory;
    }

    public static void startPlugins() {
        if (!pluginsStarted) {

            for (Plugin plugin : plugins) {
                logger.log(Level.INFO, "Starting plugin: {0}", plugin.getName());
                plugin.start();
            }

            pluginsStarted = true;
        }
    }

    public static void stopPlugins() {
        if (pluginsStarted) {
            for (final Plugin plugin : plugins) {
                logger.log(Level.INFO, "Stopping plugin: {0}", plugin.getName());
                plugin.stop();
            }

            pluginsStarted = false;
        }
    }

    public static void loadPlugins() {
        if (!pluginsLoaded) {
            final String[] paths = getPluginPaths();

            if (paths != null) {
                for (final String plugin : paths) {
                    try {
                        final Plugin p = loadPlugin(plugin);
                        if (p != null) {
                            plugins.add(p);
                        }
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                logger.info("Did not find any plugins");
            }

            pluginsLoaded = true;
        }
    }

    private static String[] getPluginPaths() {
        final File dir = new File(getPluginDirectory());
        return dir.list(new PluginFilenameFilter());
    }

    private static Plugin loadPlugin(final String jarFileName) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException {

        // If classLoader is closed, the plugin is not able to load new classes...
        final JarURLClassLoader classLoader
                = new JarURLClassLoader(new URL("file:///" + getPluginDirectory() + jarFileName));

        final String pluginActivator = classLoader.getActivator();

        Plugin plugin = null;

        if (pluginActivator != null) {
            final Object object = classLoader.loadClass(pluginActivator).newInstance();

            if (object instanceof Plugin) {
                plugin = (Plugin) object;
            }
        } else {
            logger.log(Level.SEVERE, "''{0}'' Plugin Interface was not implemented", jarFileName);
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

                if (version != null && INTERFACE_VERSION.compareTo(version) >= 0) {
                    activator = attr.getValue(PLUGIN_ACTIVATOR);
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
