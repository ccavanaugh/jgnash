/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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
 * @version $Id: FileUtils.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public final class FileUtils {

    /** Regular expression for returning the file extension */
    private final static String FILEEXTREGEX = "(?<=\\.).*$";

    private static final Pattern FILE_EXTENSION_SPLIT_PATTERN = Pattern.compile("\\.");

    private FileUtils() {
    }

    /**
     * Determines if a file has been locked for use. The lock check is performed at the OS level.
     *
     * @param fileName file name to check for locked state
     * @return true if the file is locked at the OS level.
     * @throws java.io.FileNotFoundException thrown if file does not exist
     */
    public static boolean isFileLocked(final String fileName) throws FileNotFoundException {

        boolean result = true;

        final RandomAccessFile raf = new RandomAccessFile(new File(fileName), "rw");

        FileChannel channel = null;

        try {
            channel = raf.getChannel();
            final FileLock lock = channel.tryLock();

            if (lock != null) {
                lock.release();
                result = false;
            }
        } catch (IOException e) {
            result = true;
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            }

            try {
                raf.close();
            } catch (IOException e) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }

        return result;
    }

    /**
     * Strips the extension off of the supplied filename. If the supplied filename does not contain an extension then
     * the original is returned
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

        final Pattern pattern = Pattern.compile(FILEEXTREGEX);
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
     * @throws java.io.FileNotFoundException thrown if the src file is not found or if the dst file is not a valid filename
     * @return true is the copy was successful
     */
    public static boolean copyFile(final File src, final File dst) throws FileNotFoundException {

        if (src == null || dst == null || src.equals(dst)) {
            return false;
        }

        boolean result = false;

        if (src.exists()) {
            if (dst.exists() && !dst.canWrite()) {
                return false;
            }

            final FileInputStream srcStream = new FileInputStream(src);
            final FileOutputStream dstStream = new FileOutputStream(dst);

            final FileChannel srcChannel = srcStream.getChannel();
            final FileChannel dstChannel = dstStream.getChannel();

            FileLock dstLock = null;
            FileLock srcLock = null;

            try {

                srcLock = srcChannel.tryLock(0, Long.MAX_VALUE, true);
                dstLock = dstChannel.tryLock();

                if (srcLock != null && dstLock != null) { // only copy if locks are obtained
                    // magic number for Windows, 64Mb - 32Kb)
                    int maxCount = 64 * 1024 * 1024 - 32 * 1024;
                    long size = srcChannel.size();
                    long position = 0;
                    while (position < size) {
                        position += srcChannel.transferTo(position, maxCount, dstChannel);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (srcChannel != null) {
                    try {
                        if (srcLock != null) {
                            srcLock.release();
                        }
                        srcChannel.close();
                        srcStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (dstChannel != null) {
                    try {
                        if (dstLock != null) {
                            dstLock.release();
                        }
                        dstChannel.close();
                        dstStream.close();
                        result = true;
                    } catch (IOException ex) {
                        Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return result;
    }

    public static void compressFile(final File source, final File destination) {

        byte[] ioBuffer = new byte[8192];

        FileInputStream in = null;

        FileLock fosLock;
        FileLock fisLock = null;

        try {
            // Create the ZIP file
            FileOutputStream fos = new FileOutputStream(destination);
            fosLock = fos.getChannel().tryLock();

            in = new FileInputStream(source);
            fisLock = in.getChannel().tryLock(0, Long.MAX_VALUE, true);

            if (fosLock != null && fisLock != null) {
                ZipOutputStream zipOut = new ZipOutputStream(fos);

                zipOut.setLevel(Deflater.BEST_COMPRESSION);

                // strip the path when creating the zip entry
                zipOut.putNextEntry(new ZipEntry(source.getName()));

                // Transfer bytes from the file to the ZIP file
                int length;

                while ((length = in.read(ioBuffer)) > 0) {
                    zipOut.write(ioBuffer, 0, length);
                }

                // Complete the entry
                zipOut.closeEntry();

                fosLock.release();

                // Complete the ZIP file
                zipOut.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (in != null) {
                try {
                    if (fisLock != null) {
                        fisLock.release();
                    }
                    in.close();
                } catch (IOException e) {
                    Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    /**
     * Returns a sorted list of files in a specified directory that match a DOS style wildcard search pattern.
     *
     * @param directory base directory for the search
     * @param pattern   DOS search pattern
     * @return a List of matching Files.  The list will be empty if no matches are found or if the
     *         directory is not valid.
     */
    public static List<File> getDirectoryListing(final File directory, final String pattern) {
        List<File> fileList = new ArrayList<File>();

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
