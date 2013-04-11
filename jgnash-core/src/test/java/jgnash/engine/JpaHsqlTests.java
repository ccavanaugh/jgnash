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


import jgnash.engine.jpa.Database;
import jgnash.engine.jpa.JpaConfiguration;
import jgnash.engine.jpa.JpaHsqlDataStore;

import jgnash.util.FileUtils;
import org.hsqldb.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class JpaHsqlTests {

    Server hsqlServer;
    String fileName;

    final int port = 9001;

    @Before
    public void before() throws InterruptedException, ClassNotFoundException, SQLException {
        try {
            File temp = File.createTempFile("jpatest-", "");
            fileName = temp.getAbsolutePath();

            StringBuilder urlBuilder = new StringBuilder("file:");
            urlBuilder.append(FileUtils.stripFileExtension(fileName));

            if (!temp.delete()) {
                fail("Could not delete the temp file");
            }

            hsqlServer = new Server();
            hsqlServer.setDatabaseName(0, "jgnash");    // the alias
            hsqlServer.setPort(port);
            hsqlServer.setDatabasePath(0, urlBuilder.toString());

            hsqlServer.start();

            Class.forName("org.hsqldb.jdbcDriver");
            Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/jgnash", "sa", ""); // can through sql exception
            connection.prepareStatement("CREATE USER JGNASH PASSWORD \"\" ADMIN").execute();
            connection.commit();
            connection.close();

            connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/jgnash", "JGNASH", ""); // can through sql exception
            connection.prepareStatement("DROP USER SA").execute();
            connection.commit();
            connection.close();
        } catch (IOException e) {
            Logger.getLogger(JpaHsqlTests.class.getName()).log(Level.INFO, e.getMessage(), e);
        }
    }

    @After
    public void after() throws IOException {
        hsqlServer.stop();
        JpaHsqlDataStore.deleteDatabase(fileName);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void simpleAccountTest() {

        Properties properties = JpaConfiguration.getClientProperties(Database.HSQLDB, "", "localhost", port, new char[]{});

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("jgnash", properties);

        EntityManager em = factory.createEntityManager();

        // Begin a new local transaction so that we can persist a new entity
        em.getTransaction().begin();

        CurrencyNode node = DefaultCurrencies.buildNode(Locale.US);
        em.persist(node);

        Account account = new Account(AccountType.BANK, node);
        account.setName("Test");

        String notes = "This is a character blob in the database";

        for (int i = 0; i < 8192 - notes.length(); i++) {
            notes = notes + "z";
        }

        account.setNotes(notes);

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


        em.getTransaction().begin();
        Account removable = new Account(AccountType.BANK, node);
        account.setName("Test2");
        account.setMarkedForRemoval(true);
        em.persist(removable);
        em.getTransaction().commit();

        Query rq = em.createQuery("SELECT a FROM Account a WHERE a.markedForRemoval = true");
        removable = (Account) rq.getSingleResult();

        assertEquals("Test2", removable.getName());

        em.close();
        factory.close();

        factory = Persistence.createEntityManagerFactory("jgnash", properties);
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

    @Test
    @SuppressWarnings("unchecked")
    public void transactionTest() {
        final String ACC_NAME = "Test Tran";

        Properties properties = JpaConfiguration.getClientProperties(Database.HSQLDB, "", "localhost", port, new char[]{});

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("jgnash", properties);

        EntityManager em = factory.createEntityManager();

        // Begin a new local transaction so that we can persist a new entity
        em.getTransaction().begin();

        CurrencyNode node = DefaultCurrencies.buildNode(Locale.US);
        em.persist(node);

        Account account = new Account(AccountType.BANK, node);
        account.setName(ACC_NAME);
        em.persist(account);

        em.getTransaction().commit();

        Transaction t = TransactionFactory.generateSingleEntryTransaction(account, BigDecimal.TEN, new Date(), true, "memo", "payee", "1");

        em.getTransaction().begin();
        account.addTransaction(t);
        em.persist(account);

        em.getTransaction().commit();

        em.close();
        factory.close();

        factory = Persistence.createEntityManagerFactory("jgnash", properties);
        EntityManager em2 = factory.createEntityManager();

        Query q = em2.createQuery("select a from Account a");

        for (Account a : (List<Account>) q.getResultList()) {
            if (a.getName().equals(ACC_NAME)) {
                System.out.println(a.getName());

                List<Transaction> transactions = a.getReadOnlySortedTransactionList();

                if (!transactions.isEmpty()) {
                    for (Transaction tran : transactions) {
                        System.out.println("    " + tran.getMemo() + ", " + tran.getPayee());
                    }
                }

                assertTrue(transactions.size() == 1);
            }
        }

        em2.close();
        factory.close();
    }

    @Test
    public void securityNodeTest() {
        Properties properties = JpaConfiguration.getClientProperties(Database.HSQLDB, "", "localhost", port, new char[]{});

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("jgnash", properties);

        EntityManager em = factory.createEntityManager();

        // Begin a new local transaction so that we can persist a new entity
        em.getTransaction().begin();

        CurrencyNode node = DefaultCurrencies.buildNode(Locale.US);
        em.persist(node);

        Account account = new Account(AccountType.INVEST, node);
        account.setName("Invest");


        SecurityNode sNode = new SecurityNode(node);
        sNode.setSymbol("MSFT");

        account.addSecurity(sNode);

        em.persist(account);
        em.persist(sNode);

        em.getTransaction().commit();
    }
}
