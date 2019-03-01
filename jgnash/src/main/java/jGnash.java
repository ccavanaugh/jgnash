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
import java.util.logging.Logger;

/**
 * Bootstrap a modular jGnashFx by downloading platform specific OpenJFX libraries and then launching the application
 *
 * @author Craig Cavanaugh
 */
public class jGnash {

    private static final String JFX_VERSION = "11.0.2";

    private static final String MAVEN_REPO = "http://central.maven.org/maven2/org/openjfx/";

    // download them all
    private static String[] jars = new String[] {"base", "controls", "fxml", "graphics", "media", "swing", "web"};

    public static void main(final String[] args) {
        final String separator = System.getProperty("file.separator");
        final String os = getOS();
        final Path lib = Paths.get(Paths.get(System.getProperty("user.dir")).toString() + separator + "lib");

        // TODO, do not download to the wrong directory if launched within the IDE

        if (os != null) {
            try {
                Files.createDirectories(lib);   // create the directory if needed

                final String spec = MAVEN_REPO + "javafx-{0}/{1}/javafx-{0}-{1}-{2}.jar";
                final String pathSpec = lib.toString() + separator + "javafx-{0}-{1}-{2}.jar";

                for (final String fxJar : jars) {
                    URL url = new URL(MessageFormat.format(spec, fxJar, JFX_VERSION, os));
                    Path path = Paths.get(MessageFormat.format(pathSpec, fxJar, JFX_VERSION, os));

                    if (!Files.exists(path)) {
                        downloadFile(url, path);
                    }
                }

            } catch (final IOException e) {
                e.printStackTrace();
            }

            launch(args);
        } else {
            Logger.getLogger(jGnash.class.getName()).severe("Unsupported Operating System");
        }
    }

    private static String getOS() {
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

    private static void launch(final String[] args) {
        jGnashFx.main(args);
    }

    private static void downloadFile(final URL source, final Path dest) throws IOException {
        final Logger logger = Logger.getLogger(jGnash.class.getName());

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
        }

        final URL md5Source = new URL(source.toExternalForm() + ".md5");
        final Path md5Dest = Files.createTempFile("", ".md5");

        try (final ReadableByteChannel readableByteChannel = Channels.newChannel(md5Source.openStream());
             final FileOutputStream fileOutputStream = new FileOutputStream(md5Dest.toString())) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }

        try (final BufferedReader reader = Files.newBufferedReader(md5Dest)){
            final String hash = reader.readLine();

            if (hash.compareTo(md5) != 0) {
                Files.delete(dest);
                logger.severe("Download of " + dest.toString() + " was corrupt; removing the file");
            }

            Files.delete(md5Dest);
        }
    }
}
