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

import java.util.ArrayList;
import java.util.List;

import jgnash.util.Resource;

/**
 * A general configuration class so that application configuration can be stored inside the database
 * 
 * @author Craig Cavanaugh
 * @version $Id: Config.java 3117 2012-01-22 12:10:41Z ccavanaugh $
 */
public class Config extends StoredObject {

    private static final long serialVersionUID = -7317806359608639763L;

    private CurrencyNode defaultCurrency;

    @SuppressWarnings("unused")
    private String name = "DefaultConfig"; // left for compatibility

    private String accountSeparator = ":";

    private float fileVersion = 0f;

    /**
     * Contains a list a items to display in the transaction number combo
     */
    private List<String> transactionNumberItems = new ArrayList<>();

    protected void initialize() {
        Account.setAccountSeparator(getAccountSeparator());
    }

    public float getFileVersion() {
        return fileVersion;
    }

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
            this.transactionNumberItems = transactionNumberItems;
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
