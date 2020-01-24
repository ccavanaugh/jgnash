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
package jgnash.uifx.control.wizard;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Support class to make implementation of {@code WizardPaneController} less repetitive.  Subclasses are required
 * to overwrite {@code toString()} and return a meaningful description.  {@code toString()} will be called during
 * class initialization.
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractWizardPaneController<K extends Enum<?>> implements WizardPaneController<K> {

    private final SimpleObjectProperty<WizardDescriptor> descriptorProperty =
            new SimpleObjectProperty<>(new WizardDescriptor(toString()));

    @Override
    public ObjectProperty<WizardDescriptor> descriptorProperty() {
        return descriptorProperty;
    }

    /**
     * Should be called when a change to validity occurs.
     */
    protected void updateDescriptor() {
        descriptorProperty().get().setIsValid(isPaneValid());
    }
}
