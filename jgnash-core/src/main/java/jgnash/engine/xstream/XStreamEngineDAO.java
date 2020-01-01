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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AccountDAO;
import jgnash.engine.dao.BudgetDAO;
import jgnash.engine.dao.CommodityDAO;
import jgnash.engine.dao.ConfigDAO;
import jgnash.engine.dao.EngineDAO;
import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.dao.TagDAO;
import jgnash.engine.dao.TransactionDAO;
import jgnash.engine.dao.TrashDAO;

/**
 * XML Engine DAO Interface.
 * 
 * @author Craig Cavanaugh
 */
class XStreamEngineDAO extends AbstractXStreamDAO implements EngineDAO {

    private AccountDAO accountDAO;

    private BudgetDAO budgetDAO;

    private CommodityDAO commodityDAO;

    private ConfigDAO configDAO;

    private RecurringDAO recurringDAO;

    private TagDAO tagDAO;

    private TransactionDAO transactionDAO;

    private TrashDAO trashDAO;

    private ScheduledExecutorService commitExecutor;

    private ScheduledFuture<?> future;

    private final Timer commitTimer;

    XStreamEngineDAO(final AbstractXStreamContainer container) {
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

                future = commitExecutor.schedule(() -> {
                    if (commitCount.get() > 0) {
                        Logger.getLogger(XStreamEngineDAO.class.getName()).info("Committing file");
                        commitAndReset();
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
            accountDAO = new XStreamAccountDAO(container);
        }
        return accountDAO;
    }

    @Override
    public BudgetDAO getBudgetDAO() {
        if (budgetDAO == null) {
            budgetDAO = new XStreamBudgetDAO(container);
        }
        return budgetDAO;
    }

    @Override
    public synchronized CommodityDAO getCommodityDAO() {
        if (commodityDAO == null) {
            commodityDAO = new XStreamCommodityDAO(container);
        }
        return commodityDAO;
    }

    @Override
    public synchronized ConfigDAO getConfigDAO() {
        if (configDAO == null) {
            configDAO = new XStreamConfigDAO(container);
        }
        return configDAO;
    }

    @Override
    public synchronized RecurringDAO getRecurringDAO() {
        if (recurringDAO == null) {
            recurringDAO = new XStreamRecurringDAO(container);
        }
        return recurringDAO;
    }

    @Override
    public synchronized TransactionDAO getTransactionDAO() {
        if (transactionDAO == null) {
            transactionDAO = new XStreamTransactionDAO(container);
        }
        return transactionDAO;
    }

    @Override
    public TagDAO getTagDAO() {
        if (tagDAO == null) {
            tagDAO = new XStreamTagDAO(container);
        }
        return tagDAO;
    }

    @Override
    public synchronized TrashDAO getTrashDAO() {
        if (trashDAO == null) {
            trashDAO = new XStreamTrashDAO(container);
        }
        return trashDAO;
    }

    @Override
    public List<StoredObject> getStoredObjects() {
        return container.asList();
    }

    @Override
    public <T extends StoredObject> List<T> getStoredObjects(Class<T> tClass) {
        return stripMarkedForRemoval(container.query(tClass));
    }

    @Override
    public void refresh(final StoredObject object) {
        // do nothing for XStream
    }

    @Override
    public void bulkUpdate(List<? extends StoredObject> objectList) {
        commit();
    }
}
