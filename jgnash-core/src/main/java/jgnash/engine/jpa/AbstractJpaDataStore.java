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
package jgnash.engine.jpa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import jgnash.engine.DataStore;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.attachment.DistributedAttachmentManager;
import jgnash.engine.attachment.LocalAttachmentManager;
import jgnash.engine.concurrent.DistributedLockManager;
import jgnash.engine.concurrent.LocalLockManager;
import jgnash.util.FileUtils;

/**
 * Abstract JPA DataStore
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractJpaDataStore implements DataStore {

    private EntityManager em;

    private EntityManagerFactory factory;

    private DistributedLockManager distributedLockManager;

    private DistributedAttachmentManager distributedAttachmentManager;

    private boolean remote;

    private String fileName;

    private static final boolean DEBUG = false;

    private char[] password;

    static final Logger logger = Logger.getLogger(AbstractJpaDataStore.class.getName());

    void waitForLockFileRelease(final String fileName, final char[] password) {

        // Explicitly force the database closed, Required for hsqldb and h2
        SqlUtils.waitForLockFileRelease(getType(), fileName, getLockFileExtension(), password);
    }

    @Override
    public void closeEngine() {
        logger.info("Closing");

        if (em != null && factory != null) {
            em.close();
            factory.close();
        } else {
            logger.severe("The EntityManger was already null!");
        }

        if (remote) {
            distributedLockManager.disconnectFromServer();
            distributedAttachmentManager.disconnectFromServer();
        } else {
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
            distributedLockManager = new DistributedLockManager(host, port + 2);
            boolean lockManagerResult = distributedLockManager.connectToServer(password);

            distributedAttachmentManager = new DistributedAttachmentManager(host, port + 3);
            boolean attachmentManagerResult = distributedAttachmentManager.connectToServer(password);

            if (attachmentManagerResult && lockManagerResult) {
                engine = new Engine(new JpaEngineDAO(em, true), distributedLockManager, distributedAttachmentManager, EngineFactory.DEFAULT);

                logger.info("Created local JPA container and engine");
                fileName = null;
                remote = true;
            } else {
                distributedLockManager.disconnectFromServer();
                distributedAttachmentManager.disconnectFromServer();

                em.close();
                factory.close();
                em = null;
                factory = null;
            }
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

        if (!exists(fileName)) {
            initEmptyDatabase(fileName);
        }

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                try {
                    factory = Persistence.createEntityManagerFactory("jgnash", properties);

                    em = factory.createEntityManager();

                    logger.info("Created local JPA container and engine");
                    engine = new Engine(new JpaEngineDAO(em, false), new LocalLockManager(), new LocalAttachmentManager(), engineName);

                    this.fileName = fileName;
                    this.password = password;

                    remote = false;
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } catch (final IOException e) {
            logger.info(e.getLocalizedMessage());
        }

        return engine;
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

        // Remove the existing files so we don't mix entities and cause corruption
        if (file.exists()) {
            deleteDatabase(file);
        }

        initEmptyDatabase(file.getAbsolutePath());

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

    /**
     * Returns the string representation of this {@code DataStore}.
     *
     * @return string representation of this {@code DataStore}.
     */
    @Override
    public String toString() {
        return getType().toString();
    }

    private boolean exists(final String fileName) {
        return Files.exists(Paths.get(FileUtils.stripFileExtension(fileName) + "." + getFileExt()));
    }

    boolean initEmptyDatabase(final String fileName) {
        boolean result = false;

        final Properties properties = JpaConfiguration.getLocalProperties(getType(), fileName, new char[]{}, false);
        final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

        try (final Connection connection = DriverManager.getConnection(url, "sa", "")) {
            connection.prepareStatement("CREATE USER " + JpaConfiguration.DEFAULT_USER + " PASSWORD \"\" ADMIN").execute();
            connection.commit();

            connection.prepareStatement("SHUTDOWN").execute(); // absolutely required for a correct shutdown
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        try (final Connection connection = DriverManager.getConnection(url, JpaConfiguration.DEFAULT_USER, "")) {
            connection.prepareStatement("DROP USER SA").execute();
            connection.commit();

            connection.prepareStatement("SHUTDOWN").execute(); // absolutely required for a correct shutdown
            result = true;
        } catch (final SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        waitForLockFileRelease(fileName, new char[]{});

        logger.log(Level.INFO, "Initialized an empty database for {0}", FileUtils.stripFileExtension(fileName));

        return result;
    }

    /**
     * Deletes a database and associated files and directories
     *
     * @param file one of the primary database files
     */
    protected abstract void deleteDatabase(final File file);

    /**
     * Return the extension used by the lock file with the preceding period
     *
     * @return lock file extension
     */
    protected abstract String getLockFileExtension();
}
