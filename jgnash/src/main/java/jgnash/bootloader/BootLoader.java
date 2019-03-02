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
package jgnash.bootloader;

import javax.xml.bind.DatatypeConverter;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Boot loader used to download platform specific OpenJFX files and place them in the LIB directory.
 *
 * @author Craig Cavanaugh
 */
public class BootLoader {

    private static final String JFX_VERSION = "11.0.2";

    private static final String MAVEN_REPO = "http://central.maven.org/maven2/org/openjfx/";

    private static final String FILE_PATTERN = "javafx-{0}-{1}-{2}.jar";

    private static final String SEPARATOR = System.getProperty("file.separator");

    private static final String OS = getOS();

    private static final String LIB = "lib";

    private static final String USER_DIR = "user.dir";

    public static final int FAILED_EXIT = -1;

    static final int REBOOT_EXIT = 100;

    // download them all
    private static String[] JARS = new String[]{"base", "controls", "fxml", "graphics", "media", "swing", "web"};

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
        return Paths.get(Paths.get(System.getProperty(USER_DIR)).toString() + SEPARATOR + LIB).toString();
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

    public static boolean downloadFiles(Consumer<String> fileNameConsumer, IntConsumer percentCompleteConsumer) {
        percentCompleteConsumer.accept(0);
        boolean result = true;

        final Path lib = Paths.get(BootLoader.getLibPath());

        try {
            Files.createDirectories(lib);   // create the directory if needed

            final String spec = MAVEN_REPO + "javafx-{0}/{1}/" + FILE_PATTERN;
            final String pathSpec = lib.toString() + SEPARATOR + FILE_PATTERN;

            int count = 0;

            for (final String fxJar : JARS) {
                URL url = new URL(MessageFormat.format(spec, fxJar, JFX_VERSION, OS));
                Path path = Paths.get(MessageFormat.format(pathSpec, fxJar, JFX_VERSION, OS));

                if (!Files.exists(path)) {
                    fileNameConsumer.accept(url.toExternalForm());
                    boolean downloadResult = downloadFile(url, path);

                    if (!downloadResult) {
                        result = false;
                        break;
                    }
                }

                count++;
                percentCompleteConsumer.accept((int)(((float)count / (float) JARS.length) * 100));
            }
        } catch (final IOException e) {
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    private static boolean downloadFile(final URL source, final Path dest) throws IOException {
        final Logger logger = Logger.getLogger(BootLoader.class.getName());
        boolean result = true;

        logger.info("Downloading " + source.toExternalForm() + " to " + dest.toString());

        String md5 = "";

        try (final ReadableByteChannel readableByteChannel = Channels.newChannel(source.openStream());
             final FileOutputStream fileOutputStream = new FileOutputStream(dest.toString())) {

            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            // hash the file
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Files.readAllBytes(dest));

            md5 = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            result = false;
        }

        final URL md5Source = new URL(source.toExternalForm() + ".md5");
        final Path md5Dest = Files.createTempFile("", ".md5");

        try (final ReadableByteChannel readableByteChannel = Channels.newChannel(md5Source.openStream());
             final FileOutputStream fileOutputStream = new FileOutputStream(md5Dest.toString())) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }

        try (final BufferedReader reader = Files.newBufferedReader(md5Dest)) {
            final String hash = reader.readLine();

            if (hash.compareTo(md5) != 0) {
                Files.delete(dest);
                logger.severe("Download of " + dest.toString() + " was corrupt; removing the file");
                result = false;
            }

            Files.delete(md5Dest);
        }

        return result;
    }
}
