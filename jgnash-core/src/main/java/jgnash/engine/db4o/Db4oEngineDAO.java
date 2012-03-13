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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.*;
import jgnash.util.DefaultDaemonThreadFactory;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Query;

/**
 * Db4o DAO Interface
 *
 * @author Craig Cavanaugh
 * @version $Id: Db4oEngineDAO.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class Db4oEngineDAO extends AbstractDb4oDAO implements EngineDAO {

    private AccountDAO accountDAO;

    private BudgetDAO budgetDAO;

    private CommodityDAO commodityDAO;

    private ConfigDAO configDAO;

    private RecurringDAO recurringDAO;

    private TransactionDAO transactionDAO;

    private TrashDAO trashDAO;

    private ScheduledExecutorService commitExecutor;

    Db4oEngineDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);

        // scheduled thread to check and verify a commit occurs every 30 seconds at the minimum if needed.
        commitExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultDaemonThreadFactory());

        // run commit every 30 seconds, 30 seconds after startup
        commitExecutor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                if (commitCount.get() > 0) {
                    commitAndReset();
                }
            }
        }, MAX_COMMIT_TIME, MAX_COMMIT_TIME, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void shutdown() {
        commitExecutor.shutdown();
        commitExecutor = null;
    }

    @Override
    public synchronized AccountDAO getAccountDAO() {
        if (accountDAO == null) {
            accountDAO = new Db4oAccountDAO(container, isRemote);
        }
        return accountDAO;
    }

    @Override
    public BudgetDAO getBudgetDAO() {
        if (budgetDAO == null) {
            budgetDAO = new Db4oBudgetDAO(container, isRemote);
        }
        return budgetDAO;
    }

    @Override
    public synchronized CommodityDAO getCommodityDAO() {
        if (commodityDAO == null) {
            commodityDAO = new Db4oCommodityDAO(container, isRemote);
        }
        return commodityDAO;
    }

    @Override
    public synchronized ConfigDAO getConfigDAO() {
        if (configDAO == null) {
            configDAO = new Db4oConfigDAO(container, isRemote);
        }
        return configDAO;
    }

    @Override
    public synchronized RecurringDAO getRecurringDAO() {
        if (recurringDAO == null) {
            recurringDAO = new Db4oRecurringDAO(container, isRemote);
        }
        return recurringDAO;
    }

    @Override
    public synchronized TransactionDAO getTransactionDAO() {
        if (transactionDAO == null) {
            transactionDAO = new Db4oTransactionDAO(container, isRemote);
        }
        return transactionDAO;
    }

    @Override
    public synchronized TrashDAO getTrashDAO() {
        if (trashDAO == null) {
            trashDAO = new Db4oTrashDAO(container, isRemote);
        }
        return trashDAO;
    }

    @Override
    public StoredObject getObjectByUuid(String uuid) {

        StoredObject o = null;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            Query query = container.query();
            query.constrain(StoredObject.class);
            query.descend("uuid").constrain(uuid);
            ObjectSet<?> result = query.execute();

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            assert result.size() <= 1;

            if (result.size() == 1) {
                o = (StoredObject) result.get(0);
            }
        }

        return o;
    }

    @Override
    public List<StoredObject> getStoredObjects() {
        ArrayList<StoredObject> list = new ArrayList<StoredObject>();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            ObjectSet<StoredObject> set = container.query(StoredObject.class);

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            list.addAll(set);
        }
        return list;
    }
}
