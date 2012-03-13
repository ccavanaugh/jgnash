/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.KXml2Driver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.Config;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.ExchangeRate;
import jgnash.engine.ExchangeRateHistoryNode;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.StoredObject;
import jgnash.engine.StoredObjectComparator;
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
import jgnash.engine.recurring.Reminder;
import jgnash.util.FileMagic;
import jgnash.util.FileUtils;

/**
 * Simple object container for StoredObjects that reads and writes a xml file
 * 
 * @author Craig Cavanaugh
 * @version $Id: XMLContainer.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class XMLContainer {

    private final List<StoredObject> objects = new ArrayList<StoredObject>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private final File file;

    private FileLock fileLock = null;

    private FileChannel lockChannel = null;

    protected XMLContainer(final File file) {
        this.file = file;
    }

    @SuppressWarnings({ "ChannelOpenedButNotSafelyClosed", "IOResourceOpenedButNotSafelyClosed" })
    private boolean acquireFileLock() {
        try {
            lockChannel = new RandomAccessFile(file, "rw").getChannel();
            fileLock = lockChannel.tryLock();
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OverlappingFileLockException ex) {
            Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private void releaseFileLock() {
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
            Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void commit() {
        writeXML();
    }

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
     * Returns a list of objects that are assignable from from the specified Class.
     * <p>
     * The returned list may be modified without causing side effects
     * 
     * @param <T> the type of class to query
     * @param values Collection of objects to query
     * @param clazz the Class to query for
     * @return A list of type T containing objects of type clazz
     */
    @SuppressWarnings("unchecked")
    private static <T extends StoredObject> List<T> query(final Collection<StoredObject> values, final Class<T> clazz) {
        ArrayList<T> list = new ArrayList<T>();

        for (StoredObject o : values) {
            if (clazz.isAssignableFrom(o.getClass())) {
                list.add((T) o);
            }
        }
        return list;
    }

    /**
     * Returns of list of all <code>StoredObjects</code> held within this container. The returned list is a defensive
     * copy.
     * 
     * @return A list of all <code>StoredObjects</code>
     * @see StoredObject
     */
    List<StoredObject> asList() {
        ArrayList<StoredObject> list = null;

        readWriteLock.readLock().lock();

        try {
            list = new ArrayList<StoredObject>(objects);
        } finally {
            readWriteLock.readLock().unlock();
        }

        return list;
    }

    private synchronized void writeXML() {
        readWriteLock.readLock().lock();

        try {
            releaseFileLock();
            writeXML(objects, file);
        } finally {
            acquireFileLock();
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * Writes an XML file given a collection of StoredObjects. TrashObjects and objects marked for removal are not
     * written. If the file already exists, it will be overwritten.
     * 
     * @param objects Collection of StoredObjects to write
     * @param file file to write
     */
    public static synchronized void writeXML(final Collection<StoredObject> objects, final File file) {
        Logger logger = Logger.getLogger(XMLContainer.class.getName());

        if (file.exists()) {
            File backup = new File(file.getAbsolutePath() + ".backup");
            if (backup.exists()) {
                if (!backup.delete()) {
                    logger.log(Level.WARNING, "Was not able to delete the old backup file: {0}", backup.getAbsolutePath());
                }
            }
            try {
                FileUtils.copyFile(file, backup);
            } catch (FileNotFoundException e) {
                logger.log(Level.SEVERE, null, e);
            }
        }

        List<StoredObject> list = new ArrayList<StoredObject>();

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

        logger.info("Writing XML file");
        ObjectOutputStream out = null;

        Writer writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

            try {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writer.write("<?fileVersion " + Engine.CURRENT_VERSION + "?>\n");

                XStream xstream = configureXStream(new XStream(new PureJavaReflectionProvider(), new KXml2Driver()));

                out = xstream.createObjectOutputStream(new PrettyPrintWriter(writer));
                out.writeObject(list);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }

                try {
                    writer.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, null, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
        }
        logger.info("Writing XML file complete");
    }

    void readXML() throws FileNotFoundException, UnsupportedEncodingException {
        String encoding = System.getProperty("file.encoding"); // system default encoding
        String version = FileMagic.getjGnashXMLVersion(file); // version of the jGnash XML file

        if (Float.parseFloat(version) >= 2.01f) { // 2.01f is hard coded for prior encoding bug
            encoding = "UTF-8"; // encoding is always UTF-8 for anything greater than 2.0
        }

        ObjectInputStream in = null;
        FileLock readLock = null; // obtain a shared lock for reading
        FileInputStream fis = new FileInputStream(file);

        Reader reader = new BufferedReader(new InputStreamReader(fis, encoding));

        readWriteLock.writeLock().lock();

        try {
            XStream xstream = configureXStream(new XStream(new StoredObjectReflectionProvider(objects), new KXml2Driver()));

            readLock = fis.getChannel().tryLock(0, Long.MAX_VALUE, true);

            if (readLock != null) {
                in = xstream.createObjectInputStream(reader);
                in.readObject();
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (in != null) {
                try {
                    if (readLock != null) {
                        readLock.release();
                    }
                    in.close();
                } catch (IOException e) {
                    Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, e);
                }
            }

            try {
                reader.close();
            } catch (IOException e) {
                Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, e);
            }

            try {
                fis.close();
            } catch (IOException e) {
                Logger.getLogger(XMLContainer.class.getName()).log(Level.SEVERE, null, e);
            }

            acquireFileLock(); // lock the file on open

            readWriteLock.writeLock().unlock();
        }
    }

    private static XStream configureXStream(final XStream xstream) {
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

        return xstream;
    }

    private static final class StoredObjectReflectionProvider extends PureJavaReflectionProvider {

        private final List<StoredObject> objects;

        protected StoredObjectReflectionProvider(final List<StoredObject> objects) {
            this.objects = objects;
        }

        @SuppressWarnings({ "rawtypes" })
        @Override
        public Object newInstance(final Class type) {
            Object o = super.newInstance(type);

            if (o instanceof StoredObject) {
                objects.add((StoredObject) o);
            }
            return o;
        }
    }
}
