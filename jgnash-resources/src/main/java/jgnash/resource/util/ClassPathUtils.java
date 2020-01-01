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
package jgnash.resource.util;

import java.net.URL;
import java.util.Locale;

/**
 * Classpath utilities
 *
 * @author Craig Cavanaugh
 */
public class ClassPathUtils {

    private static final String DEFAULT = "en";

//    /** Get a set of file names with the specified extension in the specified package path
//     *
//     * @param packageName   package path
//     * @param fileExtension file extension
//     * @return Set of file names
//     */
//    public static Set<String> getFilesByExtension(final String packageName, final String fileExtension) {
//
//        Set<String> fileSet = new HashSet<String>();
//
//        // Translate the package name into an absolute path
//        String name = packageName;
//        if (!name.startsWith("/")) {
//            name = "/" + name;
//        }
//
//        name = name.replace('.', '/');
//
//        // Get a File object for the package
//        URL url = new ClassPathUtils().getClass().getResource(name);
//
//        File directory = new File(url.getFile());
//
//        if (directory.isDirectory()) {
//            for (String file : directory.list()) {
//                if (file.endsWith(fileExtension)) {
//                    fileSet.add(file);
//                }
//            }
//        }
//
//        return fileSet;
//    }

    /**
     * Find the best localized path given a root
     *
     * @param rootPath root path to start search
     * @return the path for reading the resource, or null if the resource could not be found
     */
    public static String getLocalizedPath(final String rootPath) {

        final Locale locale = Locale.getDefault();

        // Try the language, country, and variant first
        String lang = locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();

        URL url = ClassPathUtils.class.getResource(rootPath + "/" + lang);

        if (url != null) {
            return rootPath + "/" + lang;
        }

        // Try the language and country first
        lang = locale.getLanguage() + "_" + locale.getCountry();

        url = ClassPathUtils.class.getResource(rootPath + "/" + lang);
        if (url != null) {
            return rootPath + "/" + lang;
        }

        // Try just the language now
        lang = locale.getLanguage();

        url = ClassPathUtils.class.getResource(rootPath + "/" + lang);
        if (url != null) {
            return rootPath + "/" + lang;
        }

        // Just use the default locale
        url = ClassPathUtils.class.getResource(rootPath + "/" + DEFAULT);
        if (url != null) {
            return rootPath + "/" + DEFAULT;
        }

        return null;
    }

    private ClassPathUtils() {
        // Utility class
    }
}
