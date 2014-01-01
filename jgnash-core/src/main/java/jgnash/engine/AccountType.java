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
package jgnash.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.util.Resource;

/**
 * Account type enumeration
 * 
 * @author Craig Cavanaugh
 *
 */
public enum AccountType {

    ASSET(Resource.get().getString("AccountType.Asset"), AccountGroup.ASSET, AccountProxy.class, true),
    BANK(Resource.get().getString("AccountType.Bank"), AccountGroup.ASSET, AccountProxy.class, true),
    CASH(Resource.get().getString("AccountType.Cash"), AccountGroup.ASSET, AccountProxy.class, true),
    CHECKING(Resource.get().getString("AccountType.Checking"), AccountGroup.ASSET, AccountProxy.class, true),
    CREDIT(Resource.get().getString("AccountType.Credit"), AccountGroup.LIABILITY, AccountProxy.class, true),
    EQUITY(Resource.get().getString("AccountType.Equity"), AccountGroup.EQUITY, AccountProxy.class, true),
    EXPENSE(Resource.get().getString("AccountType.Expense"), AccountGroup.EXPENSE, AccountProxy.class, true),
    INCOME(Resource.get().getString("AccountType.Income"), AccountGroup.INCOME, AccountProxy.class, true),
    INVEST(Resource.get().getString("AccountType.Investment"), AccountGroup.INVEST, InvestmentAccountProxy.class, false),
    SIMPLEINVEST(Resource.get().getString("AccountType.SimpleInvestment"), AccountGroup.INVEST, AccountProxy.class, true),
    LIABILITY(Resource.get().getString("AccountType.Liability"), AccountGroup.LIABILITY, AccountProxy.class, true),
    MONEYMKRT(Resource.get().getString("AccountType.MoneyMkrt"), AccountGroup.ASSET, AccountProxy.class, true),
    MUTUAL(Resource.get().getString("AccountType.Mutual"), AccountGroup.INVEST, InvestmentAccountProxy.class, false),
    ROOT(Resource.get().getString("AccountType.Root"), AccountGroup.ROOT, AccountProxy.class, true);

    private final transient String description;

    private final transient AccountGroup accountGroup;

    private final transient Class<? extends AccountProxy> accountProxy;

    private final transient boolean mutable;

    private AccountType(final String description, final AccountGroup accountGroup, final Class<? extends AccountProxy> accountProxy, final boolean mutable) {
        this.description = description;
        this.accountGroup = accountGroup;
        this.accountProxy = accountProxy;
        this.mutable = mutable;
    }

    /**
     * Returns all AccountTypes that fit the supplied AccountGroup
     * 
     * @param group AccountGroup to match
     * @return array of AccountTypes that fit the supplied group
     */
    public static Set<AccountType> getAccountTypes(final AccountGroup group) {
        final Set<AccountType> list = getAccountTypeSet();

        for (Iterator<AccountType> i = list.iterator(); i.hasNext();) {
            if (i.next().getAccountGroup() != group) {
                i.remove();
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return description;
    }

    public AccountGroup getAccountGroup() {
        return accountGroup;
    }

    public boolean isMutable() {
        return mutable;
    }

    private static Set<AccountType> getAccountTypeSet() {
        Set<AccountType> set = EnumSet.allOf(AccountType.class);

        set.remove(AccountType.ROOT);
        return set;
    }

    AccountProxy getProxy(final Account account) {
        try {
            Class<?>[] constParams = new Class<?>[] { Account.class };
            Constructor<?> accConst = accountProxy.getDeclaredConstructor(constParams);
            Object[] params = new Object[] { account };
            return (AccountProxy) accConst.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(AccountType.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null; // unable to create object

    }
}
