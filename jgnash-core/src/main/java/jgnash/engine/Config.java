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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;

import jgnash.resource.util.ResourceUtils;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * A general configuration class so that global configuration information may be stored inside the database.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class Config extends StoredObject {

    private static final String CREATE_BACKUPS = "CreateBackups";

    private static final String MAX_BACKUPS = "MaxBackups";

    private static final String REMOVE_BACKUPS = "RemoveBackups";

    private static final String LAST_SECURITIES_UPDATE_TIMESTAMP = "LastSecuritiesUpdateTimestamp";

    private static final int MAX_BACKUPS_DEFAULT = 5;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private CurrencyNode defaultCurrency;

    private String accountSeparator = ":";

    /**
     * Current file format
     */
    private String fileFormat = Engine.CURRENT_MAJOR_VERSION + "." + Engine.CURRENT_MINOR_VERSION;

    private transient ReadWriteLock preferencesLock;

    /**
     * Contains a list a items to display in the transaction number combo.
     */
    @ElementCollection
    private final List<String> transactionNumberItems = new ArrayList<>();

    /**
     * {@code Map} for file based operation preferences.
     * <p>
     * GUI locations, last used accounts, etc
     * should not be stored here as they will be dependent on the user.
     * Only values required for operation consistency should be stored here.
     */
    @ElementCollection
    @Column(columnDefinition = "varchar(8192)")
    private final Map<String, String> preferences = new HashMap<>();

    public Config() {
        preferencesLock = new ReentrantReadWriteLock(true);
    }

    void initialize() {
        Account.setAccountSeparator(getAccountSeparator());
    }

    public String getFileFormat() {
        return fileFormat;
    }

    int getMajorFileFormatVersion() {
        return Integer.parseInt(getFileFormat().split("\\.")[0]);
    }

    int getMinorFileFormatVersion() {
        return Integer.parseInt(getFileFormat().split("\\.")[1]);
    }

    void updateFileVersion() {
        this.fileFormat = Engine.CURRENT_MAJOR_VERSION + "." + Engine.CURRENT_MINOR_VERSION;
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

    void setTransactionNumberList(final List<String> transactionNumberItems) {
        if (transactionNumberItems != null) {
            this.transactionNumberItems.clear();
            this.transactionNumberItems.addAll(transactionNumberItems);
        }
    }

    List<String> getTransactionNumberList() {
        if (transactionNumberItems.isEmpty()) {
            final ResourceBundle rb = ResourceUtils.getBundle();

            transactionNumberItems.add(rb.getString("Item.EFT"));
            transactionNumberItems.add(rb.getString("Item.Trans"));
        }
        return new ArrayList<>(transactionNumberItems);
    }

    void setPreference(@NotNull final String key, @Nullable final String value) {

        preferencesLock.writeLock().lock();

        try {
            if (key.isEmpty()) {
                throw new RuntimeException(ResourceUtils.getString("Message.Error.EmptyKey"));
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
                throw new RuntimeException(ResourceUtils.getString("Message.Error.EmptyKey"));
            }

            return preferences.get(key);
        } finally {
            preferencesLock.readLock().unlock();
        }
    }

    boolean createBackups() {
        final String result = getPreference(CREATE_BACKUPS);

        return result == null || Boolean.parseBoolean(result);
    }

    void setCreateBackups(final boolean createBackups) {
        setPreference(CREATE_BACKUPS, Boolean.toString(createBackups));
    }

    int getRetainedBackupLimit() {
        final String result = getPreference(MAX_BACKUPS);

        if (result != null) {
            return Integer.parseInt(result);
        }

        return MAX_BACKUPS_DEFAULT;
    }

    void setRetainedBackupLimit(final int retainedBackupLimit) {
        setPreference(MAX_BACKUPS, Integer.toString(retainedBackupLimit));
    }

    boolean removeOldBackups() {
        final String result = getPreference(REMOVE_BACKUPS);

        return result == null || Boolean.parseBoolean(result);
    }

    void setRemoveOldBackups(final boolean removeOldBackups) {
        setPreference(REMOVE_BACKUPS, Boolean.toString(removeOldBackups));
    }

    void setLastSecuritiesUpdateTimestamp(@NotNull final LocalDateTime localDateTime) {
        setPreference(LAST_SECURITIES_UPDATE_TIMESTAMP, localDateTime.toString());
    }

    @NotNull
    LocalDateTime getLastSecuritiesUpdateTimestamp() {
        final String result = getPreference(LAST_SECURITIES_UPDATE_TIMESTAMP);

        if (result != null) {
            return LocalDateTime.parse(result);
        }

        return LocalDateTime.MIN;
    }

    /**
     * Required by XStream for proper initialization.
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
