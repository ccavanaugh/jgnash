/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.util;

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * File utilities
 *
 * @author Craig Cavanaugh
 */
public final class FileUtils {

    /**
     * Regular expression for returning the file extension
     */
    private final static String FILE_EXT_REGEX = "(?<=\\.).*$";

    private static final Pattern FILE_EXTENSION_SPLIT_PATTERN = Pattern.compile("\\.");

    public static final String[] FILE_LOCK_EXTENSIONS = new String[]{JpaHsqlDataStore.LOCK_EXT, JpaH2DataStore.LOCK_EXT, ".lock"};

    private FileUtils() {
    }

    /**
     * Determines if a file has been locked for use. A lock file check is performed
     * at the filesystem level and the actual file is checked for a locked state at the OS level.
     *
     * @param fileName file name to check for locked state
     * @return true if a lock file is found or the file is locked at the OS level.
     * @throws java.io.FileNotFoundException thrown if file does not exist
     */
    public static boolean isFileLocked(final String fileName) throws FileNotFoundException {

        boolean result = false;

        for (final String extension : FILE_LOCK_EXTENSIONS) {
            if (Files.exists(Paths.get(FileUtils.stripFileExtension(fileName) + extension))) {
                result = true;
            }
        }

        if (!result) {
            try (RandomAccessFile raf = new RandomAccessFile(new File(fileName), "rw");
                 FileChannel channel = raf.getChannel()) {

                try (FileLock lock = channel.tryLock()) {
                    if (lock == null) {
                        result = true;
                    }
                }
            } catch (IOException e) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, e.toString(), e);
                result = true;
            }
        }

        return result;
    }

    /**
     * Strips the extension off of the supplied filename. If the supplied
     * filename does not contain an extension then the original is returned
     *
     * @param fileName filename to strip the extension off
     * @return filename with extension removed
     */
    public static String stripFileExtension(final String fileName) {
        return FILE_EXTENSION_SPLIT_PATTERN.split(fileName)[0];
    }

    /**
     * Determine if the supplied file name has an extension
     *
     * @param fileName filename to check
     * @return true if supplied file has an extension
     */
    public static boolean fileHasExtension(final String fileName) {
        return !stripFileExtension(fileName).equals(fileName);
    }

    public static String getFileExtension(final String fileName) {

        String result = "";

        final Pattern pattern = Pattern.compile(FILE_EXT_REGEX);
        final Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            result = matcher.group();
        }

        return result;
    }

    /**
     * Make a copy of a file given a source and destination.
     *
     * @param src Source file
     * @param dst Destination file
     * @return true if the copy was successful
     */
    public static boolean copyFile(final File src, final File dst) {

        boolean result = false;

        if (src != null && dst != null && !src.equals(dst)) {
            try {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                result = true;
            } catch (IOException e) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, e);
            }

        }

        return result;
    }

    public static void compressFile(final File source, final File destination) {

        // Try to open the zip file for output
        try (FileOutputStream fos = new FileOutputStream(destination);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            // Try to obtain the lock on the output stream
            try (FileLock fosLock = fos.getChannel().tryLock()) {

                if (fosLock != null) {

                    // Try to open the input stream
                    try (FileInputStream in = new FileInputStream(source)) {

                        // Try to lock input stream
                        try (FileLock fisLock = in.getChannel().tryLock(0L, Long.MAX_VALUE, true)) {

                            if (fisLock != null) {
                                zipOut.setLevel(Deflater.BEST_COMPRESSION);

                                // strip the path when creating the zip entry
                                zipOut.putNextEntry(new ZipEntry(source.getName()));

                                // Transfer bytes from the file to the ZIP file
                                int length;

                                byte[] ioBuffer = new byte[8192];

                                while ((length = in.read(ioBuffer)) > 0) {
                                    zipOut.write(ioBuffer, 0, length);
                                }

                                // finish the zip entry, but let the try-with-resources handle the close
                                zipOut.finish();

                                fosLock.release();
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Returns a sorted list of files in a specified directory that match a DOS
     * style wildcard search pattern.
     *
     * @param directory base directory for the search
     * @param pattern   DOS search pattern
     * @return a List of matching Files. The list will be empty if no matches
     *         are found or if the directory is not valid.
     */
    public static List<File> getDirectoryListing(final File directory, final String pattern) {
        List<File> fileList = new ArrayList<>();

        if (directory != null && directory.isDirectory()) {

            final Pattern p = SearchUtils.createSearchPattern(pattern, false);

            File[] files = directory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return p.matcher(name).matches();
                }
            });

            fileList.addAll(Arrays.asList(files));

            Collections.sort(fileList); // sort in natural order
        }

        return fileList;
    }
}
