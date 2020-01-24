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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jgnash.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests account ancestor depth API.
 *
 * @author Craig Cavanaugh
 */
class AccountTreeDepthTest {

    AccountTreeDepthTest() {
    }

    @Test
    void getDepthTest() {
        CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

        RootAccount root = new RootAccount(defaultCurrency);

        Account a = new Account(AccountType.BANK, defaultCurrency);
        a.setName("a");
        root.addChild(a);

        Account b = new Account(AccountType.BANK, defaultCurrency);
        b.setName("b");
        a.addChild(b);

        Account c = new Account(AccountType.BANK, defaultCurrency);
        c.setName("c");
        b.addChild(c);

        Account d = new Account(AccountType.BANK, defaultCurrency);
        d.setName("d");
        c.addChild(d);

        System.out.println(root.getDepth());
        assertEquals(0, root.getDepth());

        System.out.println(a.getDepth());
        assertEquals(1, a.getDepth());

        System.out.println(b.getDepth());
        assertEquals(2, b.getDepth());

        System.out.println(c.getDepth());
        assertEquals(3, c.getDepth());

        System.out.println(d.getDepth());
        assertEquals(4, d.getDepth());

        assertNotNull(AccountUtils.searchTree(root, "d", AccountType.BANK, 4));
        assertNull(AccountUtils.searchTree(root, "d", AccountType.BANK, 3));
    }

    @Test
    void getLineageTest() {
        CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

        RootAccount root = new RootAccount(defaultCurrency);

        Account a = new Account(AccountType.BANK, defaultCurrency);
        a.setName("a");
        root.addChild(a);

        Account a1 = new Account(AccountType.BANK, defaultCurrency);
        a1.setName("a1");
        root.addChild(a1);

        Account b = new Account(AccountType.BANK, defaultCurrency);
        b.setName("b");
        a.addChild(b);

        Account c = new Account(AccountType.BANK, defaultCurrency);
        c.setName("c");
        b.addChild(c);

        Account d = new Account(AccountType.BANK, defaultCurrency);
        d.setName("d");
        c.addChild(d);

        Account e = new Account(AccountType.BANK, defaultCurrency);
        e.setName("e");
        c.addChild(e);        
    }

    @Test
    void getAncestorsTest() {
        CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

        RootAccount root = new RootAccount(defaultCurrency);

        Account a = new Account(AccountType.BANK, defaultCurrency);
        a.setName("a");
        root.addChild(a);

        Account a1 = new Account(AccountType.BANK, defaultCurrency);
        a1.setName("a1");
        root.addChild(a1);

        Account b = new Account(AccountType.BANK, defaultCurrency);
        b.setName("b");
        a.addChild(b);

        Account c = new Account(AccountType.BANK, defaultCurrency);
        c.setName("c");
        b.addChild(c);

        Account d = new Account(AccountType.BANK, defaultCurrency);
        d.setName("d");
        c.addChild(d);

        Account e = new Account(AccountType.BANK, defaultCurrency);
        e.setName("e");
        c.addChild(e);

        List<Account> ancestors = e.getAncestors();
        assertEquals(5, ancestors.size());
        assertEquals(AccountType.ROOT, ancestors.get(ancestors.size() - 1).getAccountType());
    }

}
