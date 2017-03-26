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
package jgnash.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;

/**
 * File utilities.
 *
 * @author Craig Cavanaugh
 */
public final class FileUtils {

    /**
     * Regular expression for returning the file extension.
     */
    private final static String FILE_EXT_REGEX = "(?<=\\.).*$";

    private static final Pattern FILE_EXTENSION_SPLIT_PATTERN = Pattern.compile("\\.");

    private static final String[] FILE_LOCK_EXTENSIONS = new String[]{JpaHsqlDataStore.LOCK_EXT,
            JpaH2DataStore.LOCK_EXT, ".lock"};

    public static final String separator = System.getProperty("file.separator");

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private FileUtils() {
    }

    /**
     * Deletes a path and it's contents.
     *
     * @param path {@code Path} to delete
     * @throws IOException thrown if an IO error occurs
     */
    public static void deletePathAndContents(final Path path) throws IOException {
        if (Files.exists(path)) {   // only try if it exists

            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Determines if a file has been locked for use. A lock file check is performed
     * at the filesystem level and the actual file is checked for a locked state at the OS level.
     *
     * @param fileName file name to check for locked state
     * @return true if a lock file is found or the file is locked at the OS level.
     * @throws java.io.IOException thrown if file does not exist or it is a directory
     */
    public static boolean isFileLocked(final String fileName) throws IOException {

        boolean result = false;

        for (final String extension : FILE_LOCK_EXTENSIONS) {
            if (Files.exists(Paths.get(FileUtils.stripFileExtension(fileName) + extension))) {
                result = true;
            }
        }

        if (!result) {
            try (final FileChannel channel = FileChannel.open(Paths.get(fileName), StandardOpenOption.READ,
                    StandardOpenOption.WRITE)) {

                try (final FileLock lock = channel.tryLock()) {
                    if (lock == null) {
                        result = true;
                    }
                } catch (final OverlappingFileLockException ex) {   // indicates file is already locked
                    result = true;
                }
            } catch (final IOException e) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, e.toString(), e);
                throw e;
            }
        }

        return result;
    }

    /**
     * Strips the extension off of the supplied filename including the period. If the supplied
     * filename does not contain an extension then the original is returned
     *
     * @param fileName filename to strip the extension off
     * @return filename with extension removed
     */
    public static String stripFileExtension(final String fileName) {
        return FILE_EXTENSION_SPLIT_PATTERN.split(fileName)[0];
    }

    /**
     * Determine if the supplied file name has an extension.
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
    public static boolean copyFile(final Path src, final Path dst) {

        boolean result = false;

        if (src != null && dst != null && !src.equals(dst)) {
            try {
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                result = true;
            } catch (IOException e) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, e);
            }

        }

        return result;
    }

    public static void compressFile(@NotNull final Path source, @NotNull final Path destination) {

        // Create the destination directory if needed
        if (!Files.isDirectory(destination.getParent())) {
            try {
                Files.createDirectories(destination.getParent());
                Logger.getLogger(FileUtils.class.getName()).info("Created directories");
            } catch (IOException ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            }
        }

        // Try to open the zip file for output
        try (final OutputStream fos = new BufferedOutputStream(Files.newOutputStream(destination));
             final ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            // Try to open the input stream
            try (final InputStream in = new BufferedInputStream(Files.newInputStream(source, StandardOpenOption.READ))) {
                zipOut.setLevel(Deflater.BEST_COMPRESSION);

                // strip the path when creating the zip entry
                zipOut.putNextEntry(new ZipEntry(source.getFileName().toString()));

                // Transfer bytes from the file to the ZIP file
                int length;


                byte[] ioBuffer = new byte[DEFAULT_BUFFER_SIZE];

                while ((length = in.read(ioBuffer)) > 0) {
                    zipOut.write(ioBuffer, 0, length);
                }

                // finish the zip entry, but let the try-with-resources handle the close
                zipOut.finish();
            }
        } catch (final IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Returns a sorted list of files in a specified directory that match a regex search pattern.
     *
     * @param directory base directory for the search
     * @param regexPattern   regex search pattern
     * @return a List of matching Files. The list will be empty if no matches
     * are found or if the directory is not valid.
     */
    public static List<Path> getDirectoryListing(final Path directory, final String regexPattern) {
        final List<Path> fileList = new ArrayList<>();

        if (directory != null && Files.isDirectory(directory)) {
            final Pattern p = Pattern.compile(regexPattern);

            try {
                fileList.addAll(Files.list(directory).filter(path -> p.matcher(path.toString()).matches())
                        .collect(Collectors.toList()));
            } catch (final IOException ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            }

            Collections.sort(fileList); // sort in natural order
        }

        return fileList;
    }

}
