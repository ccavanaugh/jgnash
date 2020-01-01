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
package jgnash.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentCollectionConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentMapConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernateProxyConverter;
import com.thoughtworks.xstream.hibernate.mapper.HibernateMapper;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.ArrayTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.xstream.XStreamJVM9;
import jgnash.resource.util.ClassPathUtils;
import jgnash.resource.util.ResourceUtils;

/**
 * Import and export a tree of accounts using XML files.
 *
 * @author Craig Cavanaugh
 */
public class AccountTreeXMLFactory {
    private static final Charset ENCODING = StandardCharsets.UTF_8;

    private static final String RESOURCE_ROOT_PATH = "/jgnash/resource/account";

    private AccountTreeXMLFactory() {
    }
    
    private static XStream getStream() {

        final XStreamJVM9 xstream = new XStreamJVM9(new PureJavaReflectionProvider(), new StaxDriver()) {

            @Override
            protected MapperWrapper wrapMapper(final MapperWrapper next) {
                return new HibernateMapper(next);
            }
        };

        // configure XStream security
        xstream.addPermission(NoTypePermission.NONE);
        xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
        xstream.addPermission(ArrayTypePermission.ARRAYS);
        xstream.addPermission(new WildcardTypePermission(new String[] {"java.**", "jgnash.engine.**"}));

        xstream.ignoreUnknownElements();    // gracefully ignore fields in the file that do not have object members

        xstream.setMode(XStream.ID_REFERENCES);

        xstream.alias("date", LocalDate.class); // use date instead of local-date by default

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

        // Ignore fields required for JPA
        xstream.omitField(StoredObject.class, "version");

        xstream.omitField(Account.class, "transactions");
        xstream.omitField(Account.class, "accountBalance");
        xstream.omitField(Account.class, "reconciledBalance");
        xstream.omitField(Account.class, "attributes");
        xstream.omitField(Account.class, "propertyMap");
        xstream.omitField(Account.class, "amortizeObject");

        xstream.omitField(SecurityNode.class, "historyNodes");
        xstream.omitField(SecurityNode.class, "securityHistoryEvents");

        // Filters out the hibernate
        xstream.registerConverter(new HibernateProxyConverter());
        xstream.registerConverter(new HibernatePersistentCollectionConverter(xstream.getMapper()));
        xstream.registerConverter(new HibernatePersistentMapConverter(xstream.getMapper()));
        
        
        //xstream.registerConverter(new HibernatePersistentSortedMapConverter(xstream.getMapper()));
        //xstream.registerConverter(new HibernatePersistentSortedSetConverter(xstream.getMapper()));

        return xstream;
    }

