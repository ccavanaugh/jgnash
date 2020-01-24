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

package jgnash;

import java.util.Locale;

import jgnash.resource.util.ClassPathUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for classpath utilities.
 *
 * @author Craig Cavanaugh
 */
class ClassPathTest {

    ClassPathTest() {
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
    void findRealPath() {

        String path = ClassPathUtils.getLocalizedPath("/jgnash/resource/account");

        assertNotNull(path, "failed test");

        System.out.println(path);
    }

    @Test
    void findFakePath() {

        Locale defaultLocale = Locale.getDefault();

        Locale.setDefault(new Locale("ZZ"));

        String path = ClassPathUtils.getLocalizedPath("/jgnash/resource/account");

        Locale.setDefault(defaultLocale);

        assertNotNull(path, "failed test");

        System.out.println(path);
    }
}
