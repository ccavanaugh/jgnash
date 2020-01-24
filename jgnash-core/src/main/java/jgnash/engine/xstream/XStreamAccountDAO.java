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
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;
import jgnash.engine.dao.AccountDAO;

/**
 * XML Account DAO.
 *
 * @author Craig Cavanaugh
 */
class XStreamAccountDAO extends AbstractXStreamDAO implements AccountDAO {

    private static final Logger logger = Logger.getLogger(XStreamAccountDAO.class.getName());

    XStreamAccountDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    @Override
    public RootAccount getRootAccount() {
        RootAccount root = null;

        List<RootAccount> list = container.query(RootAccount.class);

        if (list.size() == 1) {
            root = list.get(0);
        }

        if (list.size() > 1) {  // old bug
            logger.severe("More than one RootAccount found");

            for (final RootAccount rootAccount : list) {
                if (rootAccount.getChildCount() > 0) {
                    root = rootAccount;
                }
            }
        }

        return root;
    }

    @Override
    public List<Account> getAccountList() {
        return stripMarkedForRemoval(container.query(Account.class));
    }

    @Override
    public boolean addAccount(final Account parent, final Account child) {
        container.set(child);
        commit();

        return true;
    }

    @Override
    public boolean addRootAccount(final RootAccount account) {
        container.set(account);
        commit();

        return true;
    }

    @Override
    public boolean addAccountSecurity(final Account account, final SecurityNode node) {
        container.set(node);
        commit();

        return true;
    }

    @Override
    public List<Account> getIncomeAccountList() {
        return getAccountByType(AccountType.INCOME);
    }

    @Override
    public List<Account> getExpenseAccountList() {
        return getAccountByType(AccountType.EXPENSE);
    }

    @Override
    public List<Account> getInvestmentAccountList() {
        return getAccountByType(AccountType.INVEST);
    }

    @Override
    public Account getAccountByUuid(final UUID uuid) {
        return getObjectByUuid(Account.class, uuid);
    }

    @Override
    public boolean updateAccount(final Account account) {
        commit();
        return true;
    }

    @Override
    public boolean toggleAccountVisibility(final Account account) {
        commit();
        return true;
    }

    private List<Account> getAccountByType(final AccountType type) {
        return getAccountList().parallelStream().filter(a -> a.getAccountType() == type).collect(Collectors.toList());
    }
}
