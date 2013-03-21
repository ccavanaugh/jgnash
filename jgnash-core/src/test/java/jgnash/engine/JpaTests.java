/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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


import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertNotNull;

public class JpaTests {

    @Test
    public void SimpleAccountTest() {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("jgnash", System.getProperties());

        EntityManager em = factory.createEntityManager();

        // Begin a new local transaction so that we can persist a new entity
        em.getTransaction().begin();

        CurrencyNode node = DefaultCurrencies.buildNode(Locale.US);
        em.persist(node);

        Account account = new Account(AccountType.BANK, node);
        account.setName("Test");
        em.persist(account);

        em.getTransaction().commit();

        em.getTransaction().begin();
        {
            Account child = new Account(AccountType.BANK, node);
            child.setName("Child");
            account.addChild(child);
            em.persist(child);
            em.persist(account);
        }

        em.getTransaction().commit();


        em.close();
        factory.close();

        factory = Persistence.createEntityManagerFactory("jgnash", System.getProperties());
        EntityManager em2 = factory.createEntityManager();

        Query q = em2.createQuery("select a from Account a");

        for (Account a : (List<Account>) q.getResultList()) {
            System.out.println(a.getName() + " (with currency: " + a.getCurrencyNode().getSymbol() + ")");

            if (a.isParent()) {
                System.out.println("    " + a.getName() + " has child: " + a.getChildren().get(0).getName());
            }

            assertNotNull(a.getTransactionLock());

            a.getTransactionLock().readLock().lock();
            a.getTransactionLock().readLock().unlock();
        }

        em2.close();
        factory.close();
    }
}
