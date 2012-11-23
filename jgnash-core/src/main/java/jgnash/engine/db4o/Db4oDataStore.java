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
package jgnash.engine.db4o;

import com.db4o.DatabaseClosedException;
import com.db4o.DatabaseFileLockedException;
import com.db4o.DatabaseReadOnlyException;
import com.db4o.Db4o;
import com.db4o.Db4oIOException;
import com.db4o.IncompatibleFileFormatException;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.Configuration;
import com.db4o.diagnostic.Diagnostic;
import com.db4o.diagnostic.DiagnosticListener;
import com.db4o.diagnostic.NativeQueryNotOptimized;
import com.db4o.events.Event4;
import com.db4o.events.EventArgs;
import com.db4o.events.EventListener4;
import com.db4o.events.EventRegistry;
import com.db4o.events.EventRegistryFactory;
import com.db4o.events.ObjectEventArgs;
import com.db4o.ext.OldFormatException;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountProperty;
import jgnash.engine.Config;
import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStore;
import jgnash.engine.Engine;
import jgnash.engine.ExchangeRate;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;
import jgnash.engine.StoredObject;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TrashObject;
import jgnash.engine.budget.Budget;
import jgnash.engine.db4o.config.TBigDecimal;
import jgnash.engine.db4o.config.TBigInteger;
import jgnash.engine.db4o.config.TEnum;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.MonthlyReminder;
import jgnash.engine.recurring.OneTimeReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.engine.recurring.WeeklyReminder;
import jgnash.engine.recurring.YearlyReminder;
import jgnash.util.Resource;

/**
 * db4o specific code for data storage and creating an engine
 * 
 * @author Craig Cavanaugh
 *
 */
public class Db4oDataStore implements DataStore {

    private static final String FILE_EXT = "jdb";

    private boolean remote;

    private String fileName;

    private ObjectContainer db;

    private EventListener4 transactionDeletedListener;

    private EventListener4 accountDeletedListener;

    private static final boolean DEBUG = false;

    static Configuration createConfig() {

        Configuration config = Db4o.newConfiguration();

        config.optimizeNativeQueries(true);
        config.callConstructors(true);
        config.reserveStorageSpace(400000); // preallocate for large import

        if (DEBUG) {
            config.diagnostic().addListener(new DiagnosticListener() {

                @Override
                public void onDiagnostic(Diagnostic d) {
                    if (d instanceof NativeQueryNotOptimized) {
                        Logger.getLogger(Db4oDataStore.class.getName()).log(Level.INFO, "{0}\n{1}\n{2}", new Object[]{((NativeQueryNotOptimized) d).problem(), ((NativeQueryNotOptimized) d).reason(), ((NativeQueryNotOptimized) d).solution()});
                    }
                }
            });
        }

        config.objectClass(BigDecimal.class).translate(new TBigDecimal());
        config.objectClass(BigInteger.class).translate(new TBigInteger());
        config.objectClass(Enum.class).translate(new TEnum());

        config.objectClass(ExchangeRate.class).cascadeOnUpdate(true);
        config.objectClass(ExchangeRate.class).cascadeOnActivate(true);

        config.objectClass(SecurityNode.class).cascadeOnUpdate(true);
        config.objectClass(SecurityNode.class).cascadeOnActivate(true);

        config.objectClass(CurrencyNode.class).cascadeOnActivate(true);
        config.objectClass(CurrencyNode.class).cascadeOnUpdate(true);

        config.objectClass(Budget.class).cascadeOnActivate(true);
        config.objectClass(Budget.class).cascadeOnUpdate(true);

        config.objectClass(Config.class).cascadeOnActivate(true);
        config.objectClass(Config.class).cascadeOnUpdate(true);

        config.objectClass(Account.class).cascadeOnActivate(true);

        config.objectClass(Account.class).objectField("propertyMap").cascadeOnUpdate(true);
        config.objectClass(Account.class).updateDepth(1);
        config.objectClass(RootAccount.class).updateDepth(1);

        config.objectClass(DailyReminder.class).cascadeOnActivate(true);
        config.objectClass(DailyReminder.class).cascadeOnUpdate(true);

        config.objectClass(MonthlyReminder.class).cascadeOnActivate(true);
        config.objectClass(MonthlyReminder.class).cascadeOnUpdate(true);

        config.objectClass(OneTimeReminder.class).cascadeOnActivate(true);
        config.objectClass(OneTimeReminder.class).cascadeOnUpdate(true);

        config.objectClass(WeeklyReminder.class).cascadeOnActivate(true);
        config.objectClass(WeeklyReminder.class).cascadeOnUpdate(true);

        config.objectClass(YearlyReminder.class).cascadeOnActivate(true);
        config.objectClass(YearlyReminder.class).cascadeOnUpdate(true);

        config.objectClass(Reminder.class).cascadeOnActivate(true);
        config.objectClass(Reminder.class).cascadeOnUpdate(true);

        config.objectClass(TrashObject.class).cascadeOnActivate(true);
        config.objectClass(TrashObject.class).cascadeOnUpdate(true);

        config.objectClass(InvestmentTransaction.class).cascadeOnActivate(true);
        config.objectClass(Transaction.class).cascadeOnActivate(true);
        config.objectClass(TransactionEntry.class).cascadeOnActivate(true);

        // Establish Indexes       
        config.objectClass(StoredObject.class).indexed(true);
        config.objectClass(Account.class).indexed(true);
        config.objectClass(Budget.class).indexed(true);
        config.objectClass(CurrencyNode.class).indexed(true);
        config.objectClass(Reminder.class).indexed(true);
        config.objectClass(RootAccount.class).indexed(true);

        config.objectClass(StoredObject.class).objectField("uuid").indexed(true);
        config.objectClass(StoredObject.class).objectField("markedForRemoval").indexed(true);

        config.objectClass(Account.class).objectField("accountType").indexed(true);

        config.objectClass(ExchangeRate.class).objectField("rateId").indexed(true);

        config.exceptionsOnNotStorable(true);
        config.automaticShutDown(true);
        config.messageLevel(1);

        return config;
    }

