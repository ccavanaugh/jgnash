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
package jgnash.engine.xstream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.CommodityNode;
import jgnash.engine.Config;
import jgnash.engine.ExchangeRate;
import jgnash.engine.RootAccount;
import jgnash.engine.StoredObject;
import jgnash.engine.StoredObjectComparator;
import jgnash.engine.Tag;
import jgnash.engine.budget.Budget;
import jgnash.engine.recurring.Reminder;
import jgnash.util.NotNull;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;

/**
 * Simple object container for StoredObjects that reads and writes a binary file
 * using XStream.
 *
 * @author Craig Cavanaugh
 */
class BinaryContainer extends AbstractXStreamContainer {

    BinaryContainer(final Path path) {
        super(path);
    }

    @Override
    void commit() {
        writeBinary();
    }

    private synchronized void writeBinary() {
        readWriteLock.readLock().lock();

        try {
            releaseFileLock();
            writeBinary(objects, path, ignored -> { });
        } finally {
            if (!acquireFileLock()) { // lock the file on open
                Logger.getLogger(BinaryContainer.class.getName()).severe("Could not acquire the file lock");
            }
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * Writes an XML file given a collection of StoredObjects. TrashObjects and
     * objects marked for removal are not written. If the file already exists,
     * it will be overwritten.
     *
     * @param objects Collection of StoredObjects to write
     * @param path    file to write
     */
    static synchronized void writeBinary(@NotNull final Collection<StoredObject> objects, @NotNull final Path path,
                                         @NotNull final DoubleConsumer percentCompleteConsumer) {

        final Logger logger = Logger.getLogger(BinaryContainer.class.getName());

        if (!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
                logger.info("Created missing directories");
            } catch (final IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }

        percentCompleteConsumer.accept(0);

        createBackup(path);

        List<StoredObject> list = new ArrayList<>();

        list.addAll(query(objects, Budget.class));
        list.addAll(query(objects, Config.class));
        list.addAll(query(objects, CommodityNode.class));
        list.addAll(query(objects, ExchangeRate.class));
        list.addAll(query(objects, RootAccount.class));
        list.addAll(query(objects, Reminder.class));
        list.addAll(query(objects, Tag.class));

        percentCompleteConsumer.accept(0.25);

        // remove any objects marked for removal
        list.removeIf(StoredObject::isMarkedForRemoval);

        // sort the list
        list.sort(new StoredObjectComparator());

        percentCompleteConsumer.accept(0.5);

        logger.info("Writing Binary file");

        try (final OutputStream os = new BufferedOutputStream(Files.newOutputStream(path))) {

            final XStream xstream = configureXStream(new XStreamOut(new PureJavaReflectionProvider(),
                    new BinaryStreamDriver()));

            try (final ObjectOutputStream out = xstream.createObjectOutputStream(os)) {
                out.writeObject(list);
                out.flush();
            }

            os.flush(); // forcibly flush before letting go of the resources to help older windows systems write correctly
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        logger.info("Writing Binary file complete");

        percentCompleteConsumer.accept(1);
    }

    void readBinary() {

        // A file lock will be held on Windows OS when reading
        try (final InputStream fis = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ))) {
            readWriteLock.writeLock().lock();

            final XStream xstream = configureXStream(new XStreamJVM9(new StoredObjectReflectionProvider(objects),
                    new BinaryStreamDriver()));

            try (final ObjectInputStream in = xstream.createObjectInputStream(fis)) {
                in.readObject();
            }

        } catch (final IOException | ClassNotFoundException e) {
            Logger.getLogger(BinaryContainer.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (!acquireFileLock()) { // lock the file on open
                Logger.getLogger(BinaryContainer.class.getName()).severe("Could not acquire the file lock");
            }
            readWriteLock.writeLock().unlock();
        }
    }
}
