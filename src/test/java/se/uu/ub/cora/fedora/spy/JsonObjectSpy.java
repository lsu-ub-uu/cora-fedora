package se.uu.ub.cora.fedora.spy;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import se.uu.ub.cora.json.parser.JsonArray;
import se.uu.ub.cora.json.parser.JsonObject;
import se.uu.ub.cora.json.parser.JsonString;
import se.uu.ub.cora.json.parser.JsonValue;
import se.uu.ub.cora.json.parser.JsonValueType;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class JsonObjectSpy implements JsonObject {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public JsonObjectSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getValueType", () -> JsonValueType.FALSE);
		MRV.setDefaultReturnValuesSupplier("getValue", JsonValueSpy::new);
		MRV.setDefaultReturnValuesSupplier("getValueAsJsonString", String::new);
		MRV.setDefaultReturnValuesSupplier("getValueAsJsonObject", JsonObjectSpy::new);
		MRV.setDefaultReturnValuesSupplier("getValueAsJsonArray", JsonArraySpy::new);
		MRV.setDefaultReturnValuesSupplier("containsKey", () -> false);
		MRV.setDefaultReturnValuesSupplier("keySet", HashSet<String>::new);
		MRV.setDefaultReturnValuesSupplier("entrySet", HashSet<Entry<String, JsonValue>>::new);
		MRV.setDefaultReturnValuesSupplier("size", () -> 0);
		MRV.setDefaultReturnValuesSupplier("toJsonFormattedString", String::new);
	}

	@Override
	public JsonValueType getValueType() {
		return (JsonValueType) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public JsonValue getValue(String key) {
		return (JsonValue) MCR.addCallAndReturnFromMRV("key", key);
	}

	@Override
	public JsonString getValueAsJsonString(String key) {
		return (JsonString) MCR.addCallAndReturnFromMRV("key", key);
	}

	@Override
	public JsonObject getValueAsJsonObject(String key) {
		return (JsonObject) MCR.addCallAndReturnFromMRV("key", key);
	}

	@Override
	public JsonArray getValueAsJsonArray(String key) {
		return (JsonArray) MCR.addCallAndReturnFromMRV("key", key);
	}

	@Override
	public boolean containsKey(String key) {
		return (boolean) MCR.addCallAndReturnFromMRV("key", key);
	}

	@Override
	public Set<String> keySet() {
		return (Set<String>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Set<Entry<String, JsonValue>> entrySet() {
		// TODO Auto-generated method stub
		return (Set<Entry<String, JsonValue>>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public int size() {
		return (int) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String toJsonFormattedString() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public void removeKey(String key) {
		// TODO Auto-generated method stub

	}

}
