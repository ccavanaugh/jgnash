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
package jgnash.bootloader;

import jgnash.util.NotNull;

import javax.swing.JOptionPane;
import javax.xml.bind.DatatypeConverter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.logging.Logger;

/**
 * Boot loader used to download platform specific OpenJFX files and place them in the LIB directory.
 *
 * @author Craig Cavanaugh
 */
public class BootLoader {

    private static final String JFX_VERSION = "15.0.1";

    private static final String MAVEN_REPO = "https://repo1.maven.org/maven2/org/openjfx/";

    private static final String FILE_PATTERN = "javafx-{0}-{1}-{2}.jar";

    private static final String SEPARATOR = System.getProperty("file.separator");

    private static final String OS = getOS();

    public static final int FAILED_EXIT = -1;

    static final int REBOOT_EXIT = 100;

    private static final int BUFFER_SIZE = 4096;

    // download them all
    private static final String[] JARS = new String[]{"base", "controls", "fxml", "graphics", "media", "swing", "web"};

    public static boolean doFilesExist() {
        String libPath = getLibPath();

        boolean result = true;

        final String pathSpec = libPath + SEPARATOR + FILE_PATTERN;

        for (final String fxJar : JARS) {
            Path path = Paths.get(MessageFormat.format(pathSpec, fxJar, JFX_VERSION, OS));
            if (!Files.exists(path)) {
                result = false;
                break;
            }
        }

        return result;
    }

    private static String getLibPath() {
        // Current class lives in a .jar that lives in the lib folder we want to populate.  Let's
        // use that to find the lib path rather than depend on jGnash being invoked from the correct
        // folder.
        // https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file?noredirect=1&lq=1
        try {
            return new File(BootLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                           .getParentFile().getPath();
        } catch (final URISyntaxException e) {
            throw new RuntimeException("Unable to determine lib path fpr bootloader");
        }
    }

    public static String getOS() {
        final String os = System.getProperty("os.name");

        if (os.startsWith("Linux")) {
            return "linux";
        } else if (os.startsWith("Windows")) {
            return "win";
        } else if (os.startsWith("Darwin") || os.startsWith("Mac")) {
            return "mac";
        }

        return null;
    }

    public static boolean downloadFiles(final Consumer<String> fileNameConsumer, final IntConsumer percentCompleteConsumer) {
        percentCompleteConsumer.accept(0);
        boolean result = true;

        final Path lib = Paths.get(BootLoader.getLibPath());

        try {
            final long completeDownloadSize = getTotalDownloadSize();

            Files.createDirectories(lib);   // create the directory if needed

            final String spec = MAVEN_REPO + "javafx-{0}/{1}/" + FILE_PATTERN;
            final String pathSpec = lib + SEPARATOR + FILE_PATTERN;

            final LongConsumer countConsumer = new LongConsumer() {
                long totalCounts;

                @Override
                public void accept(final long value) {
                    totalCounts += value;
                    percentCompleteConsumer.accept((int) (((double) totalCounts / (double) completeDownloadSize) * 100));
                }
            };

            for (final String fxJar : JARS) {
                URL url = new URL(MessageFormat.format(spec, fxJar, JFX_VERSION, OS));
                Path path = Paths.get(MessageFormat.format(pathSpec, fxJar, JFX_VERSION, OS));

                if (!Files.exists(path)) {
                    fileNameConsumer.accept(url.toExternalForm());

                    try {
                        boolean downloadResult = downloadFile(url, path, countConsumer);

                        if (!downloadResult) {
                            result = false;
                            break;
                        }
                    } catch (final IOException e) {
                        e.printStackTrace();
                        showException(e);
                    }
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    private static long getTotalDownloadSize() throws MalformedURLException {
        long size = 0;

        for (String fxJar : JARS) {
            final String spec = MAVEN_REPO + "javafx-{0}/{1}/" + FILE_PATTERN;
            URL url = new URL(MessageFormat.format(spec, fxJar, JFX_VERSION, OS));

            size += getFileDownloadSize(url);
        }

        return size;
    }

    private static long getFileDownloadSize(final URL source) {
        long length = 0;

        try {
            final HttpURLConnection httpConnection = (HttpURLConnection) (source.openConnection());
            length = httpConnection.getContentLength();

            httpConnection.disconnect();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return length;
    }

    private static void showException(final Exception exception) {
        final String message = exception.getMessage() + "\nStackTrace: " + Arrays.toString(exception.getStackTrace());
        final String title = exception.getClass().getName();

        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static boolean downloadFile(final URL source, final Path dest, final LongConsumer countConsumer) throws IOException {
        final Logger logger = Logger.getLogger(BootLoader.class.getName());
        boolean result = true;

        logger.info("Downloading " + source.toExternalForm() + " to " + dest.toString());

        String md5 = "";

        HttpURLConnection httpConnection = (HttpURLConnection) (source.openConnection());

        try (final BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
             final BufferedOutputStream bout = new BufferedOutputStream(new CountingFileOutputStream(dest.toString(),
                     countConsumer), BUFFER_SIZE)) {

            byte[] data = new byte[BUFFER_SIZE];

            int x;
            while ((x = in.read(data, 0, BUFFER_SIZE)) >= 0) {
                bout.write(data, 0, x);
            }
            bout.close();

            // hash the file
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Files.readAllBytes(dest));

            md5 = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        } catch (final Exception e) {
            e.printStackTrace();
            showException(e);
            result = false;
        }

        final URL md5Source = new URL(source.toExternalForm() + ".md5");
        final Path md5Dest = Files.createTempFile("", ".md5");

        try (final ReadableByteChannel readableByteChannel = Channels.newChannel(md5Source.openStream());
             final FileOutputStream fileOutputStream = new FileOutputStream(md5Dest.toString())) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (final Exception e) {
            e.printStackTrace();
            showException(e);
        }

        try (final BufferedReader reader = Files.newBufferedReader(md5Dest)) {
            final String hash = reader.readLine();

            if (hash.compareTo(md5) != 0) {
                Files.delete(dest);
                logger.severe("Download of " + dest + " was corrupt; removing the file");
                result = false;
            }

            Files.delete(md5Dest);
        } catch (final Exception e) {
            e.printStackTrace();
            showException(e);
        }

        return result;
    }

    /**
     * Updates a supplied consumer with the number of bytes written
     */
    private static class CountingFileOutputStream extends FileOutputStream {

        final LongConsumer countConsumer;

        CountingFileOutputStream(final String name, @NotNull final LongConsumer consumer) throws FileNotFoundException {
            super(name);
            this.countConsumer = consumer;
        }

        @Override
        public void write(final int idx) throws IOException {
            super.write(idx);
            countConsumer.accept(1);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            super.write(b);
            countConsumer.accept(b.length);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            super.write(b, off, len);
            countConsumer.accept(len);
        }
    }
}
