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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;
import jgnash.engine.StoredObject;
import jgnash.engine.dao.AccountDAO;

/**
 * XML Account DAO
 *
 * @author Craig Cavanaugh
 * @version $Id: XMLAccountDAO.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class XMLAccountDAO extends AbstractXMLDAO implements AccountDAO {

    private static final Logger logger = Logger.getLogger(XMLAccountDAO.class.getName());

    XMLAccountDAO(XMLContainer container) {
        super(container);
    }

    @Override
    public RootAccount getRootAccount() {
        RootAccount root = null;

        List<RootAccount> list = container.query(RootAccount.class);

        if (list.size() == 1) {
            root = list.get(0);
        }

        if (list.size() > 1) {
            logger.severe("More than one RootAccount found");
            root = list.get(0);
        }

        return root;
    }

    @Override
    public List<Account> getAccountList() {
        return stripMarkedForRemoval(container.query(Account.class));
    }

    @Override
    public boolean addAccount(Account parent, Account child) {
        container.set(child);
        commit();

        return true;
    }

    @Override
    public boolean addRootAccount(RootAccount account) {
        container.set(account);
        commit();

        return true;
    }

    @Override
    public boolean addAccountSecurity(Account account, SecurityNode node) {
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
    public Account getAccountByUuid(String uuid) {
        Account account = null;

        StoredObject o = container.get(uuid);

        if (o != null && o instanceof Account) {
            account = (Account) o;
        }

        return account;
    }

    @Override
    public boolean updateAccount(Account account) {
        commit();
        return true;
    }

    @Override
    public boolean setAccountProperty(Account account, Object object) {
        commit();
        return true;
    }

    @Override
    public boolean removeAccountProperty(Account account, Object object) {
        commit();
        return true;
    }

    @Override
    public boolean toggleAccountVisibility(Account account) {
        commit();
        return true;
    }

    @Override
    public void refreshAccount(Account account) {
        // do nothing for XML DAO
    }

    private List<Account> getAccountByType(AccountType type) {
        List<Account> list = new ArrayList<>();

        for (Account a : getAccountList()) {
            if (a.getAccountType() == type) {
                list.add(a);
            }
        }

        return list;
    }
}
