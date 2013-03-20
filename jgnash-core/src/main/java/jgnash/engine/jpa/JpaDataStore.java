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
package jgnash.engine.jpa;

import jgnash.engine.DataStore;
import jgnash.engine.Engine;
import jgnash.engine.StoredObject;

import java.io.File;
import java.util.Collection;

/**
 * JPA specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class JpaDataStore implements DataStore {
    @Override
    public void closeEngine() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Engine getClientEngine(String host, int port, String user, String password, String engineName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Engine getLocalEngine(String fileName, String engineName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFileExt() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFileName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRemote() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void saveAs(File file, Collection<StoredObject> objects) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
