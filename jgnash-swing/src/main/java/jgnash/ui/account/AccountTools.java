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
package jgnash.ui.account;

import java.util.Collection;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.util.Resource;

/**
 * Static account creation and modification methods
 * 
 * @author Craig Cavanaugh
 * @version $Id: AccountTools.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class AccountTools {

    private AccountTools() {
    }

    static void createAccount(final Account _account) {
        Account parentAccount = _account;

        Resource rb = Resource.get();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (parentAccount == null) {
            if (engine.getRootAccount() == null) {
                return; // no root account at all, file was closed
            }
            parentAccount = engine.getRootAccount();
        }

        AccountDialog dlg = new AccountDialog();
        dlg.setParentAccount(parentAccount);
        dlg.setAccountType(parentAccount.getAccountType());
        dlg.setTitle(rb.getString("Title.NewAccount"));
        dlg.setVisible(true);

        if (dlg.returnStatus()) {
            Account account;
            CurrencyNode commodity = dlg.getAccountCommodity();
            AccountType accType = dlg.getAccountType();

            account = new Account(accType, commodity);

            if (accType.getAccountGroup() == AccountGroup.INVEST) {
                Collection<SecurityNode> collection = dlg.getAccountSecurities();

                for (SecurityNode node : collection) {
                    account.addSecurity(node);
                }
            }

            account.setName(dlg.getAccountName());
            account.setAccountNumber(dlg.getAccountCode());
            account.setBankId(dlg.getBankId());
            account.setDescription(dlg.getAccountDescription());
            account.setNotes(dlg.getAccountNotes());
            account.setLocked(dlg.isAccountLocked());
            account.setPlaceHolder(dlg.isAccountPlaceholder());
            account.setVisible(dlg.isAccountVisible());
            account.setExcludedFromBudget(dlg.isExcludedFromBudget());

            engine.addAccount(dlg.getParentAccount(), account);
        }
    }

    static void modifyAccount(final Account account) {

        if (account == null) {
            return;
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Resource rb = Resource.get();

        Account parentAccount = account.getParent();

        if (parentAccount == null) {
            if (engine.getRootAccount() == null) {
                return; // no root account at all, file was closed
            }
            account.setParent(engine.getRootAccount());
        }

        AccountDialog dlg = new AccountDialog();
        dlg.setTitle(rb.getString("Title.ModifyAccount"));

        dlg.setParentAccount(account.getParent());
        dlg.setAccountName(account.getName());
        dlg.setAccountDescription(account.getDescription());
        dlg.setAccountCode(account.getAccountNumber());
        dlg.setBankID(account.getBankId());
        dlg.setAccountCommodity(account.getCurrencyNode());
        dlg.setAccountNotes(account.getNotes());
        dlg.setAccountLocked(account.isLocked());
        dlg.setAccountVisible(account.isVisible());
        dlg.setExcludedFromBudget(account.isExcludedFromBudget());

        if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
            dlg.setAccountSecurities(account.getSecurities());
        }
        dlg.setAccountType(account.getAccountType());
        dlg.disableAccountType(account.getAccountType());

        if (account.getTransactionCount() > 0) {
            dlg.disableAccountCurrency();
        }

        if (account.getTransactionCount() > 0) {
            dlg.setPlaceholderEnabled(false);
        } else {
            dlg.setAccountPlaceholder(account.isPlaceHolder());
        }

        dlg.setVisible(true);

        if (dlg.returnStatus()) {

            Account tAccount = new Account(dlg.getAccountType(), dlg.getAccountCommodity());
            // set the data
            tAccount.setAccountNumber(dlg.getAccountCode());
            tAccount.setBankId(dlg.getBankId());
            tAccount.setName(dlg.getAccountName());
            tAccount.setDescription(dlg.getAccountDescription());
            tAccount.setNotes(dlg.getAccountNotes());
            tAccount.setLocked(dlg.isAccountLocked());
            tAccount.setPlaceHolder(dlg.isAccountPlaceholder());

            if (dlg.getParentAccount() == account) {
                tAccount.setParent(account.getParent());
                System.out.println("Prevented an attempt to assign accounts parent to itself");
            } else {
                tAccount.setParent(dlg.getParentAccount());
            }

            tAccount.setVisible(dlg.isAccountVisible());
            tAccount.setExcludedFromBudget(dlg.isExcludedFromBudget());

            engine.modifyAccount(tAccount, account);

            if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                engine.updateAccountSecurities(account, dlg.getAccountSecurities());
            }
        }
    }
}
