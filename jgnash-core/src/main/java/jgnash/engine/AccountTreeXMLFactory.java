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
package jgnash.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Import and export a tree of accounts
 * 
 * @author Craig Cavanaugh
 */
public class AccountTreeXMLFactory {
    private static final String ENCODING = "UTF-8";

    private static XStream getStream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider(), new StaxDriver());
        xstream.setMode(XStream.ID_REFERENCES);

        xstream.alias("Account", Account.class);
        xstream.alias("RootAccount", RootAccount.class);
        xstream.alias("CurrencyNode", CurrencyNode.class);
        xstream.alias("SecurityNode", SecurityNode.class);

        xstream.useAttributeFor(Account.class, "placeHolder");
        xstream.useAttributeFor(Account.class, "locked");
        xstream.useAttributeFor(Account.class, "visible");
        xstream.useAttributeFor(Account.class, "name");
        xstream.useAttributeFor(Account.class, "description");

        xstream.useAttributeFor(CommodityNode.class, "symbol");
        xstream.useAttributeFor(CommodityNode.class, "scale");
        xstream.useAttributeFor(CommodityNode.class, "prefix");
        xstream.useAttributeFor(CommodityNode.class, "suffix");
        xstream.useAttributeFor(CommodityNode.class, "description");

        xstream.omitField(StoredObject.class, "uuid");
        xstream.omitField(StoredObject.class, "markedForRemoval");

        xstream.omitField(Account.class, "transactions");
        xstream.omitField(Account.class, "accountBalance");
        xstream.omitField(Account.class, "reconciledBalance");

        xstream.omitField(SecurityNode.class, "historyNodes");

        return xstream;
    }

    public static void exportAccountTree(final Engine engine, final File file) {
        RootAccount account = engine.getRootAccount();

        XStream xstream = getStream();

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), ENCODING);
                ObjectOutputStream out = xstream.createObjectOutputStream(new PrettyPrintWriter(writer))) {
            out.writeObject(account);
        } catch (IOException e) {
            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Load an account tree given a reader
     * 
     * @param reader
     *            Reader to use
     * @return RootAccount if reader is valid
     */
    private static RootAccount loadAccountTree(final Reader reader) {
        RootAccount account = null;

        XStream xstream = getStream();

        try (ObjectInputStream in = xstream.createObjectInputStream(reader)) {
            Object o = in.readObject();

            if (o instanceof RootAccount) {
                account = (RootAccount) o;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        return account;
    }

    /**
     * Load an account tree given a reader
     * 
     * @param file
     *            file name to use
     * @return RootAccount if file name is valid
     */
    public static RootAccount loadAccountTree(final File file) {
        RootAccount account = null;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), ENCODING)) {
            account = loadAccountTree(reader);
        } catch (IOException ex) {
            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        return account;
    }

    /**
     * Load an account tree given an InputStream
     * 
     * @param stream
     *            InputStream to use
     * @return RootAccount if stream is valid
     */
    public static RootAccount loadAccountTree(final InputStream stream) {
        try (Reader reader = new InputStreamReader(stream, ENCODING)) {
            return loadAccountTree(reader);
        } catch (IOException ex) {
            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Imports an account tree into the existing account tree. Account
     * currencies are forced to the engine's default
     * 
     * @param engine
     *            current engine to merge into
     * @param root
     *            root of account structure to merge
     */
    public static void importAccountTree(final Engine engine, final RootAccount root) {
        AccountImport accountImport = new AccountImport();
        accountImport.importAccountTree(engine, root);
    }

    /**
     * Merges an account tree into the existing account tree. Duplicate
     * currencies are prevented
     * 
     * @param engine
     *            current engine to merge into
     * @param root
     *            root of account structure to merge
     */
    public static void mergeAccountTree(final Engine engine, final RootAccount root) {
        AccountImport accountImport = new AccountImport();
        accountImport.mergeAccountTree(engine, root);
    }

    static private class AccountImport {

        // merge map for accounts
        private final Map<Account, Account> mergeMap = new HashMap<>();

        private void importAccountTree(final Engine engine, final RootAccount root) {
            forceCurrency(engine, root);

            for (Account child : root.getChildren()) {
                importChildren(engine, child);
            }
        }

        private void mergeAccountTree(final Engine engine, final RootAccount root) {
            fixCurrencies(engine, root);

            for (Account child : root.getChildren()) {
                importChildren(engine, child);
            }
        }

        /**
         * Ensures that duplicate currencies are not created when the accounts
         * are merged
         * 
         * @param engine
         *            Engine with existing currencies
         * @param account
         *            account to correct
         */
        private void fixCurrencies(final Engine engine, final Account account) {

            for (CurrencyNode currencyNode : engine.getCurrencies()) {
                if (account.getCurrencyNode().matches(currencyNode)) {
                    account.setCurrencyNode(currencyNode);
                }
            }

            // match SecurityNodes to prevent duplicates
            if (account.memberOf(AccountGroup.INVEST)) {
                Set<SecurityNode> nodes = account.getSecurities();

                for (SecurityNode node : nodes) {
                    SecurityNode sNode = engine.getSecurity(node.getSymbol());

                    if (sNode == null) { // no match found
                        try {
                            sNode = (SecurityNode) node.clone();

                            for (CurrencyNode currencyNode : engine.getCurrencies()) {
                                if (sNode.getReportedCurrencyNode().matches(currencyNode)) {
                                    sNode.setReportedCurrencyNode(currencyNode);
                                }
                            }
                            engine.addCommodity(sNode);
                        } catch (CloneNotSupportedException e) {
                            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE,
                                    e.getLocalizedMessage(), e);
                        }
                    }

                    account.removeSecurity(node);
                    account.addSecurity(sNode);
                }
            }

            for (Account child : account.getChildren()) {
                fixCurrencies(engine, child);
            }
        }

        /**
         * Ensures that duplicate currencies are not created when the accounts
         * are merged
         * 
         * @param engine
         *            Engine with existing currencies
         * @param account
         *            account to correct
         */
        private void forceCurrency(final Engine engine, final Account account) {

            account.setCurrencyNode(engine.getDefaultCurrency());

            // match SecurityNodes to prevent duplicates
            if (account.memberOf(AccountGroup.INVEST)) {
                Set<SecurityNode> nodes = account.getSecurities();

                for (SecurityNode node : nodes) {
                    SecurityNode sNode = engine.getSecurity(node.getSymbol());

                    if (sNode == null) { // no match found
                        try {
                            sNode = (SecurityNode) node.clone();

                            sNode.setReportedCurrencyNode(engine.getDefaultCurrency());
                            engine.addCommodity(sNode);
                        } catch (CloneNotSupportedException e) {
                            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, e.toString(), e);
                        }
                    }

                    account.removeSecurity(node);
                    account.addSecurity(sNode);
                }
            }

            for (Account child : account.getChildren()) {
                forceCurrency(engine, child);
            }
        }

        private void importChildren(final Engine engine, final Account account) {

            // match RootAccount special case
            if (account.getParent() instanceof RootAccount) {
                mergeMap.put(account.getParent(), engine.getRootAccount());
            }

            // search for a pre-existing match
            Account match = AccountUtils.searchTree(engine.getRootAccount(), account.getName(),
                    account.getAccountType(), account.getDepth());

            if (match != null && match.getParent().equals(mergeMap.get(account.getParent()))) { // found a match
                mergeMap.put(account, match);
            } else { // the account is unique

                // place in the merge map
                mergeMap.put(account, account);

                Account parent = mergeMap.get(account.getParent());
                engine.addAccount(parent, account);
            }

            for (Account child : account.getChildren()) {
                importChildren(engine, child);
            }
        }
    }

    private AccountTreeXMLFactory() {
    }
}
