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
package jgnash.imports.ofx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.util.FileMagic;

/**
 * Utility class to convert OFX version 1 (SGML) to OFX version 2 (XML)
 * 
 * @author Craig Cavanaugh
 * 
 */
public class OfxV1ToV2 {

	private static final int READ_AHEAD_LIMIT = 2048;

	public static String convertToXML(final File file) {
		String encoding = FileMagic.getOfxV1Encoding(file);

		Logger.getLogger(OfxV1ToV2.class.getName()).info(
				"OFX Version 1 file encoding was " + encoding);

		return convertSgmlToXML(readFile(file, encoding));
	}

	public static String convertToXML(final InputStream stream) {
		return convertSgmlToXML(readFile(stream,
				System.getProperty("file.encoding")));
	}

	private static String convertSgmlToXML(final String sgml) {
		StringBuilder xml = new StringBuilder(sgml);

		int readPos = 0;
		int tagEnd = 0;

		while (readPos < xml.length() && readPos != -1) {
			String tag;

			readPos = xml.indexOf("<", tagEnd);

			if (readPos != -1) {
				tagEnd = xml.indexOf(">", readPos);

				if (tagEnd != -1) {
					tag = xml.substring(readPos + 1, tagEnd);

					if (!tag.startsWith("/")) {
						if (xml.indexOf("</" + tag + ">", tagEnd) == -1) {

							readPos = xml.indexOf("<", tagEnd);
							xml = xml.insert(readPos, "</" + tag + ">");
						}
					}
				} else {
					readPos = -1;
				}
			}
		}
		return xml.toString();
	}

	private static String concat(final Collection<String> strings) {
		StringBuilder b = new StringBuilder();

		for (String s : strings) {
			b.append(s.trim());
		}

		return b.toString();
	}

	/**
	 * Munch through the header one character at a time. Do not assume clean
	 * formating or EOL characters.
	 * 
	 * @param reader
	 * @throws IOException
	 */
	private static void consumeHeader(final BufferedReader reader)
			throws IOException {

		Logger logger = Logger.getLogger(OfxV1ToV2.class.getName());

		while (true) {
			reader.mark(READ_AHEAD_LIMIT);

			int character = reader.read();

			if (character >= 0) {
				if (character == '<') {
					reader.reset();
					logger.info("readHeader() Complete");
					break;
				}
			} else {
				break;
			}
		}
	}

	/**
	 * Reads a file, strips the header and reads the SGML content into one large
	 * string
	 * 
	 * @param stream
	 *            input stream
	 * @return a String with the SGML content and header removed
	 */
	private static String readFile(final InputStream stream,
			final String characterSet) {

		Logger logger = Logger.getLogger(OfxV1ToV2.class.getName());

		if (stream == null) {
			logger.severe("InputStream was null");
			return null;
		}

		List<String> strings = new ArrayList<>();

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new InputStreamReader(stream,
					characterSet));

			// consume the Ofx1 header
			consumeHeader(reader);

			String line = reader.readLine();

			while (line != null) {

				line = line.trim();

				if (line.length() > 0) {
					strings.add(line);
				}

				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE, e.toString(), e);
				}
			}
		}

		return concat(strings);
	}

	private static String readFile(final File file, final String characterSet) {
		try {
			return readFile(new FileInputStream(file), characterSet);
		} catch (FileNotFoundException e) {
			Logger logger = Logger.getLogger(OfxV1ToV2.class.getName());
			logger.log(Level.SEVERE, e.toString(), e);
			return "";
		}
	}

	private OfxV1ToV2() {
	}
}
