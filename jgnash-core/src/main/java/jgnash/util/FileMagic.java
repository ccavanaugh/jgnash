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
package jgnash.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jgnash.engine.Engine;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.READ;

/**
 * Class to identify file types.
 *
 * @author Craig Cavanaugh
 */
public class FileMagic {

    private static final Pattern COLON_DELIMITER_PATTERN = Pattern.compile(":");

    private static final byte[] BINARY_XSTREAM_HEADER = new byte[]{10, -127, 0, 13, 111, 98, 106, 101, 99, 116, 45,
            115, 116, 114, 101, 97, 109, 11, -127, 10};

    private static final byte[] H2_HEADER = new byte[]{0x2D, 0x2D, 0x20, 0x48, 0x32, 0x20, 0x30, 0x2E, 0x35, 0x2F,
            0x42, 0x20, 0x2D, 0x2D};

    private static final byte[] H2MV_HEADER = new byte[]{72, 58, 50};

    private static final byte[] HSQL_HEADER = "SET DATABASE UNIQUE NAME HSQLDB".getBytes(StandardCharsets.UTF_8);

    private static final byte[] XML_HEADER = "<?xml version=\"1.0\"".getBytes(StandardCharsets.UTF_8);

    private static final String USASCII = "USASCII";

    private static final String WINDOWS_1252 = "windows-1252";

    private static final Charset[] CHARSETS = {UTF_8, US_ASCII, ISO_8859_1, UTF_16, UTF_16BE, UTF_16LE};

    private static final int BUFFER_SIZE = 8192;

    private FileMagic() {
    }

    /**
     * Returns the path type.
     *
     * @param path path to identify
     * @return identified path type
     */
    public static FileType magic(final Path path) {

        if (isValidVersion2File(path)) {
            return FileType.jGnash2XML;
        } else if (isBinaryXStreamFile(path)) {
            return FileType.BinaryXStream;
        } else if (isH2File(path)) {
            return FileType.h2;
        } else if (isH2MvFile(path)) {
            return FileType.h2mv;
        } else if (isHsqlFile(path)) {
            return FileType.hsql;
        } else if (isOfxV1(path)) {
            return FileType.OfxV1;
        } else if (isOfxV2(path)) {
            return FileType.OfxV2;
        }

        return FileType.unknown;
    }

    /**
     * Determine the correct character encoding of an OFX Version 1 file.
     *
     * @param path File to look at
     * @return encoding of the file
     */
    public static String getOfxV1Encoding(final Path path) {

        // Work through common standard Charsets
        try {
            return getOfxV1Encoding(path, StandardCharsets.UTF_8);
        } catch (final MalformedInputException e) {
            try {
                return getOfxV1Encoding(path, ISO_8859_1);
            } catch (final MalformedInputException ex) {
                try {
                    return getOfxV1Encoding(path, US_ASCII);
                } catch (final MalformedInputException exx) {
                    return WINDOWS_1252;
                }
            }
        }
    }

    /**
     * Determine the correct character encoding of an OFX Version 1 file.
     *
     * @param path File to look at
     * @return encoding of the file
     */
    private static String getOfxV1Encoding(final Path path, final Charset charset) throws MalformedInputException {
        String encoding = null;
        String characterSet = null;

        if (Files.exists(path)) {
            try (final BufferedReader reader = Files.newBufferedReader(path, charset)) {
                String line = reader.readLine();

                while (line != null) {
                    line = line.trim();

                    if (!line.isEmpty()) { // allow empty lines at the beginning of the file
                        if (line.startsWith("ENCODING:")) {
                            String[] splits = COLON_DELIMITER_PATTERN.split(line);

                            if (splits.length == 2) {
                                encoding = splits[1];
                            }
                        } else if (line.startsWith("CHARSET:")) {
                            String[] splits = COLON_DELIMITER_PATTERN.split(line);

                            if (splits.length == 2) {
                                characterSet = splits[1];
                            }
                        }

                        if (encoding != null && characterSet != null) {
                            break;
                        }
                    }
                    line = reader.readLine();
                }
            } catch (final MalformedInputException e) {
                throw e;    // rethrow
            } catch (final IOException e) {
                LogUtil.logSevere(FileMagic.class, e);
            }
        }

        if (encoding != null && characterSet != null) {
            if (encoding.equals(StandardCharsets.UTF_8.name()) && characterSet.equals("CSUNICODE")) {
                return ISO_8859_1.name();
            } else if (encoding.equals(StandardCharsets.UTF_8.name())) {
                return StandardCharsets.UTF_8.name();
            } else if (encoding.equals(USASCII) && characterSet.equals("1252")) {
                return WINDOWS_1252;
            } else if (encoding.equals(USASCII) && characterSet.contains("8859-1")) {
                return "ISO-8859-1";
            } else if (encoding.equals(USASCII) && characterSet.equals("NONE")) {
                return WINDOWS_1252;
            }
        }

        return WINDOWS_1252;
    }


    public static boolean isOfxV1(final Path path) {

        boolean result = false;

        if (Files.exists(path)) {

            final String encoding = getOfxV1Encoding(path);
            final Charset charset = Objects.requireNonNull(Charset.forName(encoding));

            try (final BufferedReader reader = Files.newBufferedReader(path, charset)) {
                String line = reader.readLine();

                while (line != null) {

                    line = line.trim(); // time white space.  Some OFX files may contain ugly white space

                    if (!line.isEmpty()) { // allow empty lines at the beginning of the file

                        if (line.contains("OFXHEADER:")) {
                            result = true;
                        }
                        break;
                    }
                    line = reader.readLine();
                }
            } catch (final IOException e) {
                LogUtil.logSevere(FileMagic.class, e);
            }
        }

        return result;
    }