    public static void exportAccountTree(final Engine engine, final Path file) {
        RootAccount account = engine.getRootAccount();

        XStream xstream = getStream();

        try (final Writer writer = Files.newBufferedWriter(file, ENCODING);
             final ObjectOutputStream out = xstream.createObjectOutputStream(new PrettyPrintWriter(writer))) {
            out.writeObject(account);
        } catch (IOException e) {
            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Load an account tree given a reader.
     *
     * @param reader Reader to use
     * @return RootAccount if reader is valid
     */
    private static RootAccount loadAccountTree(final Reader reader) {
        RootAccount account = null;

        XStream xstream = getStream();

        try (final ObjectInputStream in = xstream.createObjectInputStream(reader)) {
            final Object o = in.readObject();

            if (o instanceof RootAccount) {
                account = (RootAccount) o;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        return account;
    }

    /**
     * Load an account tree given a reader.
     *
     * @param file file name to use
     * @return RootAccount if file name is valid
     */
    public static RootAccount loadAccountTree(final Path file) {
        RootAccount account = null;

        try (final Reader reader = Files.newBufferedReader(file, ENCODING)) {
            account = loadAccountTree(reader);
        } catch (IOException ex) {
            Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        return account;
    }

    /**
     * Load an account tree given an InputStream.
     *
     * @param stream InputStream to use
     * @return RootAccount if stream is valid
     */
    private static RootAccount loadAccountTree(final InputStream stream) {
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
     * @param engine current engine to merge into
     * @param root   root of account structure to merge
     */
    public static void importAccountTree(final Engine engine, final RootAccount root) {
        AccountImport accountImport = new AccountImport();
        accountImport.importAccountTree(engine, root);
    }

    /**
     * Merges an account tree into the existing account tree. Duplicate
     * currencies are prevented
     *
     * @param engine current engine to merge into
     * @param root   root of account structure to merge
     */
    public static void mergeAccountTree(final Engine engine, final RootAccount root) {
        AccountImport accountImport = new AccountImport();
        accountImport.mergeAccountTree(engine, root);
    }

    public static Collection<RootAccount> getLocalizedAccountSet() {
        final List<RootAccount> files = new ArrayList<>();

        for (final String string : getAccountSetList()) {

            try (final InputStream stream = AccountTreeXMLFactory.class.getResourceAsStream(string)) {
                final RootAccount account = AccountTreeXMLFactory.loadAccountTree(stream);
                files.add(account);
            } catch (final IOException e) {
                Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        return files;
    }

    private static List<String> getAccountSetList() {
        final String path = ClassPathUtils.getLocalizedPath(RESOURCE_ROOT_PATH);

        final List<String> set = new ArrayList<>();

        if (path != null) {
            try (final InputStream stream = AccountTreeXMLFactory.class.getResourceAsStream(path + "/set.txt");
                 final BufferedReader r = new BufferedReader(new InputStreamReader(stream, ENCODING))) {

                String line = r.readLine();

                while (line != null) {
                    set.add(path + "/" + line);
                    line = r.readLine();
                }
            } catch (final IOException ex) {
                Logger.getLogger(AccountTreeXMLFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return set;
    }

    private static class AccountImport {

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

            for (final Account child : root.getChildren()) {
                importChildren(engine, child);
            }
        }

        /**
         * Ensures that duplicate currencies are not created when the accounts are merged.
         *
         * @param engine  Engine with existing currencies
         * @param account account to correct
         */
        private static void fixCurrencies(final Engine engine, final Account account) {

            // If an existing currency matches, assign it to the account
            engine.getCurrencies().stream().filter(currencyNode -> account.getCurrencyNode()
                    .matches(currencyNode)).forEach(account::setCurrencyNode);

            // Need to persist the currency before the account if it does not exist within the database
            if (!engine.getCurrencies().contains(account.getCurrencyNode())) {
                engine.addCurrency(account.getCurrencyNode());
            }

            // match SecurityNodes to prevent duplicates
            if (account.memberOf(AccountGroup.INVEST)) {
                final Set<SecurityNode> nodes = account.getSecurities();

                for (final SecurityNode node : nodes) {
                    SecurityNode sNode = engine.getSecurity(node.getSymbol());

                    if (sNode == null) { // no match found
                        try {
                            sNode = (SecurityNode) node.clone();

                            for (final CurrencyNode currencyNode : engine.getCurrencies()) {
                                if (sNode.getReportedCurrencyNode().matches(currencyNode)) {
                                    sNode.setReportedCurrencyNode(currencyNode);
                                }
                            }
                            if (!engine.addSecurity(sNode)) {
                                final ResourceBundle rb = ResourceUtils.getBundle();
                                Logger.getLogger(AccountImport.class.getName()).log(Level.SEVERE,
                                        rb.getString("Message.Error.SecurityAdd"), sNode.getSymbol());
                            }
                        } catch (final CloneNotSupportedException e) {
                            Logger.getLogger(AccountImport.class.getName()).log(Level.SEVERE,
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
         * Ensures that duplicate currencies are not created when the accounts are merged.
         *
         * @param engine  Engine with existing currencies
         * @param account account to correct
         */
        private static void forceCurrency(final Engine engine, final Account account) {

            account.setCurrencyNode(engine.getDefaultCurrency());

            // match SecurityNodes to prevent duplicates
            if (account.memberOf(AccountGroup.INVEST)) {
                final Set<SecurityNode> nodes = account.getSecurities();

                for (final SecurityNode node : nodes) {
                    SecurityNode sNode = engine.getSecurity(node.getSymbol());

                    if (sNode == null) { // no match found
                        try {
                            sNode = (SecurityNode) node.clone();

                            sNode.setReportedCurrencyNode(engine.getDefaultCurrency());

                            if (!engine.addSecurity(sNode)) {
                                final ResourceBundle rb = ResourceUtils.getBundle();
                                Logger.getLogger(AccountImport.class.getName()).log(Level.SEVERE,
                                        rb.getString("Message.Error.SecurityAdd"), sNode.getSymbol());
                            }
                        } catch (final CloneNotSupportedException e) {
                            Logger.getLogger(AccountImport.class.getName()).log(Level.SEVERE, e.toString(), e);
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

            // fix the exchange rate DAO if needed
            engine.attachCurrencyNode(account.getCurrencyNode());

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

            for (final Account child : account.getChildren()) {
                importChildren(engine, child);
            }
        }
    }
}
