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
package jgnash.engine.xstream;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.hibernate.converter.*;
import com.thoughtworks.xstream.hibernate.mapper.HibernateMapper;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import jgnash.engine.*;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.budget.BudgetPeriod;

/**
 * Abstract XStream container
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractXStreamContainer {
    final List<StoredObject> objects = new ArrayList<>();
    final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    final File file;
    private FileLock fileLock = null;
    private FileChannel lockChannel = null;

    AbstractXStreamContainer(final File file) {
        this.file = file;
    }

    /**
     * Returns a list of objects that are assignable from from the specified Class.
     * <p/>
     * The returned list may be modified without causing side effects
     *
     * @param <T>    the type of class to query
     * @param values Collection of objects to query
     * @param clazz  the Class to query for
     * @return A list of type T containing objects of type clazz
     */
    @SuppressWarnings("unchecked")
    static <T extends StoredObject> List<T> query(final Collection<StoredObject> values, final Class<T> clazz) {
        ArrayList<T> list = new ArrayList<>();

        for (StoredObject o : values) {
            if (clazz.isAssignableFrom(o.getClass())) {
                list.add((T) o);
            }
        }
        return list;
    }

    static XStream configureXStream(final XStream xstream) {
        xstream.setMode(XStream.ID_REFERENCES);

        xstream.alias("Account", Account.class);
        xstream.alias("RootAccount", RootAccount.class);
        xstream.alias("Budget", Budget.class);
        xstream.alias("BudgetGoal", BudgetGoal.class);
        xstream.alias("Config", Config.class);
        xstream.alias("CurrencyNode", CurrencyNode.class);
        xstream.alias("ExchangeRate", ExchangeRate.class);
        xstream.alias("ExchangeRateHistoryNode", ExchangeRateHistoryNode.class);
        xstream.alias("InvestmentTransaction", InvestmentTransaction.class);
        xstream.alias("BudgetPeriod", BudgetPeriod.class);
        xstream.alias("SecurityNode", SecurityNode.class);
        xstream.alias("SecurityHistoryNode", SecurityHistoryNode.class);
        xstream.alias("Transaction", Transaction.class);
        xstream.alias("TransactionEntry", TransactionEntry.class);
        xstream.alias("TransactionEntryAddX", TransactionEntryAddX.class);
        xstream.alias("TransactionEntryBuyX", TransactionEntryBuyX.class);
        xstream.alias("TransactionEntryDividendX", TransactionEntryDividendX.class);
        xstream.alias("TransactionEntryMergeX", TransactionEntryMergeX.class);
        xstream.alias("TransactionEntryReinvestDivX", TransactionEntryReinvestDivX.class);
        xstream.alias("TransactionEntryRemoveX", TransactionEntryRemoveX.class);
        xstream.alias("TransactionEntrySellX", TransactionEntrySellX.class);
        xstream.alias("TransactionEntrySplitX", TransactionEntrySplitX.class);

        xstream.useAttributeFor(Account.class, "placeHolder");
        xstream.useAttributeFor(Account.class, "locked");
        xstream.useAttributeFor(Account.class, "visible");
        xstream.useAttributeFor(Account.class, "name");
        xstream.useAttributeFor(Account.class, "description");

        xstream.useAttributeFor(Budget.class, "description");
        xstream.useAttributeFor(Budget.class, "name");

        xstream.useAttributeFor(CommodityNode.class, "symbol");
        xstream.useAttributeFor(CommodityNode.class, "scale");
        xstream.useAttributeFor(CommodityNode.class, "prefix");
        xstream.useAttributeFor(CommodityNode.class, "suffix");
        xstream.useAttributeFor(CommodityNode.class, "description");

        xstream.useAttributeFor(SecurityHistoryNode.class, "date");
        xstream.useAttributeFor(SecurityHistoryNode.class, "price");
        xstream.useAttributeFor(SecurityHistoryNode.class, "high");
        xstream.useAttributeFor(SecurityHistoryNode.class, "low");
        xstream.useAttributeFor(SecurityHistoryNode.class, "volume");

        xstream.useAttributeFor(StoredObject.class, "uuid");

        xstream.omitField(StoredObject.class, "markedForRemoval");

        // Ignore fields required for JPA
        xstream.omitField(StoredObject.class, "version");

        xstream.omitField(AmortizeObject.class, "id");
        xstream.omitField(BudgetGoal.class, "id");
        xstream.omitField(TransactionEntry.class, "id");
        xstream.omitField(ExchangeRateHistoryNode.class, "id");
        xstream.omitField(SecurityHistoryNode.class, "id");

        // Filters out the hibernate
        xstream.registerConverter(new HibernateProxyConverter());
        xstream.registerConverter(new HibernatePersistentCollectionConverter(xstream.getMapper()));
        xstream.registerConverter(new HibernatePersistentMapConverter(xstream.getMapper()));
        xstream.registerConverter(new HibernatePersistentSortedMapConverter(xstream.getMapper()));
        xstream.registerConverter(new HibernatePersistentSortedSetConverter(xstream.getMapper()));

        return xstream;
    }

    @SuppressWarnings(value = {"ChannelOpenedButNotSafelyClosed", "IOResourceOpenedButNotSafelyClosed"})
    boolean acquireFileLock() {
        try {
            lockChannel = new RandomAccessFile(file, "rw").getChannel();
            fileLock = lockChannel.tryLock();
            return true;
        } catch (IOException | OverlappingFileLockException ex) {
            Logger.getLogger(AbstractXStreamContainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    void releaseFileLock() {
        try {
            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
            }

            if (lockChannel != null) {
                lockChannel.close();
                lockChannel = null;
            }
        } catch (IOException ex) {
            Logger.getLogger(AbstractXStreamContainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    abstract void commit();

    boolean set(final StoredObject object) {

        boolean result = false;

        Lock l = readWriteLock.writeLock();
        l.lock();

        try {
            if (get(object.getUuid()) == null) { // make sure the UUID is unique before adding
                objects.add(object);
            }
            result = true;
        } finally {
            l.unlock();
        }

        return result;
    }

    void delete(final StoredObject object) {
        Lock l = readWriteLock.writeLock();
        l.lock();

        try {
            objects.remove(object);
        } finally {
            l.unlock();
        }
    }

    StoredObject get(final String uuid) {
        StoredObject result = null;

        Lock l = readWriteLock.readLock();
        l.lock();

        try {
            for (StoredObject o : objects) {
                if (o.getUuid().equals(uuid)) {
                    result = o;
                }
            }
        } finally {
            l.unlock();
        }

        return result;
    }

    <T extends StoredObject> List<T> query(final Class<T> clazz) {
        List<T> list = null;

        Lock l = readWriteLock.readLock();
        l.lock();

        try {
            list = query(objects, clazz);
        } finally {
            l.unlock();
        }

        return list;
    }

    void close() {
        releaseFileLock();
    }

    String getFileName() {
        if (file != null) {
            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     * Returns of list of all <code>StoredObjects</code> held within this container. The returned list is a defensive
     * copy.
     *
     * @return A list of all <code>StoredObjects</code>
     * @see jgnash.engine.StoredObject
     */
    List<StoredObject> asList() {
        ArrayList<StoredObject> list = null;

        readWriteLock.readLock().lock();

        try {
            list = new ArrayList<>(objects);
        } finally {
            readWriteLock.readLock().unlock();
        }

        return list;
    }

    protected static class XStreamOut extends XStream {

        public XStreamOut(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver hierarchicalStreamDriver) {
            super(reflectionProvider, hierarchicalStreamDriver);
        }

        protected MapperWrapper wrapMapper(final MapperWrapper next) {
            return new HibernateMapper(next);
        }
    }

    /**
     * Add a custom wrapper to gracefully ignore fields removed from updated objects
     */
    protected static class XStreamIn extends XStream {

        public XStreamIn(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver hierarchicalStreamDriver) {
            super(reflectionProvider, hierarchicalStreamDriver);
        }

        protected MapperWrapper wrapMapper(final MapperWrapper next) {
            return new MapperWrapper(next) {
                public boolean shouldSerializeMember(final Class definedIn, final String fieldName) {
                    try {

                        // Check and ignore the locale field in CurrencyNode because it is now obsolete
                        if (definedIn == CurrencyNode.class && fieldName.equals("locale")) {
                            return false;
                        }

                        return definedIn != Object.class || realClass(fieldName) != null;
                    } catch (final CannotResolveClassException e) {
                        Logger.getLogger(AbstractXStreamContainer.class.getName()).info("Dropping missing field: " + fieldName);
                        return false;
                    }
                }
            };
        }
    }
}