    /**
     * Returns the default file extension for this <code>DataStore</code>
     * @see DataStore#getFileExt() 
     * @see Db4oDataStore#FILE_EXT
     */
    @Override
    public final String getFileExt() {
        return FILE_EXT;
    }

    /**
     * Returns the full path to the file the DataStore is using.
     * @see DataStore#getFileName()     
     */
    @Override
    public final String getFileName() {
        return fileName;
    }

    private ObjectContainer createLocalContainer(final String filename) {
        Configuration config = createConfig();

        File file = new File(filename);

        // create the base directory if needed
        if (!file.exists()) {
            File parent = file.getParentFile();

            if (parent != null && !parent.exists()) {
                boolean result = parent.mkdirs();

                if (!result) {
                    throw new RuntimeException("Could not create directory for file: " + parent.getAbsolutePath());
                }
            }
        }

        ObjectContainer container = Db4o.openFile(config, filename);

        registerListeners(container);

        return container;
    }

    private ObjectContainer createClientContainer(final String host, final int port, final String user, final String password) {
        Configuration config = createConfig();

        ObjectContainer container = Db4o.openClient(config, host, port, user, password);

        registerListeners(container);

        return container;
    }

    private void registerListeners(final ObjectContainer container) {
        EventRegistry registry = EventRegistryFactory.forObjectContainer(container);

        transactionDeletedListener = new TransactionDeletedListener(container);
        accountDeletedListener = new AccountDeletedListener(container);

        registry.deleted().addListener(transactionDeletedListener);
        registry.deleted().addListener(accountDeletedListener);
    }

    private void unregisterListeners(final ObjectContainer container) {
        EventRegistry registry = EventRegistryFactory.forObjectContainer(container);

        registry.deleted().removeListener(transactionDeletedListener);
        registry.deleted().removeListener(accountDeletedListener);

        if (DEBUG) {
            container.ext().configure().diagnostic().removeAllListeners();
        }

        transactionDeletedListener = null;
        accountDeletedListener = null;
    }

    /**   
     * Create an engine instance that uses a local db4o file
     * 
     * @see DataStore#getLocalEngine(java.lang.String, java.lang.String)  
     */
    @Override
    public Engine getLocalEngine(final String filename, final String engineName) {
        db = createLocalContainer(filename);

        Engine engine = null;

        if (db != null) {
            Logger.getLogger(Db4oDataStore.class.getName()).info("Created local db4o container and engine");
            engine = new Engine(new Db4oEngineDAO(db, false), engineName);
            this.fileName = filename;
            remote = false;
        }

        return engine;
    }

