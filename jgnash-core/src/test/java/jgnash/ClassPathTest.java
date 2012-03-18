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

package jgnash;

import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import jgnash.util.ClassPathUtils;

import org.junit.Test;

/**
 * @author Craig Cavanaugh
 *
 */
public class ClassPathTest {

    public ClassPathTest() {
    }

    //    @Test
    //    public void findExtensions() {
    //
    //        Collection<String> list = ClassPathUtils.getFilesByExtension("/jgnash/resource", ".xml");
    //
    //        for (String string : list) {
    //            System.out.println(string);
    //        }
    //
    //        assertTrue("passed test: ", list.size() > 0);
    //    }

    @Test
    public void findRealPath() {

        String path = ClassPathUtils.getLocalizedPath("/jgnash/resource/account");

        System.out.println(path);

        assertNotNull("failed test", path);
    }

    @Test
    public void findFakePath() {

        Locale defaultLocale = Locale.getDefault();

        Locale.setDefault(new Locale("ZZ"));

        String path = ClassPathUtils.getLocalizedPath("/jgnash/resource/account");

        System.out.println(path);

        Locale.setDefault(defaultLocale);

        assertNotNull("failed test", path);
    }
}
