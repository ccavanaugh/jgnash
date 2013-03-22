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
import jgnash.util.Resource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.util.Collection;
import java.util.Properties;

/**
 * JPA specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class JpaDataStore implements DataStore {

    private static final String FILE_EXT = "db";

    private EntityManager em;

    private EntityManagerFactory factory;

    private boolean remote;

    private String fileName;

    // private static final boolean DEBUG = false;

    @Override
    public void closeEngine() {
        if (em != null && factory != null) {
            em.close();
            factory.close();
        }
    }

    @Override
    public Engine getClientEngine(final String host, final int port, final String user, final String password, final String engineName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Engine getLocalEngine(final String fileName, final String engineName) {

        Properties properties = new Properties();

        properties.setProperty("openjpa.ConnectionURL","jdbc:h2:" + fileName);
        properties.setProperty("openjpa.ConnectionDriverName","org.h2.Driver");
        properties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema");

        properties.setProperty("openjpa.ConnectionUserName", "");
        properties.setProperty("openjpa.ConnectionPassword", "");

        factory = Persistence.createEntityManagerFactory("jgnash", System.getProperties());
        em = factory.createEntityManager();

        this.fileName = fileName;

        remote = false;


        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFileExt() {
        return FILE_EXT;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean isRemote() {
        return remote;
    }

    @Override
    public void saveAs(final File file, final Collection<StoredObject> objects) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns the string representation of this <code>DataStore</code>.
     *
     * @return string representation of this <code>DataStore</code>.
     */
    @Override
    public String toString() {
        return Resource.get().getString("DataStoreType.H2");
    }
}
