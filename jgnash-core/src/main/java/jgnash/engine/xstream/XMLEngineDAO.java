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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.*;

/**
 * XML Engine DAO Interface
 * 
 * @author Craig Cavanaugh
 *
 */
public class XMLEngineDAO extends AbstractXMLDAO implements EngineDAO {

    private AccountDAO accountDAO;

    private BudgetDAO budgetDAO;

    private CommodityDAO commodityDAO;

    private ConfigDAO configDAO;

    private RecurringDAO recurringDAO;

    private TransactionDAO transactionDAO;

    private TrashDAO trashDAO;

    private ScheduledExecutorService commitExecutor;

    ScheduledFuture<?> future;

    private final Timer commitTimer;

    protected XMLEngineDAO(final XMLContainer container) {
        super(container);

        commitTimer = new Timer();

        // scheduled thread to check and verify a commit occurs every 30 seconds at the minimum if needed.
        commitExecutor = Executors.newSingleThreadScheduledExecutor();

        // run commit every 30 seconds, 30 seconds after startup
        commitTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                if (future != null && !future.isDone() || commitExecutor == null) {
                    return;
                }

                future = commitExecutor.schedule(new Runnable() {

                    @Override
                    public void run() {
                        if (commitCount.get() > 0) {
                            Logger.getLogger(XMLEngineDAO.class.getName()).info("Commiting file");
                            commitAndReset();
                        }
                    }

                }, 0, TimeUnit.SECONDS);
            }
        }, MAX_COMMIT_TIME * 1000, MAX_COMMIT_TIME * 1000);
    }

    @Override
    public synchronized void shutdown() {
        commitTimer.cancel();
        commitExecutor.shutdown();
        commitExecutor = null;
    }

    @Override
    public synchronized AccountDAO getAccountDAO() {
        if (accountDAO == null) {
            accountDAO = new XMLAccountDAO(container);
        }
        return accountDAO;
    }

    @Override
    public BudgetDAO getBudgetDAO() {
        if (budgetDAO == null) {
            budgetDAO = new XMLBudgetDAO(container);
        }
        return budgetDAO;
    }

    @Override
    public synchronized CommodityDAO getCommodityDAO() {
        if (commodityDAO == null) {
            commodityDAO = new XMLCommodityDAO(container);
        }
        return commodityDAO;
    }

    @Override
    public synchronized ConfigDAO getConfigDAO() {
        if (configDAO == null) {
            configDAO = new XMLConfigDAO(container);
        }
        return configDAO;
    }

    @Override
    public synchronized RecurringDAO getRecurringDAO() {
        if (recurringDAO == null) {
            recurringDAO = new XMLRecurringDAO(container);
        }
        return recurringDAO;
    }

    @Override
    public synchronized TransactionDAO getTransactionDAO() {
        if (transactionDAO == null) {
            transactionDAO = new XMLTransactionDAO(container);
        }
        return transactionDAO;
    }

    @Override
    public synchronized TrashDAO getTrashDAO() {
        if (trashDAO == null) {
            trashDAO = new XMLTrashDAO(container);
        }
        return trashDAO;
    }

    @Override
    public StoredObject getObjectByUuid(String uuid) {
        return container.get(uuid);
    }

    @Override
    public List<StoredObject> getStoredObjects() {
        return container.asList();
    }
}
