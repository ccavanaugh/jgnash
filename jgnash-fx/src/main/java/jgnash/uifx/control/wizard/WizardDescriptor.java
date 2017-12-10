/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.util.Objects;

import jgnash.util.NotNull;

/**
 * Wizard Task Descriptor and status object.
 *
 * @author Craig Cavanaugh
 */
class WizardDescriptor {

    private boolean valid;

    private String description = "";

    WizardDescriptor(@NotNull final String description) {
        Objects.requireNonNull(description);

        setDescription(description);
    }

    public boolean isValid() {
        return valid;
    }

    public void setIsValid(boolean valid) {
        this.valid = valid;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    private void setDescription(@NotNull final String description) {
        Objects.requireNonNull(description);

        this.description = description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WizardDescriptor that = (WizardDescriptor) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription());
    }
}
