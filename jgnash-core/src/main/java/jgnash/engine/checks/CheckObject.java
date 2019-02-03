/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.engine.checks;

import java.io.Serializable;

import jgnash.resource.util.ResourceUtils;

/**
 * Check Object.
 *
 * @author Craig Cavanaugh
 */
public class CheckObject implements Serializable {

    public enum CheckObjectType {
        AMOUNT(ResourceUtils.getString("Item.Amount")),
        AMOUNT_TEXT(ResourceUtils.getString("Item.AmountText")),
        DATE(ResourceUtils.getString("Item.Date")),
        PAYEE(ResourceUtils.getString("Item.Payee")),
        MEMO(ResourceUtils.getString("Item.Memo")),
        ADDRESS(ResourceUtils.getString("Item.Address"));

        final transient private String typeName;

        CheckObjectType(final String name) {
            this.typeName = name;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }

    private String name = "New";

    private CheckObjectType type;

    private float x;

    private float y;

    public CheckObject() {
    }

    public void setType(CheckObjectType type) {
        this.type = type;
    }

    public CheckObjectType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getX() {
        return x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return y;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
