/*
 * Copyright (C) 2008 Arnout Engelen
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
package net.bzzt.swift.mt940.parser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import net.bzzt.swift.mt940.Mt940Entry;
import net.bzzt.swift.mt940.Mt940Entry.SollHabenKennung;
import net.bzzt.swift.mt940.Mt940File;
import net.bzzt.swift.mt940.Mt940Record;

/**
 * @author Arnout Engelen
 * @author Miroslav Holubec
 */
public class Mt940Parser {

    /**
     * Invoke the Mt940-parser stand alone: for testing.
     *
     * @param args
     * @throws IOException
     * @throws ParseException
     */
    /*public static void main(String[] args) throws IOException, ParseException {
         String fileName = "/home/arnouten/dev/swiftmt940/voorbeeld.STA";

         LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(fileName)));
         Mt940File file = new Mt940Parser().parse(reader);
         for (Mt940Record record : file.getRecords())
         {
             for (Mt940Entry entry : record.getEntries())
             {
                 System.out.println(entry.toString());
             }
         }
     }*/

    /** 
     * Parse the Mt940-file. Mt940 records are delimited by '-'.
     *
     * @param reader
     * @return Mt940File instance
     * @throws IOException
     * @throws ParseException
     */
    public Mt940File parse(final LineNumberReader reader) throws IOException, ParseException {
        final Mt940File retval = new Mt940File();

        List<String> recordLines = new ArrayList<>();

        String currentLine = reader.readLine();
        while (currentLine != null) {
            if (currentLine.startsWith("-")) {
                // Parse this record and add it to the file
                retval.getRecords().add(parseRecord(recordLines));

                // Clear recordLines to start on the next record
                recordLines = new ArrayList<>();
            } else {
                recordLines.add(currentLine);
            }
            currentLine = reader.readLine();
        }

        // A file might not end with a trailing '-' (e.g. from Rabobank):
        retval.getRecords().add(parseRecord(recordLines));

        return retval;
    }

    /**
     * An mt940-record first has a couple of 'header' lines that do not
     * start with a ':'.
     *
     * After that, a line that doesn't start with a ':' is assumed to
     * belong to the previous 'real' line.
     *
     * @return
     */
    private static List<String> mergeLines(final List<String> recordLines) {
        List<String> retval = new ArrayList<>();
        String currentString = null;
        boolean inMessage = false;
        for (String string : recordLines) {
            // Are we in the message proper, after the
            // header?
            if (inMessage) {
                if (string.startsWith(":")) {
                    retval.add(currentString);
                    currentString = "";
                }
                currentString += string;
            } else {
                if (string.startsWith(":")) {
                    // then we're past the header
                    inMessage = true;
                    currentString = string;
                } else {
                    // add a line of the header
                    retval.add(string);
                }
            }
        }
        return retval;
    }

    /**
     * An mt940-record consists of some general lines and a couple
     * of entries consisting of a :61: and a :86:-section.
     *
     * @param recordLines
     * @return
     * @throws ParseException
     */
    private static Mt940Record parseRecord(final List<String> recordLines) throws ParseException {
        Mt940Record retval = new Mt940Record();

        // Merge 'lines' that span multiple actual lines.
        List<String> mergedLines = mergeLines(recordLines);
        
        Mt940Entry currentEntry = null;
        for (String line : mergedLines) {
            if (line.startsWith(":61:")) {
            	currentEntry = nextEntry(retval.getEntries(), currentEntry);
            	
                line = line.substring(4);
                line = parseDatumJJMMTT(currentEntry, line);
                // for now don't handle the buchungsdatum. It is optional.
                if (startsWithBuchungsDatum(line))
                {
                	line = line.substring(4);
                }
                // for now only support C and D, not RC and RD
                line = parseSollHabenKennung(currentEntry, line);
                line = parseBetrag(currentEntry, line);
            }
            if (line.startsWith(":86:")) {
                currentEntry.addToMehrzweckfeld(line.substring(4));
            }
        }
        
        // add the last one:
        nextEntry(retval.getEntries(), currentEntry);
        
        return retval;
    }

    /**
     *  adds the current entry to the result as a side-effect, if available 
     */
    private static Mt940Entry nextEntry(List<Mt940Entry> entries,
			Mt940Entry previousEntry) {
    	if (previousEntry != null)
    	{
    		entries.add(previousEntry);
    	}
    	return new Mt940Entry();
	}

	/** 
     * BuchungsDatum is a 4-character optional field - but how can we check whether it was included?
     * 
     * The field is directly followed by the mandatory 'soll/haben-kennung' character, so
     * we assume that when the string starts with a digit that's probably the buchungsdatum
     */
    private static boolean startsWithBuchungsDatum(String line) {
		return line != null && line.matches("^\\d.*");
	}

	/**
     * Parse the value, put it into the entry.
     *
     * @return the rest of the string to be parsed
     */
    private static String parseBetrag(final Mt940Entry currentEntry, final String line) {
        int endIndex = line.indexOf('N');
        if (endIndex < 0) {
            endIndex = line.indexOf('F');
        }

        String betrag = line.substring(0, endIndex);
        betrag = betrag.replaceAll(",", ".");
        //currentEntry.setBetrag(BigDecimal.valueOf(Double.valueOf(betrag)));
        currentEntry.setBetrag(new BigDecimal(betrag));

        return line.substring(endIndex);
    }

    /**
     * Parse the debet/credit value, put it into the entry.
     *
     * @return the rest of the string to be parsed
     */
    private static String parseSollHabenKennung(final Mt940Entry currentEntry, final String string) {
        String s = string;

        if (string.startsWith("D")) {
            currentEntry.setSollHabenKennung(SollHabenKennung.DEBIT);
            s = string.substring(1);
        } else if (string.startsWith("C")) {
            currentEntry.setSollHabenKennung(SollHabenKennung.CREDIT);
            s = string.substring(1);
        } else {
            throw new UnsupportedOperationException("soll-haben-kennung " + s + " not yet supported");
        }
        return s;
    }

    /**
     * Parse the JJMMTT-formatted date, put it into the entry.
     *
     * @return the rest of the string to be parsed
     */
    private static String parseDatumJJMMTT(final Mt940Entry currentEntry, final String string) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
        String date = string.substring(0, 6);
        currentEntry.setValutaDatum(format.parse(date));

        return string.substring(6);
    }
}
