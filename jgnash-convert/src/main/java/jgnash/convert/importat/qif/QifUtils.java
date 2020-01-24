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
package jgnash.convert.importat.qif;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgnash.engine.MathConstants;
import jgnash.util.FileMagic;

/**
 * Various helper methods for importing QIF files
 *
 * @author Craig Cavanaugh
 * @author Navneet Karnani
 */
public class QifUtils {

    private static final Pattern MONEY_PREFIX_PATTERN = Pattern.compile("\\D");

    private static final Pattern CATEGORY_DELIMITER_PATTERN = Pattern.compile("/");

    private QifUtils() {
    }

    static BigDecimal parseMoney(final String money) {
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
                        Logger l = Logger.getLogger(QifUtils.class.getName());
                        l.info("second parse attempt failed");
                        l.info(buf.toString());
                        l.info("falling back to rounding");
                    }
                }
                NumberFormat formatter = NumberFormat.getNumberInstance();
                try {
                    Number num = formatter.parse(sMoney);
                    BigDecimal bd = BigDecimal.valueOf(num.floatValue());
                    if (bd.scale() > 6) {
                        Logger l = Logger.getLogger(QifUtils.class.getName());
                        l.warning("-Warning-");
                        l.warning("Large scale detected in QifUtils.parseMoney");
                        l.warning("Truncating scale to 2 places");
                        l.warning(bd.toString());
                        bd = bd.setScale(2, MathConstants.roundingMode);
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

    public static boolean isFullFile(final File file) {

        boolean result = false;

        final Charset charset = FileMagic.detectCharset(file.getPath());

        try (final QifReader in = new QifReader(Files.newBufferedReader(file.toPath(), charset))) {
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
                    break;
                } else if (startsWith(line, "!Type:CCard")) { // QIF from an online credit card statement
                    break;
                } else if (startsWith(line, "!Type:Oth")) { // QIF from an online credit card statement
                    break;
                } else if (startsWith(line, "!Type:Cash")) { // Partial QIF export
                    break;
                } else if (startsWith(line, "!Option:AutoSwitch")) {
                    Logger.getLogger(QifUtils.class.getName()).fine("!Option:AutoSwitch");
                } else if (startsWith(line, "!Clear:AutoSwitch")) {
                    Logger.getLogger(QifUtils.class.getName()).fine("!Clear:AutoSwitch");
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
     * @param source the source String.
     * @param prefix the prefix String.
     * @return true, if the source starts with the prefix string.
     */
    private static boolean startsWith(final String source, final String prefix) {
        return prefix.length() <= source.length() && source.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Strip any category tags from the category name... found when parsing
     * transactions
     *
     * @param category string to strip
     * @return the stripped string
     */
    static String stripCategoryTags(final String category) {
        // Auto:Gas/matrix:Vacation > Auto:Gas

        if (category != null && category.contains("/")) {
            return CATEGORY_DELIMITER_PATTERN.split(category)[0];
        }

        return category;
    }
}