    public static boolean isOfxV2(final Path path) {

        boolean result = false;

        if (Files.exists(path)) {

            try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

                String line = reader.readLine();

                /* Assume that the OFX file is one continuous string of information when searching for OFXHEADER */
                while (line != null) {
                    line = line.trim();

                    // consume any processing instructions and check for ofx 2.0 hints
                    if (!line.isEmpty() && line.startsWith("<?")) {
                        if (line.contains("OFXHEADER=\"200\"")) {
                            result = true;
                            break;
                        }
                    } else if (!line.isEmpty()) { // allow empty lines at the beginning of the file
                        if (line.startsWith("<OFX>")) { //must be ofx
                            result = true;
                        }
                        break;
                    }

                    line = reader.readLine();
                }
            } catch (final MalformedInputException mie) {
                result = false; // caused by binary file
                Logger.getLogger(FileMagic.class.getName()).info("Tried to read a binary file");
            } catch (IOException e) {
                Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }

        return result;
    }

    static boolean isBinaryXStreamFile(final Path path) {
        return isFile(path, BINARY_XSTREAM_HEADER);
    }

    private static boolean isH2File(final Path path) {
        return isFile(path, H2_HEADER);
    }

    private static boolean isH2MvFile(final Path path) {
        return isFile(path, H2MV_HEADER);
    }

    private static boolean isHsqlFile(final Path path) {
        return isFile(path, HSQL_HEADER);
    }

    private static boolean isFile(final Path path, final byte[] header) {
        boolean result = false;

        if (Files.exists(path)) {
            try (final SeekableByteChannel channel = Files.newByteChannel(path, EnumSet.of(READ))) {
                if (channel.size() > 0) { // must not be a zero length file
                    final ByteBuffer buff = ByteBuffer.allocate(header.length);

                    channel.read(buff);
                    result = Arrays.equals(buff.array(), header);
                    buff.clear();
                }
            } catch (final IOException ex) {
                Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }

    /**
     * Determines if this is a valid jGnash XML file.
     * <p>
     * This will fail if the file was created by a future version of the file with a greater major version.
     *
     * @param path path to verify*
     * @return {@code true} if valid.
     */
    private static boolean isValidVersion2File(final Path path) {

        if (!Files.exists(path) || !Files.isReadable(path) || !Files.isRegularFile(path)) {
            return false;
        }

        boolean result = false;

        if (isFile(path, XML_HEADER)) {
            try {
                result = (int) Math.floor(Float.parseFloat(getXMLVersion(path))) <= Engine.CURRENT_MAJOR_VERSION;
            } catch (final NumberFormatException nfe) {
                Logger.getLogger(FileMagic.class.getName()).info("Invalid version string");
            }
        }

        return result;
    }

    /**
     * Determines the version of the jGnash file.
     *
     * @param path {@code file to check}
     * @return file version as a String
     */
    private static String getXMLVersion(final Path path) {
        String version = "";

        try (final InputStream input = Files.newInputStream(path)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();

            // Protect against external entity attacks
            inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            XMLStreamReader reader = inputFactory.createXMLStreamReader(input, StandardCharsets.UTF_8.name());

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.PROCESSING_INSTRUCTION) {
                    String name = reader.getPITarget();
                    String data = reader.getPIData();

                    if (name.equals("fileFormat")) {
                        version = data;
                        break;
                    }
                }
            }

            reader.close();
        } catch (final IOException e) {
            LogUtil.logSevere(FileMagic.class, e);
        } catch (final XMLStreamException e) {
            Logger.getLogger(FileMagic.class.getName()).log(Level.INFO, "{0} was not a valid jGnash XML_HEADER file",
                    path.toString());
        }

        return version;
    }

    private static boolean detectCharset(final InputStream inputStream, final Charset charset) {
        boolean identified;

        try (final BufferedInputStream input = new BufferedInputStream(inputStream)) {
            final CharsetDecoder decoder = charset.newDecoder();
            final byte[] buffer = new byte[BUFFER_SIZE];

            identified = false;

            while ((input.read(buffer) != -1) && (!identified)) {
                try {
                    decoder.decode(ByteBuffer.wrap(buffer));
                    identified = true;
                } catch (final CharacterCodingException e) {
                    return false;
                }
            }
        } catch (final IOException e) {
            return false;
        }

        return identified;
    }

    private static boolean detectCharset(final Path path, final Charset charset) {
        try {
            return detectCharset(Files.newInputStream(path), charset);
        } catch (final IOException e) {
            return false;
        }
    }

    private static Charset detectCharset(final Path path) {
        for (final Charset charset : CHARSETS) {
            if (detectCharset(path, charset)) {
                return charset;
            }
        }

        return UTF_8;   // default
    }

    /**
     * Determines the Charset of an input stream.
     *
     * The stream will be closed after detection
     * @param inputStream stream to check
     * @return detected Charset
     */
    public static Charset detectCharset(final InputStream inputStream) {
        for (final Charset charset : CHARSETS) {
            if (detectCharset(inputStream, charset)) {
                return charset;
            }
        }

        return UTF_8;   // default
    }

    /**
     * Determines the Charset of an input file
     * The stream will be closed after detection
     * @param fileName file to check
     * @return detected Charset
     */
    public static Charset detectCharset(final String fileName) {
        Charset charset = StandardCharsets.UTF_8;

        final Path path = Paths.get(fileName);

        if (Files.exists(path)) {
            charset = FileMagic.detectCharset(path);
            Logger.getLogger(FileMagic.class.getName()).info(fileName + " was encoded as " + charset);
        }

        return charset;
    }

    public enum FileType {
        BinaryXStream, OfxV1, OfxV2, jGnash2XML, h2, h2mv, hsql, unknown
    }
}
