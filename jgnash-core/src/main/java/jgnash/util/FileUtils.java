/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaH2MvDataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;
import jgnash.engine.xstream.XMLDataStore;

import static jgnash.util.LogUtil.logSevere;

/**
 * File utilities.
 *
 * @author Craig Cavanaugh
 */
public final class FileUtils {

    /**
     * Regular expression for returning the file extension.
     */
    private static final String FILE_EXT_REGEX = "(?<=\\.).*$";

    private static final String[] FILE_LOCK_EXTENSIONS = new String[]{JpaHsqlDataStore.LOCK_EXT,
            JpaH2DataStore.LOCK_EXT, JpaH2MvDataStore.LOCK_EXT, ".lock"};

    private static final String[] FILE_EXTENSIONS = new String[]{JpaH2DataStore.H2_FILE_EXT,
            JpaH2MvDataStore.MV_FILE_EXT, JpaHsqlDataStore.FILE_EXT, XMLDataStore.FILE_EXT};

    public static final String SEPARATOR = System.getProperty("file.separator");

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    // HSQLDB ~10.10 seconds
    // H2 database ~2.0 seconds
    private static final long LOCK_FILE_PERIOD = 11000;

    private static final int LOCK_WAIT_PERIOD = 20;

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

            Files.walkFileTree(path, new SimpleFileVisitor<>() {
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
                logSevere(FileUtils.class, e);
                throw e;
            }
        }

        return result;
    }

    /**
     * Determines if a lock file is stale / leftover from a crash
     *
     * @param fileName database file/lockfile to test
     * @return true if the lock file is determined to be stale
     * @throws IOException thrown if file does not exist or it is a directory
     */
    public static boolean isLockFileStale(final String fileName) throws IOException {
        boolean result = false;

        final long maxIterations = LOCK_FILE_PERIOD / LOCK_WAIT_PERIOD;

        search:
        for (final String extension : FILE_LOCK_EXTENSIONS) {

            final Path path = Paths.get(FileUtils.stripFileExtension(fileName) + extension);

            if (Files.exists(path)) {

                for (int i = 0; i < maxIterations; i++) {

                    long timeStamp = lastModified(path);

                    if (System.currentTimeMillis() - timeStamp > LOCK_FILE_PERIOD) {
                        result = true;
                        break search;
                    }

                    try {
                        Thread.sleep(LOCK_WAIT_PERIOD);
                    } catch (final InterruptedException e) {
                        logSevere(FileUtils.class, e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return result;
    }

    public static boolean deleteLockFile(final String fileName) throws IOException {
        boolean result = false;

        for (final String extension : FILE_LOCK_EXTENSIONS) {
            final Path path = Paths.get(FileUtils.stripFileExtension(fileName) + extension);

            if (Files.exists(path)) {
                Files.delete(path);
                result = true;
            }
        }

        return result;
    }

    /**
     * Strips the file extensions off the supplied filename including the period.
     * <p>
     * Known @code(DataStore} extensions are checked first prior to assuming a simple file extension.
     * If the supplied filename does not contain an extension ending with a period, the original is returned.
     *
     * @param fileName filename to strip the extension off
     * @return filename with extension removed
     */
    public static String stripFileExtension(@NotNull final String fileName) {

        // check for known types first
        for (final String extension : FILE_EXTENSIONS) {
            int index = fileName.toLowerCase().lastIndexOf(extension.toLowerCase());

            if (index >= 0) {
                return fileName.substring(0, index);
            }
        }

        int index = fileName.lastIndexOf('.');

        if (index >= 0) {
            return fileName.substring(0, index);
        }

        return fileName;
    }

    /**
     * Returns the modification time of a file
     *
     * @param path file to check
     * @return last modification timestamp in millis
     * @throws IOException thrown if file is not found
     */
    private static long lastModified(final Path path) throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
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

    /**
     * Returns the file extension if it has one
     * jGnash specific file extensions are check first.
     *
     * @param fileName file name to extract extension from
     * @return file extension or an empty string if not found.
     */
    public static String getFileExtension(final String fileName) {
        Objects.requireNonNull(fileName);

        for (final String extension : FILE_EXTENSIONS) {
            if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
                return extension.substring(1);  // skip the leading period
            }
        }

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
            } catch (final IOException e) {
                logSevere(FileUtils.class, e);
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
            } catch (final IOException e) {
                logSevere(FileUtils.class, e);
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
        } catch (final IOException e) {
            logSevere(FileUtils.class, e);
        }
    }

    /**
     * Returns a sorted list of files in a specified directory that match a regex search pattern.
     *
     * @param directory    base directory for the search
     * @param regexPattern regex search pattern
     * @return a List of matching Files. The list will be empty if no matches
     * are found or if the directory is not valid.
     */
    public static List<Path> getDirectoryListing(final Path directory, final String regexPattern) {
        final List<Path> fileList = new ArrayList<>();

        if (directory != null && Files.isDirectory(directory)) {
            final Pattern p = Pattern.compile(regexPattern);

            try (final Stream<Path> stream = Files.list(directory)) {
                fileList.addAll(stream.filter(path -> p.matcher(path.toString()).matches())
                                        .collect(Collectors.toList()));
            } catch (IOException e) {
                logSevere(FileUtils.class, e);
            }

            Collections.sort(fileList); // sort in natural order
        }

        return fileList;
    }

}
