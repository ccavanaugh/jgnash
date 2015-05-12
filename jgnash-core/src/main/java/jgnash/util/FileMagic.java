/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Class to identify file type
 *
 * @author Craig Cavanaugh
 */
public class FileMagic {

    private static final Pattern COLON_DELIMITER_PATTERN = Pattern.compile(":");

    private static final byte[] BINARY_XSTREAM_HEADER = new byte[]{10, -127, 0, 13, 111, 98, 106, 101, 99, 116, 45,
            115, 116, 114, 101, 97, 109, 11, -127, 10};

    private static final byte[] H2_HEADER = new byte[]{0x2D, 0x2D, 0x20, 0x48, 0x32, 0x20, 0x30, 0x2E, 0x35, 0x2F, 0x42, 0x20, 0x2D, 0x2D};

    private static final byte[] HSQL_HEADER = "SET DATABASE UNIQUE NAME HSQLDB".getBytes(StandardCharsets.UTF_8);

    private static final byte[] XML_HEADER = "<?xml version=\"1.0\"".getBytes(StandardCharsets.UTF_8);

    private static final String USASCII = "USASCII";

    private static final String WINDOWS_1252 = "windows-1252";

    public enum FileType {
        db4o, BinaryXStream, OfxV1, OfxV2, jGnash1XML, jGnash2XML, h2, hsql, unknown
    }

    /**
     * Returns the file type
     *
     * @param file file to identify
     * @return identified file type
     */
    public static FileType magic(final File file) {

        if (isValidVersion2File(file)) {
            return FileType.jGnash2XML;
        } else if (isBinaryXStreamFile(file)) {
            return FileType.BinaryXStream;
        } else if (isH2File(file)) {
            return FileType.h2;
        } else if (isHsqlFile(file)) {
            return FileType.hsql;
        } else if (isValidVersion1File(file)) {
            return FileType.jGnash1XML;
        } else if (isOfxV1(file)) {
            return FileType.OfxV1;
        } else if (isOfxV2(file)) {
            return FileType.OfxV2;
        } else if (isdb4o(file)) {
            return FileType.db4o;
        }

        return FileType.unknown;
    }

    /**
     * Determine the correct character encoding of an OFX Version 1 file
     *
     * @param file File to look at
     * @return encoding of the file
     */
    public static String getOfxV1Encoding(final File file) {
        String encoding = null;
        String charset = null;

        if (file.exists()) {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
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
                                charset = splits[1];
                            }
                        }

                        if (encoding != null && charset != null) {
                            break;
                        }
                    }
                    line = reader.readLine();
                }
            } catch (final IOException e) {
                Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }

        if (encoding != null && charset != null) {
            if (encoding.equals(StandardCharsets.UTF_8.name()) && charset.equals("CSUNICODE")) {
                return StandardCharsets.ISO_8859_1.name();
            } else if (encoding.equals(StandardCharsets.UTF_8.name())) {
                return StandardCharsets.UTF_8.name();
            } else if (encoding.equals(USASCII) && charset.equals("1252")) {
                return WINDOWS_1252;
            } else if (encoding.equals(USASCII) && charset.contains("8859-1")) {
                return "ISO-8859-1";
            } else if (encoding.equals(USASCII) && charset.equals("NONE")) {
                return WINDOWS_1252;
            }
        }

        return WINDOWS_1252;
    }

    public static boolean isOfxV1(final File file) {

        boolean result = false;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line = reader.readLine();

                while (line != null) {
                    line = line.trim();

                    if (!line.isEmpty()) { // allow empty lines at the beginning of the file
                        if (line.startsWith("OFXHEADER:")) {
                            result = true;
                        }
                        break;
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {
                Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }

        return result;
    }

    public static boolean isOfxV2(final File file) {

        boolean result = false;

        if (file.exists()) {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

                String line = reader.readLine();

                while (line != null) {
                    line = line.trim();

                    // consume any processing instructions and check for ofx 2.0 hints
                    if (!line.isEmpty() && line.startsWith("<?")) {
                        if (line.startsWith("<?OFX") && line.contains("OFXHEADER=\"200\"")) {
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
            } catch (IOException e) {
                Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }

        return result;
    }

    public static boolean isBinaryXStreamFile(final File file) {
        return isFile(file, BINARY_XSTREAM_HEADER);
    }

    private static boolean isH2File(final File file) {
        return isFile(file, H2_HEADER);
    }

    private static boolean isHsqlFile(final File file) {
        return isFile(file, HSQL_HEADER);
        //return file.getAbsolutePath().endsWith("script");
    }

    private static boolean isFile(final File file, final byte[] header) {
        boolean result = false;

        if (file.exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                if (raf.length() > 0) { // must not be a zero length file
                    byte[] fileHeader = new byte[header.length];

                    raf.readFully(fileHeader);

                    result = Arrays.equals(fileHeader, header);
                }
            } catch (IOException ex) {
                Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }

    static boolean isdb4o(final File file) {
        boolean result = false;

        if (file.exists()) {

            try (RandomAccessFile di = new RandomAccessFile(file, "r")) {
                byte[] header = new byte[4];

                if (di.length() > 0) { // must not be a zero length file
                    di.readFully(header);

                    if (new String(header, StandardCharsets.UTF_8).equals("db4o")) {
                        result = true;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }

    public static boolean isValidVersion1File(final File file) {
        return isValidVersionXFile(file, "1");
    }

    private static boolean isValidVersion2File(final File file) {
        return isValidVersionXFile(file, "2");
    }

    private static boolean isValidVersionXFile(final File file, final String majorVersion) {

        if (!file.exists() || !file.isFile() || !file.canRead()) {
            return false;
        }

        boolean result = false;

        if (isFile(file, XML_HEADER)) {
            result = getXMLVersion(file).startsWith(majorVersion);
        }

        return result;
    }

    /**
     * Determines the version of the jGnash file
     * @param file {@code file to check}
     * @return file version as a String
     */
    public static String getXMLVersion(final File file) {
        String version = "";

        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();

            XMLStreamReader reader = inputFactory.createXMLStreamReader(input, StandardCharsets.UTF_8.name());

            parse:
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.PROCESSING_INSTRUCTION:
                        String name = reader.getPITarget();
                        String data = reader.getPIData();

                        if (name.equals("fileVersion")) {
                            version = data;
                            break parse;
                        }
                        break;
                    default:
                        break;
                }
            }

            reader.close();
        } catch (IOException e) {
            Logger.getLogger(FileMagic.class.getName()).log(Level.SEVERE, e.toString(), e);
        } catch (XMLStreamException e) {
            Logger.getLogger(FileMagic.class.getName()).log(Level.INFO, "{0} was not a valid jGnash XML_HEADER file",
                    file.getAbsolutePath());
        }

        return version;
    }

    private FileMagic() {
    }
}
