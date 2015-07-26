/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.ui.components;

import java.awt.EventQueue;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.CommodityNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageProperty;
import jgnash.util.ResourceUtils;

/**
 * JComboBox to list Securities available to the account
 * 
 * @author Craig Cavanaugh
 *
 */
public class AccountSecurityComboBox extends AbstractCommodityComboBox<SecurityNode> {

    private Account account;

    public AccountSecurityComboBox(final Account acc) {
        super();

        if (acc == null || acc.getAccountType().getAccountGroup() != AccountGroup.INVEST) {
            throw new IllegalArgumentException(ResourceUtils.getString("Message.Error.InvalidAccountGroup"));
        }

        this.account = acc;

        addAll(account.getSecurities());

        if (model.getSize() > 0) {
            setSelectedIndex(0);
        }

        registerListeners();
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.COMMODITY);
    }

    @Override
    public void messagePosted(final Message event) {
        final Account a = event.getObject(MessageProperty.ACCOUNT);

        if (account.equals(a)) {
            final SecurityNode node = event.getObject(MessageProperty.COMMODITY);

            EventQueue.invokeLater(() -> {
                switch (event.getEvent()) {
                    case ACCOUNT_REMOVE:
                        MessageBus.getInstance().unregisterListener(AccountSecurityComboBox.this, MessageChannel.ACCOUNT, MessageChannel.COMMODITY);
                        model.removeAllElements();
                        account = null;
                        break;
                    case ACCOUNT_SECURITY_ADD:
                        model.addElement(node);
                        break;
                    case SECURITY_REMOVE:
                    case ACCOUNT_SECURITY_REMOVE:
                        final CommodityNode commodityNode = getSelectedNode();
                        model.removeElement(node);

                        if (commodityNode != null && node != null) {
                            if (commodityNode.equals(node)) {
                                setSelectedItem(null);
                            }
                        }
                        break;
                    case SECURITY_MODIFY:
                        updateNode(node);
                        break;
                    default:
                }
            });
        }
    }
}
