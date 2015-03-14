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

/**
 * Callable is defined with one generic parameter: the parameter specifies the return type of the method.
 *
 * This is similar to java.util.concurrent.Callable, but call does not throw an exception
 *
 * @param <R> The type of the return type of the <code>call</code> method.
 */
@FunctionalInterface
public interface Callable<R> {

    /**
     * The <code>call</code> method is called when required, and is given a
     * single argument of type P, with a requirement that an object of type R
     * is returned.

     * @return An object of type R that may be determined based on the provided parameter value.
     */
    public R call();
}
