/*
 * Copyright 2021 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.fedora.spy;

import java.util.Iterator;

import se.uu.ub.cora.json.parser.JsonArray;
import se.uu.ub.cora.json.parser.JsonObject;
import se.uu.ub.cora.json.parser.JsonString;
import se.uu.ub.cora.json.parser.JsonValue;
import se.uu.ub.cora.json.parser.JsonValueType;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class JsonArraySpy implements JsonArray {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public JsonArraySpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getValueType", () -> JsonValueType.FALSE);
		MRV.setDefaultReturnValuesSupplier("iterator", IteratorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getValue", JsonValueSpy::new);
		MRV.setDefaultReturnValuesSupplier("getValueAsJsonString", JsonStringSpy::new);
		MRV.setDefaultReturnValuesSupplier("getValueAsJsonObject", JsonObjectSpy::new);
		MRV.setDefaultReturnValuesSupplier("getValueAsJsonArray", JsonArraySpy::new);
		MRV.setDefaultReturnValuesSupplier("toJsonFormattedString", String::new);
	}

	@Override
	public JsonValueType getValueType() {
		return (JsonValueType) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Iterator<JsonValue> iterator() {
		return (Iterator<JsonValue>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public JsonValue getValue(int index) {
		return (JsonValue) MCR.addCallAndReturnFromMRV("index", index);
	}

	@Override
	public JsonString getValueAsJsonString(int index) {
		return (JsonString) MCR.addCallAndReturnFromMRV("index", index);
	}

	@Override
	public JsonObject getValueAsJsonObject(int index) {
		return (JsonObject) MCR.addCallAndReturnFromMRV("index", index);
	}

	@Override
	public JsonArray getValueAsJsonArray(int index) {
		return (JsonArray) MCR.addCallAndReturnFromMRV("index", index);
	}

	@Override
	public String toJsonFormattedString() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

}
