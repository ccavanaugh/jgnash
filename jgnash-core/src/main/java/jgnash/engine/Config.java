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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jgnash.util.NotNull;
import jgnash.util.Nullable;
import jgnash.util.Resource;

import javax.persistence.*;

/**
 * A general configuration class so that global configuration information may be stored inside the database
 *
 * @author Craig Cavanaugh
 */
@Entity
public class Config extends StoredObject {

    @ManyToOne(cascade = CascadeType.PERSIST)
    private CurrencyNode defaultCurrency;

    private String accountSeparator = ":";

    private float fileVersion = 0f;

    private transient ReadWriteLock preferencesLock;

    /**
     * Contains a list a items to display in the transaction number combo
     */
    @ElementCollection
    private List<String> transactionNumberItems = new ArrayList<>();

    /**
     * Contains a list of custom transaction tags a user may apply
     */
    @ElementCollection
    @SuppressWarnings("unused")
    private Set<String> customTransactionTags = new HashSet<>();

    /**
     * {@code Map} for file based operation preferences.
     *
     * GUI locations, last used accounts, etc
     * should not be stored here as they will be dependent on the user.
     * Only values required for operation consistency should be stored here.
     */
    @ElementCollection
    @Column(columnDefinition = "varchar(8192)")
    private Map<String, String> preferences = new HashMap<>();

    public Config() {
        preferencesLock = new ReentrantReadWriteLock(true);
    }

    void initialize() {
        Account.setAccountSeparator(getAccountSeparator());
    }

    public float getFileVersion() {
        return fileVersion;
    }

    @SuppressWarnings("SameParameterValue")
    void setFileVersion(final float fileVersion) {
        this.fileVersion = fileVersion;
    }

    void setDefaultCurrency(final CurrencyNode defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    CurrencyNode getDefaultCurrency() {
        return defaultCurrency;
    }

    void setAccountSeparator(final String accountSeparator) {
        this.accountSeparator = accountSeparator;
        Account.setAccountSeparator(this.accountSeparator);
    }

    String getAccountSeparator() {
        return accountSeparator;
    }

    public void setTransactionNumberList(final List<String> transactionNumberItems) {
        if (transactionNumberItems != null) {
            this.transactionNumberItems.clear();
            this.transactionNumberItems.addAll(transactionNumberItems);
        }
    }

    public List<String> getTransactionNumberList() {
        if (transactionNumberItems.isEmpty()) {
            Resource rb = Resource.get();
            transactionNumberItems.add(rb.getString("Item.EFT"));
            transactionNumberItems.add(rb.getString("Item.Trans"));
        }
        return new ArrayList<>(transactionNumberItems);
    }

    void setPreference(@NotNull final String key, @Nullable final String value) {

        preferencesLock.writeLock().lock();

        try {
            if (key.isEmpty()) {
                throw new RuntimeException(Resource.get().getString("Message.Error.EmptyKey"));
            }

            if (value == null) {    // find and remove
                preferences.remove(key);
            } else {
                preferences.put(key, value);
            }
        } finally {
            preferencesLock.writeLock().unlock();
        }
    }

    @Nullable
    String getPreference(@NotNull final String key) {
        preferencesLock.readLock().lock();

        try {
            if (key.isEmpty()) {
                throw new RuntimeException(Resource.get().getString("Message.Error.EmptyKey"));
            }

            return preferences.get(key);
        } finally {
            preferencesLock.readLock().unlock();
        }
    }

    /**
     * Needed by XStream for proper initialization
     *
     * @return Properly initialized Config object
     */
    protected Object readResolve() {
        postLoad();
        return this;
    }

    @PostLoad
    private void postLoad() {
        preferencesLock = new ReentrantReadWriteLock(true);
    }

}
