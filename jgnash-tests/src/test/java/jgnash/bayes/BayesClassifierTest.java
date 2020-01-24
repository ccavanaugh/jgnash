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
package jgnash.bayes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BayesClassifierTest {

    @Test
    void testClassifier() {
        BayesClassifier<String> classifier = new BayesClassifier<>("default");

        classifier.train("Gasoline oil washer fluid brakes lights transmission auto", "Auto");
        classifier.train("groceries bacon fish burger milk chips", "Grocery");
        classifier.train("movie video DVD music theater", "Entertainment");

        assertEquals("Auto", classifier.classify("Oil and washer fluid"));
        assertEquals("Grocery", classifier.classify("Fish and chips"));
        assertEquals("default",  classifier.classify("flowers and shrubs"));
    }
}
