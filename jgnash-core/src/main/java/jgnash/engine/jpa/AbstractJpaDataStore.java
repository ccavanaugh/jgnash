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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.DoubleConsumer;
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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.math3.util.Precision;

/**
 * Abstract JPA DataStore.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractJpaDataStore implements DataStore {

    private static final String SHUTDOWN = "SHUTDOWN";

    private static final int PARTITION_SIZE = 200;

    private EntityManager em;

    private EntityManagerFactory factory;

    private DistributedLockManager distributedLockManager;

    private DistributedAttachmentManager distributedAttachmentManager;

    private boolean local = true;

    private String fileName;

    private static final boolean DEBUG = false;

    private char[] password;

    static final Logger logger = Logger.getLogger(AbstractJpaDataStore.class.getName());

    private void waitForLockFileRelease(final String fileName, final char[] password) {

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

        if (local) {
            waitForLockFileRelease(fileName, password);
        } else {
            distributedLockManager.disconnectFromServer();
            distributedAttachmentManager.disconnectFromServer();
        }
    }

    @Override
    public Engine getClientEngine(final String host, final int port, final char[] password, final String dataBasePath) {
        final Properties properties
                = JpaConfiguration.getClientProperties(getType(), dataBasePath, host, port, password);

        Engine engine = null;

        try {
            if (SqlUtils.isConnectionValid(properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL))) {
                factory = Persistence.createEntityManagerFactory(JpaConfiguration.UNIT_NAME, properties);

                em = factory.createEntityManager();

                if (em != null) {
                    distributedLockManager = new DistributedLockManager(host, port
                            + JpaNetworkServer.LOCK_SERVER_INCREMENT);

                    boolean lockManagerResult = distributedLockManager.connectToServer(password);

                    distributedAttachmentManager = new DistributedAttachmentManager(host, port
                            + JpaNetworkServer.TRANSFER_SERVER_INCREMENT);

                    boolean attachmentManagerResult = distributedAttachmentManager.connectToServer(password);

                    if (attachmentManagerResult && lockManagerResult) {
                        engine = new Engine(new JpaEngineDAO(em, true), distributedLockManager,
                                distributedAttachmentManager, EngineFactory.DEFAULT);

                        logger.info("Created local JPA container and engine");
                        fileName = null;
                        local = false;
                    } else {
                        distributedLockManager.disconnectFromServer();
                        distributedAttachmentManager.disconnectFromServer();

                        em.close();
                        factory.close();
                        em = null;
                        factory = null;
                    }
                }
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.toString(), e);
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

        if (!exists(fileName) && !initEmptyDatabase(fileName)) {
            return null;
        }

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                try {
                    float fileVersion = SqlUtils.getFileVersion(fileName, password);

                    if (Precision.equals(fileVersion, 3.5f)) {
                        SqlUtils.dropColumn(fileName, password, "TAG", "SHAPE", "ICONSET");
                        logger.info("Dropped old TAG columns");
                    }

                    /* specifies the unit name and properties.  Unit name can be used to specify a different persistence
                       unit defined in persistence.xml */
                    factory = Persistence.createEntityManagerFactory(JpaConfiguration.UNIT_NAME, properties);
                    em = factory.createEntityManager();

                    logger.info("Created local JPA container and engine");
                    engine = new Engine(new JpaEngineDAO(em, false), new LocalLockManager(),
                            new LocalAttachmentManager(), engineName);

                    this.fileName = fileName;
                    this.password = password.clone();   // clone to protect against side effects

                    local = true;
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
    public boolean isLocal() {
        return local;
    }

    @Override
    public void saveAs(final Path path, final Collection<StoredObject> objects, final DoubleConsumer percentComplete) {

        final int collectionSize = objects.size();

        // Remove the existing files so we don't mix entities and cause corruption
        if (Files.exists(path)) {
            deleteDatabase(path.toString());
        }

        if (initEmptyDatabase(path.toString())) {

            final Properties properties = JpaConfiguration.getLocalProperties(getType(), path.toString(),
                    new char[]{}, false);

            EntityManagerFactory emFactory = null;
            EntityManager entityManager = null;

            try {
                emFactory = Persistence.createEntityManagerFactory(JpaConfiguration.UNIT_NAME, properties);
                entityManager = emFactory.createEntityManager();

                final List<List<StoredObject>> partitions = ListUtils.partition(new ArrayList<>(objects), PARTITION_SIZE);

                int writeCount = 0;

                for (final List<StoredObject> partition : partitions) {
                    entityManager.getTransaction().begin();

                    for (final StoredObject o : partition) {
                        entityManager.persist(o);
                        writeCount++;

                        percentComplete.accept((double) writeCount / (double) collectionSize);
                    }

                    entityManager.getTransaction().commit();
                }
            } catch (final Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            } finally {
                if (entityManager != null) {
                    entityManager.close();
                }

                if (emFactory != null) {
                    emFactory.close();
                }
            }

            waitForLockFileRelease(path.toString(), new char[]{});
        }
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
        return Files.exists(Paths.get(FileUtils.stripFileExtension(fileName) + getFileExt()));
    }

    /**
     * Opens and closes the database in order to create a new file.
     *
     * @param fileName database file
     * @return {@code true} if successful
     */
    private boolean initEmptyDatabase(final String fileName) {
        boolean result = false;

        final Properties properties = JpaConfiguration.getLocalProperties(getType(), fileName,
                EngineFactory.EMPTY_PASSWORD, false);

        final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

        try (final Connection connection = DriverManager.getConnection(url)) {

            // absolutely required for a correct shutdown
            try (final PreparedStatement statement = connection.prepareStatement(SHUTDOWN)) {
                statement.execute();
            }

            result = true;
        } catch (final SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        waitForLockFileRelease(fileName, EngineFactory.EMPTY_PASSWORD);

        logger.log(Level.INFO, "Initialized an empty database for {0}", fileName);

        return result;
    }

    /**
     * Deletes a database and associated files and directories.
     *
     * @param fileName one of the primary database files
     */
    protected abstract void deleteDatabase(final String fileName);

    /**
     * Return the extension used by the lock file with the preceding period.
     *
     * @return lock file extension
     */
    protected abstract String getLockFileExtension();
}
