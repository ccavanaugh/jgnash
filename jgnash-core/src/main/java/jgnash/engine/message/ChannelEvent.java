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
package jgnash.engine.message;

/**
 * Standard message channel events.
 * 
 * @author Craig Cavanaugh
 */
public enum ChannelEvent {
    ACCOUNT_ADD,
    ACCOUNT_ADD_FAILED,
    ACCOUNT_ATTRIBUTE_MODIFY,
    ACCOUNT_MODIFY,
    ACCOUNT_MODIFY_FAILED,
    ACCOUNT_REMOVE,
    ACCOUNT_REMOVE_FAILED,
    ACCOUNT_SECURITY_ADD,
    ACCOUNT_SECURITY_ADD_FAILED,
    ACCOUNT_SECURITY_REMOVE,
    ACCOUNT_SECURITY_REMOVE_FAILED,
    ACCOUNT_VISIBILITY_CHANGE,
    ACCOUNT_VISIBILITY_CHANGE_FAILED,
    BACKGROUND_PROCESS_STARTED,
    BACKGROUND_PROCESS_STOPPED,
    BUDGET_ADD,
    BUDGET_ADD_FAILED,
    BUDGET_GOAL_UPDATE,
    BUDGET_GOAL_UPDATE_FAILED,
    BUDGET_UPDATE,
    BUDGET_UPDATE_FAILED,
    BUDGET_REMOVE,
    CONFIG_MODIFY,
    // CONFIG_MODIFY_FAILED,
    CURRENCY_ADD,
    CURRENCY_ADD_FAILED,
    CURRENCY_MODIFY,
    CURRENCY_MODIFY_FAILED,
    CURRENCY_REMOVE,
    CURRENCY_REMOVE_FAILED,
    EXCHANGE_RATE_ADD,
    EXCHANGE_RATE_ADD_FAILED,
    EXCHANGE_RATE_REMOVE,
    EXCHANGE_RATE_REMOVE_FAILED,
    REMINDER_ADD,
    REMINDER_ADD_FAILED,
    REMINDER_REMOVE,
    REMINDER_UPDATE,
    REMINDER_UPDATE_FAILED,
    SECURITY_ADD,
    SECURITY_ADD_FAILED,
    SECURITY_MODIFY,
    SECURITY_MODIFY_FAILED,
    SECURITY_REMOVE,
    SECURITY_REMOVE_FAILED,
    SECURITY_HISTORY_ADD,
    SECURITY_HISTORY_ADD_FAILED,
    SECURITY_HISTORY_REMOVE,
    SECURITY_HISTORY_REMOVE_FAILED,
    SECURITY_HISTORY_EVENT_ADD,
    SECURITY_HISTORY_EVENT_ADD_FAILED,
    SECURITY_HISTORY_EVENT_REMOVE,
    SECURITY_HISTORY_EVENT_REMOVE_FAILED,
    TRANSACTION_ADD,
    TRANSACTION_ADD_FAILED,
    TRANSACTION_REMOVE,
    TRANSACTION_REMOVE_FAILED,
    TAG_ADD,
    TAG_ADD_FAILED,
    TAG_MODIFY,
    TAG_MODIFY_FAILED,
    TAG_REMOVE,
    TAG_REMOVE_FAILED,
    FILE_CLOSING,
    FILE_NOT_FOUND,
    FILE_IO_ERROR,
    FILE_LOAD_FAILED,
    FILE_LOAD_SUCCESS,
}
