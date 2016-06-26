package net.bzzt.swift.mt940;
/*
 * Copyright (C) 2008 Arnout Engelen
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

import java.util.ArrayList;
import java.util.List;


public class Mt940File {
	private List<Mt940Record> records = new ArrayList<>();

	public List<Mt940Record> getRecords() {
		return records;
	}

	public void setRecords(List<Mt940Record> records) {
		this.records = records;
	}

	List<Mt940Entry> getEntries() {
		List<Mt940Entry> retval = new ArrayList<>();
		for (Mt940Record record : getRecords())
		{
			retval.addAll(record.getEntries());
		}
		return retval;
	}
}
