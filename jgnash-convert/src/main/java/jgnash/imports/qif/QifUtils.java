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
package jgnash.imports.qif;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Various helper methods for importing QIF files
 * 
 * @author Craig Cavanaugh
 * @author Navneet Karnani
 */
public class QifUtils {

    /**
     * US date format
     */
    public static final String US_FORMAT = "mm/dd/yyyy";

    /**
     * European date format
     */
    public static final String EU_FORMAT = "dd/mm/yyyy";

    private static final Pattern DATE_DELIMITER_PATTERN = Pattern.compile("/|'|\\.|-");

    private static final Pattern MONEY_PREFIX_PATTERN = Pattern.compile("\\D");

    private static final Pattern CATEGORY_DELIMITER_PATTERN = Pattern.compile("/");

    private QifUtils() {
    }

    /**
     * Converts a string into a data object
     * <p>
     * <p/>
     * format "6/21' 1" -> 6/21/2001 format "6/21'01" -> 6/21/2001 format
     * "9/18'2001 -> 9/18/2001 format "06/21/2001" format "06/21/01" format
     * "3.26.03" -> German version of quicken format "03-26-2005" -> MSMoney
     * format format "1.1.2005" -> kmymoney2 20.1.94 European dd/mm/yyyy has
     * been confirmed
     * <p/>
     * 21/2/07 -> 02/21/2007 UK, Quicken 2007 D15/2/07
     * 
     * @param sDate
     *            String QIF date to parse
     * @param format
     *            String identifier of format to parse
     * @return Returns parsed date and current date if an error occurs
     */
    @SuppressWarnings("MagicConstant")
    public static Date parseDate(String sDate, String format) {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);

        String[] chunks = DATE_DELIMITER_PATTERN.split(sDate);

        switch (format) {
        case US_FORMAT:
            try {
                month = Integer.parseInt(chunks[0].trim());
                day = Integer.parseInt(chunks[1].trim());
                year = Integer.parseInt(chunks[2].trim());
            } catch (Exception e) {
                Logger.getAnonymousLogger().severe(e.toString());
            }
            break;
        case EU_FORMAT:
            try {
                day = Integer.parseInt(chunks[0].trim());
                month = Integer.parseInt(chunks[1].trim());
                year = Integer.parseInt(chunks[2].trim());
            } catch (Exception e) {
                Logger.getAnonymousLogger().severe(e.toString());
            }
            break;
        default:
            Logger.getAnonymousLogger().severe("Invalid date format specified");
            return new Date();
        }

        if (year < 100) {
            if (year < 29) {
                year += 2000;
            } else {
                year += 1900;
            }
        }
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static BigDecimal parseMoney(final String money) {
        String sMoney = money;

        if (sMoney != null) {
            sMoney = sMoney.trim(); // to be safe
            try {
                return new BigDecimal(sMoney);
            } catch (NumberFormatException e) {
                /* there must be commas, etc in the number.  Need to look for them
                 * and remove them first, and then try BigDecimal again.  If that
                 * fails, then give up and use NumberFormat and scale it down
                 * */

                String[] split = MONEY_PREFIX_PATTERN.split(sMoney);
                if (split.length > 2) {
                    StringBuilder buf = new StringBuilder();
                    if (sMoney.startsWith("-")) {
                        buf.append('-');
                    }
                    for (int i = 0; i < split.length - 1; i++) {
                        buf.append(split[i]);
                    }
                    buf.append('.');
                    buf.append(split[split.length - 1]);
                    try {
                        return new BigDecimal(buf.toString());
                    } catch (final NumberFormatException e2) {
                        Logger l = Logger.getAnonymousLogger();
                        l.info("second parse attempt failed");
                        l.info(buf.toString());
                        l.info("falling back to rounding");
                    }
                }
                NumberFormat formatter = NumberFormat.getNumberInstance();
                try {
                    Number num = formatter.parse(sMoney);
                    BigDecimal bd = new BigDecimal(num.floatValue());
                    if (bd.scale() > 6) {
                        Logger l = Logger.getLogger(QifUtils.class.getName());
                        l.warning("-Warning-");
                        l.warning("Large scale detected in QifUtils.parseMoney");
                        l.warning("Truncating scale to 2 places");
                        l.warning(bd.toString());
                        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                        l.warning(bd.toString());
                    }
                    return bd;
                } catch (ParseException ignored) {
                    Logger.getLogger(QifUtils.class.getName())
                            .log(Level.SEVERE, "poorly formatted number: {0}", sMoney);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public static boolean isFullFile(File file) {

        boolean result = false;

        try (QifReader in = new QifReader(new FileReader(file))) {
            String line = in.readLine();

            while (line != null) {
                if (startsWith(line, "!Type:Class")) {
                    result = true;
                    break;
                } else if (startsWith(line, "!Type:Cat")) {
                    result = true;
                    break;
                } else if (startsWith(line, "!Account")) {
                    result = true;
                    break;
                } else if (startsWith(line, "!Type:Memorized")) {
                    result = true;
                    break;
                } else if (startsWith(line, "!Type:Security")) {
                    result = true;
                    break;
                } else if (startsWith(line, "!Type:Prices")) {
                    result = true;
                    break;
                } else if (startsWith(line, "!Type:Bank")) { // QIF from an online bank statement... assumes the account is known
                    result = false;
                    break;
                } else if (startsWith(line, "!Type:CCard")) { // QIF from an online credit card statement
                    result = false;
                    break;
                } else if (startsWith(line, "!Type:Oth")) { // QIF from an online credit card statement
                    result = false;
                    break;
                } else if (startsWith(line, "!Type:Cash")) { // Partial QIF export
                    result = false;
                    break;
                } else if (startsWith(line, "!Option:AutoSwitch")) {
                    // eat the line
                } else if (startsWith(line, "!Clear:AutoSwitch")) {
                    // eat the line
                } else {
                    System.out.println("Error: " + line);
                    break;
                }
                line = in.readLine();
            }
            in.close();
            return result;
        } catch (FileNotFoundException e) {
            Logger.getLogger(QifUtils.class.getName()).log(Level.SEVERE, "Could not find file: {0}",
                    file.getAbsolutePath());

            return false;
        } catch (IOException e) {
            Logger.getLogger(QifUtils.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
    }

    /**
     * Tests if the source string starts with the prefix string. Case is
     * ignored.
     * 
     * @param source
     *            the source String.
     * @param prefix
     *            the prefix String.
     * @return true, if the source starts with the prefix string.
     */
    private static boolean startsWith(final String source, final String prefix) {
        return prefix.length() <= source.length() && source.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Strip any category tags from the category name... found when parsing
     * transactions
     * 
     * @param category
     *            string to strip
     * @return the stripped string
     */
    public static String stripCategoryTags(final String category) {
        // Auto:Gas/matrix:Vacation > Auto:Gas

        if (category != null && category.contains("/")) {
            return CATEGORY_DELIMITER_PATTERN.split(category)[0];
        }

        return category;
    }
}
