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
package jgnash.engine.xstream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.CommodityNode;
import jgnash.engine.Config;
import jgnash.engine.ExchangeRate;
import jgnash.engine.RootAccount;
import jgnash.engine.StoredObject;
import jgnash.engine.StoredObjectComparator;
import jgnash.engine.budget.Budget;
import jgnash.engine.recurring.Reminder;
import jgnash.util.FileUtils;
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

    BinaryContainer(final File file) {
        super(file);
    }

    @Override
    void commit() {
        writeBinary();
    }

    private synchronized void writeBinary() {
        readWriteLock.readLock().lock();

        try {
            releaseFileLock();
            writeBinary(objects, file);
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
     * @param file    file to write
     */
    public static synchronized void writeBinary(@NotNull final Collection<StoredObject> objects, @NotNull final File file) {
        final Logger logger = Logger.getLogger(BinaryContainer.class.getName());

        if (file.getParentFile().mkdirs()) {
            logger.info("Created missing directories");
        }

        if (file.exists()) {
            File backup = new File(file.getAbsolutePath() + ".backup");
            if (backup.exists()) {
                if (!backup.delete()) {
                    logger.log(Level.WARNING, "Was not able to delete the old backup file: {0}",
                            backup.getAbsolutePath());
                }
            }

            FileUtils.copyFile(file, backup);
        }

        List<StoredObject> list = new ArrayList<>();

        list.addAll(query(objects, Budget.class));
        list.addAll(query(objects, Config.class));
        list.addAll(query(objects, CommodityNode.class));
        list.addAll(query(objects, ExchangeRate.class));
        list.addAll(query(objects, RootAccount.class));
        list.addAll(query(objects, Reminder.class));

        // remove any objects marked for removal
        Iterator<StoredObject> i = list.iterator();
        while (i.hasNext()) {
            StoredObject o = i.next();
            if (o.isMarkedForRemoval()) {
                i.remove();
            }
        }

        // sort the list
        Collections.sort(list, new StoredObjectComparator());

        logger.info("Writing Binary file");

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {

            XStream xstream = configureXStream(new XStreamOut(new PureJavaReflectionProvider(), new BinaryStreamDriver()));

            try (ObjectOutputStream out = xstream.createObjectOutputStream(os)) {
                out.writeObject(list);
                out.flush();
            }

            os.flush(); // forcibly flush before letting go of the resources to help older windows systems write correctly
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        logger.info("Writing Binary file complete");
    }

    void readBinary() {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream inputStream = new BufferedInputStream(fis)) {

            readWriteLock.writeLock().lock();

            final XStream xstream = configureXStream(new XStream(new StoredObjectReflectionProvider(objects),
                    new BinaryStreamDriver()));

            // Filters out any java.sql.Dates that sneaked in when saving from a relational database
            // and forces to a LocalDate
            xstream.alias("sql-date", LocalDate.class);

            try (final ObjectInputStream in = xstream.createObjectInputStream(inputStream);
                 FileLock readLock = fis.getChannel().tryLock(0, Long.MAX_VALUE, true)) {
                if (readLock != null) {
                    in.readObject();
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(BinaryContainer.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (!acquireFileLock()) { // lock the file on open
                Logger.getLogger(BinaryContainer.class.getName()).severe("Could not acquire the file lock");
            }
            readWriteLock.writeLock().unlock();
        }
    }
}
