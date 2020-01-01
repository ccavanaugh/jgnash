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
package jgnash.report.table;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import jgnash.util.NotNull;

import static jgnash.report.table.AbstractReportTableModel.DEFAULT_GROUP;

/**
 * Used to create Groups within a report
 *
 * @author Craig Cavanaugh
 */
public class GroupInfo implements Comparable<GroupInfo> {

    /**
     * Group name
     */
    public final String group;

    /**
     * Summation values for cross tabulation of columns
     */
    private final Map<Integer, BigDecimal> summationMap = new HashMap<>();

    private boolean hasSummation = false;

    private GroupInfo(@NotNull final String group) {
        Objects.requireNonNull(group);

        this.group = group;
    }

    public static Set<GroupInfo> getGroups(final AbstractReportTableModel tableModel) {
        final Map<String, GroupInfo> groupInfoMap = new HashMap<>();

        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            final ColumnStyle columnStyle = tableModel.getColumnStyle(c);

            if (columnStyle == ColumnStyle.GROUP || columnStyle == ColumnStyle.GROUP_NO_HEADER) {
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                   groupInfoMap.computeIfAbsent(tableModel.getValueAt(r, c).toString(), GroupInfo::new);
                }
            }
        }

        // create a default group for tables that do not specify one
        if (groupInfoMap.isEmpty()) {
            final GroupInfo groupInfo = new GroupInfo(DEFAULT_GROUP);
            groupInfoMap.put(DEFAULT_GROUP, groupInfo);
        }

        // perform summation
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            final GroupInfo groupInfo = groupInfoMap.get(tableModel.getGroup(r));

            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                if (tableModel.getColumnClass(c) == BigDecimal.class) {
                    switch (tableModel.getColumnStyle(c)) {
                        case AMOUNT_SUM:
                        case BALANCE_WITH_SUM:
                        case BALANCE_WITH_SUM_AND_GLOBAL:
                            groupInfo.addValue(c, (BigDecimal) tableModel.getValueAt(r, c));
                        default:
                            break;
                    }
                }
            }
        }

        return new TreeSet<>(groupInfoMap.values());
    }

    @Override
    public int compareTo(final GroupInfo o) {
        return group.compareTo(o.group);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof GroupInfo) {
            return group.equals(((GroupInfo) o).group);
        }
        return false;
    }

    private void addValue(final int column, final BigDecimal value) {
        if (value != null) {    // protect against a null / filtered value
            summationMap.put(column, getValue(column).add(value));
            hasSummation = true;
        }
    }

    @NotNull
    public BigDecimal getValue(final int column) {
        return summationMap.getOrDefault(column, BigDecimal.ZERO);
    }

    public boolean hasSummation() {
        return hasSummation;
    }

    public int hashCode() {
        return group.hashCode();
    }
}
