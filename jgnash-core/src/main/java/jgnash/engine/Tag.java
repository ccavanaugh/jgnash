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

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Class for user created tags to mark transactions.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class Tag extends StoredObject implements Comparable<Tag> {

    /**
     * Tag name
     */
    private String name = "";

    /**
     * Tag description
     */
    @Column(columnDefinition = "VARCHAR(2048)")
    private String description = "";

    /**
     * Tag color
     * <p>
     * Default value is black
     */
    private long color = 255;

    /**
     * Icon unicode value
     */
    private Integer unicode = 0xf764;   // circle

    public Tag() {
        // zero arg constructor required for persistence
    }

    public void setDescription(final String description) {
        if (description != null && description.length() <= 2048) {
            this.description = description;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setName(final String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Sets the Tag color
     *
     * @see jgnash.util.EncodeDecode#longToColorString(long)
     * @param color integer equivalent of a web based color string
     */
    public void setColor(final long color) {
        this.color = color;
    }

    /**
     * Returns the Tag color
     *
     * @see jgnash.util.EncodeDecode#colorStringToLong(String)
     * @return integer value encoded from a web based color string
     */
    public long getColor() {
        return color;
    }

    /**
     * Returns the Tag shape
     *
     * @return char value of the font character
     */
    public int getShape() {
        if (unicode == null) {
            unicode = 0xf0e4; // bug symbol.. from conversion of an older file
        }
        return unicode;
    }

    /**
     * Sets the Tag shape
     *
     * @param unicode value of the font character
     */
    public void setShape(final int unicode) {
        this.unicode = unicode;
    }

    @Override
    public java.lang.String toString() {
        return getName();
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(final Tag o) {
        if (o == this) {
            return 0;
        }

        int result = name.compareTo(o.name);
        if (result != 0) {
            return result;
        }

        result = Long.compare(color, o.color);
        if (result != 0) {
            return result;
        }

        result = Integer.compare(getShape(), o.getShape());
        if (result != 0) {
            return result;
        }

        result = description.compareTo(o.description);
        if (result != 0) {
            return result;
        }

        return getUuid().compareTo(o.getUuid());
    }
}
