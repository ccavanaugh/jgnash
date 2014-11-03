/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
import java.util.Objects;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.ui.StaticUIMethods;
import jgnash.util.Resource;

/**
 * Static account creation and modification methods
 * 
 * @author Craig Cavanaugh
 */
class AccountTools {

    private AccountTools() {
    }

    static void createAccount(final Account account) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        Account parentAccount = account;

        final Resource rb = Resource.get();

        if (parentAccount == null) {            
            parentAccount = engine.getRootAccount();
            
            if (parentAccount == null) {
                return; // no root account at all, file was closed
            }           
        }

        AccountDialog dlg = new AccountDialog();
        dlg.setParentAccount(parentAccount);
        dlg.setAccountType(parentAccount.getAccountType());
        dlg.setTitle(rb.getString("Title.NewAccount"));
        dlg.setVisible(true);

        if (dlg.returnStatus()) {          
            CurrencyNode currency = dlg.getCurrency();
            AccountType accType = dlg.getAccountType();
            
            if (currency == null) {
                currency = engine.getRootAccount().getCurrencyNode();
                Logger.getLogger(AccountTools.class.getName()).warning("Forcing use of the default currency");
            }

            Account newAccount = new Account(accType, currency);

            if (accType.getAccountGroup() == AccountGroup.INVEST) {
                Collection<SecurityNode> collection = dlg.getAccountSecurities();

                for (SecurityNode node : collection) {
                    newAccount.addSecurity(node);
                }
            }

            newAccount.setName(dlg.getAccountName());
            newAccount.setAccountNumber(dlg.getAccountCode());
            newAccount.setBankId(dlg.getBankId());
            newAccount.setDescription(dlg.getAccountDescription());
            newAccount.setNotes(dlg.getAccountNotes());
            newAccount.setLocked(dlg.isAccountLocked());
            newAccount.setPlaceHolder(dlg.isAccountPlaceholder());
            newAccount.setVisible(dlg.isAccountVisible());
            newAccount.setExcludedFromBudget(dlg.isExcludedFromBudget());

            engine.addAccount(dlg.getParentAccount(), newAccount);
        }
    }

    static void modifyAccount(final Account account) {

        if (account == null) {
            return;
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Resource rb = Resource.get();

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
        dlg.setBankId(account.getBankId());
        dlg.setCurrency(account.getCurrencyNode());
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

            Account tAccount = new Account(dlg.getAccountType(), dlg.getCurrency());
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
               
                Logger.getLogger(AccountTools.class.getName()).warning("Prevented an attempt to assign accounts parent to itself");
            } else {
                tAccount.setParent(dlg.getParentAccount());
            }

            tAccount.setVisible(dlg.isAccountVisible());
            tAccount.setExcludedFromBudget(dlg.isExcludedFromBudget());

            if (!engine.modifyAccount(tAccount, account)) {
                StaticUIMethods.displayError(rb.getString("Message.Error.AccountUpdate"));
            }

            if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                if (!engine.updateAccountSecurities(account, dlg.getAccountSecurities())) {
                    StaticUIMethods.displayError(rb.getString("Message.Error.SecurityAccountUpdate"));
                }
            }
        }
    }
}
