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
package jgnash.engine.jpa;

import jgnash.engine.DataStoreType;
import jgnash.util.NotNull;

/**
 * JPA specific code for data storage and creating an engine.
 *
 * @author Craig Cavanaugh
 */
public class JpaH2MvDataStore extends JpaH2DataStore {

    public static final String MV_FILE_EXT = ".mv.db";

    @NotNull
    @Override
    public String getFileExt() {
        return MV_FILE_EXT;
    }

    @Override
    public DataStoreType getType() {
        return DataStoreType.H2MV_DATABASE;
    }
}