    /**
     * @see DataStore#getClientEngine(java.lang.String, int, java.lang.String, java.lang.String, java.lang.String)     
     */
    @Override
    public Engine getClientEngine(final String host, final int port, final String user, final String password, final String engineName) {
        db = createClientContainer(host, port, user, password);

        Engine engine = null;

        if (db != null) {
            Logger.getLogger(Db4oDataStore.class.getName()).info("Created client db4o container and engine");
            engine = new Engine(new Db4oEngineDAO(db, true), engineName);
            fileName = null;
            remote = true;
        }
        return engine;
    }

    /**
     * Close the open <code>Engine</code>
     * 
     * @see DataStore#closeEngine() 
     */
    @Override
    public void closeEngine() {
        if (db != null) {
            db.commit(); // force a commit before close

            // remove deletion listeners
            unregisterListeners(db);

            db.close(); // close the db4o store

            // Force a really long delay to ensure the db4o file is closed.
            try {
                Thread.sleep(10000); //
            } catch (InterruptedException e) {
                Logger.getLogger(Db4oDataStore.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * @see DataStore#isRemote() 
     */
    @Override
    public boolean isRemote() {
        return remote;
    }

    /**
     * @see DataStore#saveAs(java.io.File, java.util.Collection)  
     */
    @Override
    public void saveAs(final File file, final Collection<StoredObject> objects) {
        Configuration config = createConfig();

        ObjectContainer container = Db4o.openFile(config, file.getAbsolutePath());

        for (StoredObject o : objects) {
            container.set(o);
        }

        container.commit();
        container.close();
    }

    /**
     * Returns the string representation of this <code>DataStore</code>.
     * @return string representation of this <code>DataStore</code>.
     */
    @Override
    public String toString() {
        return Resource.get().getString("DataStoreType.Db4o");
    }

    /**
     * Opens and db4o file in readonly mode and reads the version of the file format.
     * 
     * @param file <code>File</code> to open
     * @return file version
     */
    public static float getFileVersion(final File file) {

        float fileVersion = 0;

        Configuration config = createConfig();
        config.readOnly(true);

        ObjectContainer container = null;

        try {
            container = Db4o.openFile(config, file.getAbsolutePath());

            ObjectSet<Config> set = container.query(Config.class);

            if (set.size() == 1) {
                fileVersion = set.get(0).getFileVersion();

            } else {
                Logger.getLogger(Db4oDataStore.class.getName()).severe("Invalid file");
            }
        } catch (Db4oIOException | DatabaseFileLockedException | IncompatibleFileFormatException | OldFormatException | DatabaseReadOnlyException | DatabaseClosedException e) {
            container = null;
            Logger.getLogger(Db4oDataStore.class.getName()).log(Level.WARNING, "Tried to open an incompatible file version", e);
        } finally {
            if (container != null) {
                container.close();
            }
        }

        return fileVersion;
    }

    private static class TransactionDeletedListener implements EventListener4 {

        private final ObjectContainer container;

        TransactionDeletedListener(ObjectContainer container) {
            this.container = container;
        }

        @Override
        public void onEvent(final Event4 e, final EventArgs args) {
            if (args instanceof ObjectEventArgs) {
                ObjectEventArgs queryArgs = (ObjectEventArgs) args;
                Object obj = queryArgs.object();
                if (obj instanceof Transaction) {
                    for (TransactionEntry entry : ((Transaction) obj).getTransactionEntries()) {
                        container.delete(entry);
                        container.ext().purge(entry);
                    }
                }
            } else {
                throw new RuntimeException("Unexpected event");
            }
        }
    }

    private static class AccountDeletedListener implements EventListener4 {

        private final ObjectContainer container;

        AccountDeletedListener(ObjectContainer container) {
            this.container = container;
        }

        @Override
        public void onEvent(final Event4 e, final EventArgs args) {
            if (args instanceof ObjectEventArgs) {
                ObjectEventArgs queryArgs = (ObjectEventArgs) args;
                Object obj = queryArgs.object();
                if (obj instanceof Account) {
                    Account account = (Account) obj;

                    for (AccountProperty key : account.getProperties()) {
                        Object o = account.getProperty(key);
                        container.delete(o);
                        container.ext().purge(o);
                    }
                }
            } else {
                throw new RuntimeException("Unexpected event");
            }
        }
    }
}
