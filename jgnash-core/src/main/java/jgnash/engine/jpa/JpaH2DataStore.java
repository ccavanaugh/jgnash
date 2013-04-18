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
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * JPA specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class JpaH2DataStore implements DataStore {

    public static final String FILE_EXT = "h2.db";

    public static final String LOCK_EXT = ".lock.db";

    private EntityManager em;

    private EntityManagerFactory factory;

    private boolean remote;

    private String fileName;

    private static final boolean DEBUG = false;

    private static final Logger logger = Logger.getLogger(JpaH2DataStore.class.getName());

    private char[] password;

    @Override
    public void closeEngine() {

        logger.info("Closing");

        if (em != null && factory != null) {
            em.close();
            factory.close();
        } else {
            logger.severe("The EntityManger was already null!");
        }

        if (!remote) {
            waitForLockFileRelease(fileName, password);
        }
    }

    @Override
    public Engine getClientEngine(final String host, final int port, final char[] password, final String dataBasePath) {
        Properties properties = JpaConfiguration.getClientProperties(getType(), dataBasePath, host, port, password);

        Engine engine = null;

        factory = Persistence.createEntityManagerFactory("jgnash", properties);

        em = factory.createEntityManager();

        if (em != null) {
            engine = new Engine(new JpaEngineDAO(em, true), EngineFactory.DEFAULT);

            logger.info("Created local JPA container and engine");
            fileName = null;
            remote = true;
        }

        return engine;
    }

    @Override
    public Engine getLocalEngine(final String fileName, final String engineName, final char[] password) {
        Properties properties = JpaConfiguration.getLocalProperties(getType(), fileName, password, false);

        Engine engine = null;

        if (DEBUG) {
            System.out.println(FileUtils.stripFileExtension(fileName));
        }

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                try {
                    factory = Persistence.createEntityManagerFactory("jgnash", properties);

                    em = factory.createEntityManager();

                    logger.info("Created local JPA container and engine");
                    engine = new Engine(new JpaEngineDAO(em, false), engineName);

                    this.fileName = fileName;
                    this.password = password;

                    remote = false;
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } catch (FileNotFoundException e) {
            logger.info(Resource.get().getString("Message.FileNotFound"));
        }

        return engine;
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
    public DataStoreType getType() {
        return DataStoreType.H2_DATABASE;
    }

    @Override
    public boolean isRemote() {
        return remote;
    }

    @Override
    public void saveAs(final File file, final Collection<StoredObject> objects) {

        // Remove the existing file so we don't mix entities and cause corruption
        if (file.exists()) {
            if (!file.delete()) {
                logger.log(Level.SEVERE, "Could not delete the old file for save");
                return;
            }
        }

        Properties properties = JpaConfiguration.getLocalProperties(getType(), file.getAbsolutePath(), new char[]{}, false);

        EntityManagerFactory factory = null;
        EntityManager em = null;

        try {
            factory = Persistence.createEntityManagerFactory("jgnash", properties);
            em = factory.createEntityManager();

            em.getTransaction().begin();

            for (StoredObject o : objects) {
                em.persist(o);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (em != null) {
                em.close();
            }

            if (factory != null) {
                factory.close();
            }
        }

        waitForLockFileRelease(file.getAbsolutePath(), new char[]{});
    }

    private void waitForLockFileRelease(final String fileName, final char[] password) {
        // Explicitly force the database closed, Required for h2
        try {
            Class.forName("org.h2.Driver");
            SqlUtils.waitForLockFileRelease(getType(), fileName, LOCK_EXT, password);
        } catch (final ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

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
