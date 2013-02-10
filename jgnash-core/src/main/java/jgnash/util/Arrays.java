/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.util.Iterator;
import java.util.List;

/**
 * Utility class for working with Arrays and Lists
 * 
 * @author Craig Cavanaugh
 */
public class Arrays {
    
    private Arrays() {}
    
    /**
     * Converts a List of Integers to an integer array
     * 
     * @param integerList List of Integers
     * @return integer array
     */
    public static int[] intListToArray(final List<Integer> integerList)
    {
        int[] intArray = new int[integerList.size()];        
        Iterator<Integer> iterator = integerList.iterator();
        
        for (int i = 0; i < intArray.length; i++)
        {
            intArray[i] = iterator.next();
        }
        
        return intArray;
    }

}
