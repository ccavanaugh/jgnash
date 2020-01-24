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

/*
This scripts normalizes the case of the payee and memo fields
*/

var importTransaction;  // place holder for the passed ImportTransaction

/* This will be called first and passed the ImportTransaction */
function acceptTransaction(transaction) {
    importTransaction = transaction;    // do nothing with it in this script
}

function processMemo(memo) {
    return capitalizeFirstLetter(memo.toLocaleLowerCase());
}

function processPayee(payee) {
    return titleCase(payee.toLocaleLowerCase());
}

function getDescription(locale) {

    var Locale = Packages.java.util.Locale;

    switch (locale) {
        case Locale.ENGLISH:
            return "Tidy Memo and Payee fields";
        default:
            return "Tidy Memo and Payee fields";
    }
}

function capitalizeFirstLetter(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

function titleCase(str) {
    return str.replace(/(^|\s)[a-z]/g,function(f){return f.toUpperCase();});
}