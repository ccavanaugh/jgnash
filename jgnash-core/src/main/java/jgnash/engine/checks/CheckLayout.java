/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
package jgnash.engine.checks;

import java.util.ArrayList;
import java.util.List;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

/**
 * Check layout object
 *
 * @author Craig Cavanaugh
 */
public class CheckLayout {
    private final List<CheckObject> checkObjects = new ArrayList<>();

    final private PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();

    private int numberOfChecks = 1;

    private double checkHeight = 240;

    public void add(CheckObject object) {
        checkObjects.add(object);
    }

    public void remove(CheckObject object) {
        checkObjects.remove(object);
    }

    public List<CheckObject> getCheckObjects() {
        return checkObjects;
    }

    public void setCheckHeight(double height) {
        checkHeight = height;
    }

    public double getCheckHeight() {
        return checkHeight;
    }

    public void setNumberOfChecks(int count) {
        numberOfChecks = count;
    }

    public int getNumberOfChecks() {
        return numberOfChecks;
    }

    public PrintRequestAttributeSet getPrintAttributes() {
        return printAttributes;
    }
}