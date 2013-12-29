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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

}
