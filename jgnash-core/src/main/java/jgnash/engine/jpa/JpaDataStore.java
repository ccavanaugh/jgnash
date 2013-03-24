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

import jgnash.engine.Config;
import jgnash.engine.DataStore;
import jgnash.engine.Engine;
import jgnash.engine.StoredObject;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.File;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JPA specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class JpaDataStore implements DataStore {

    public static final String FILE_EXT = "h2.db";

    private EntityManager em;

    private EntityManagerFactory factory;

    private boolean remote;

    private String fileName;

    private static final boolean DEBUG = false;

    @Override
    public void closeEngine() {
        if (em != null && factory != null) {
            em.close();
            factory.close();
        }
    }

    @Override
    public Engine getClientEngine(final String host, final int port, final String user, final String password, final String engineName) {
        Properties properties = System.getProperties();

        properties.setProperty("openjpa.ConnectionURL", "jdbc:h2:" + FileUtils.stripFileExtension(fileName));
        properties.setProperty("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema");

        properties.setProperty("openjpa.ConnectionUserName", user);
        properties.setProperty("openjpa.ConnectionPassword", password);

        return null;  // TODO Fix me
    }

    @Override
    public Engine getLocalEngine(final String fileName, final String engineName) {
        Properties properties = System.getProperties();

        properties.setProperty("openjpa.ConnectionURL", "jdbc:h2:" + FileUtils.stripFileExtension(fileName));
        //properties.setProperty("openjpa.ConnectionDriverName", "org.h2.Driver");
        //properties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema");

        properties.setProperty("openjpa.ConnectionUserName", "");
        properties.setProperty("openjpa.ConnectionPassword", "");

        Engine engine = null;

        if (DEBUG) {
            System.out.println(FileUtils.stripFileExtension(fileName));
        }

        try {
            factory = Persistence.createEntityManagerFactory("jgnash", properties);
            //factory = Persistence.createEntityManagerFactory("jgnash", System.getProperties());

            em = factory.createEntityManager();

            Logger.getLogger(JpaDataStore.class.getName()).info("Created local JPA container and engine");
            engine = new Engine(new JpaEngineDAO(em, false), engineName);

            this.fileName = fileName;
            remote = false;
        } catch (final Exception e) {
            Logger.getLogger(JpaDataStore.class.getName()).log(Level.SEVERE, e.getMessage(), e);
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
    public boolean isRemote() {
        return remote;
    }

    @Override
    public void saveAs(final File file, final Collection<StoredObject> objects) {

        // Remove the existing file so we don't mix entities and cause corruption
        if (file.exists()) {
            file.delete();
        }

        Properties properties = System.getProperties();

        properties.setProperty("openjpa.ConnectionURL", "jdbc:h2:" + FileUtils.stripFileExtension(file.getAbsolutePath()));
        properties.setProperty("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema");

        properties.setProperty("openjpa.ConnectionUserName", "");
        properties.setProperty("openjpa.ConnectionPassword", "");

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
            Logger.getLogger(JpaDataStore.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (em != null) {
                em.close();
            }

            if (factory != null) {
                factory.close();
            }
        }
    }

    /**
     * Opens the database in readonly mode and reads the version of the file format.
     *
     * @param file <code>File</code> to open
     * @return file version
     */
    public static float getFileVersion(final File file) {

        float fileVersion = 0;

        Properties properties = System.getProperties();

        properties.setProperty("openjpa.ConnectionURL", "jdbc:h2:" + FileUtils.stripFileExtension(file.getAbsolutePath()));
        properties.setProperty("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema");

        properties.setProperty("openjpa.ConnectionUserName", "");
        properties.setProperty("openjpa.ConnectionPassword", "");

        EntityManagerFactory factory = null;
        EntityManager em = null;

        try {
            factory = Persistence.createEntityManagerFactory("jgnash", properties);
            em = factory.createEntityManager();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Config> cq = cb.createQuery(Config.class);
            Root<Config> root = cq.from(Config.class);
            cq.select(root);

            TypedQuery<Config> q = em.createQuery(cq);

            Config defaultConfig = q.getSingleResult();

            fileVersion = defaultConfig.getFileVersion();


        } catch (Exception e) {
            Logger.getLogger(JpaDataStore.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (em != null) {
                em.close();
            }

            if (factory != null) {
                factory.close();
            }
        }

        return fileVersion;
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
