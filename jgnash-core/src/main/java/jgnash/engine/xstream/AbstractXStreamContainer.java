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
import java.util.stream.Collectors;

import jgnash.engine.Account;
import jgnash.engine.AmortizeObject;
import jgnash.engine.CommodityNode;
import jgnash.engine.Config;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ExchangeRate;
import jgnash.engine.ExchangeRateHistoryNode;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.StoredObject;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryAddX;
import jgnash.engine.TransactionEntryBuyX;
import jgnash.engine.TransactionEntryDividendX;
import jgnash.engine.TransactionEntryMergeX;
import jgnash.engine.TransactionEntryReinvestDivX;
import jgnash.engine.TransactionEntryRemoveX;
import jgnash.engine.TransactionEntrySellX;
import jgnash.engine.TransactionEntrySplitX;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.util.NotNull;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentCollectionConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentMapConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentSortedMapConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentSortedSetConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernateProxyConverter;
import com.thoughtworks.xstream.hibernate.mapper.HibernateMapper;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

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
    @NotNull static <T extends StoredObject> List<T> query(final Collection<StoredObject> values, final Class<T> clazz) {
        return values.parallelStream().filter(o -> clazz.isAssignableFrom(o.getClass()))
                .map(o -> (T) o).collect(Collectors.toList());
    }

    static XStream configureXStream(final XStream xstream) {
        xstream.ignoreUnknownElements();    // gracefully ignore fields in the file that do not have object members

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

        // Converters for new Java time API
        xstream.registerConverter(new LocalDateConverter());
        xstream.registerConverter(new LocalDateTimeConverter());

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

        readWriteLock.writeLock().lock();

        try {
            if (get(object.getUuid()) == null) { // make sure the UUID is unique before adding
                objects.add(object);
            }
            result = true;
        } catch (final Exception ex) {
            Logger.getLogger(AbstractXStreamContainer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.writeLock().unlock();
        }

        return result;
    }

    void delete(final StoredObject object) {
        readWriteLock.writeLock().lock();

        try {
            objects.remove(object);
        } finally {
            readWriteLock.writeLock().unlock();
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
     * Returns of list of all {@code StoredObjects} held within this container. The returned list is a defensive
     * copy.
     *
     * @return A list of all {@code StoredObjects}
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

    static class XStreamOut extends XStream {

        public XStreamOut(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver hierarchicalStreamDriver) {
            super(reflectionProvider, hierarchicalStreamDriver);
        }

        @Override
        protected MapperWrapper wrapMapper(final MapperWrapper next) {
            return new HibernateMapper(next);
        }
    }
}
