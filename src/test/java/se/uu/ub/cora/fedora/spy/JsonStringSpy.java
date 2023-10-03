package se.uu.ub.cora.fedora.spy;

import se.uu.ub.cora.json.parser.JsonString;
import se.uu.ub.cora.json.parser.JsonValueType;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class JsonStringSpy implements JsonString {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public JsonStringSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getValueType", () -> JsonValueType.FALSE);
		MRV.setDefaultReturnValuesSupplier("getStringValue", String::new);
	}

	@Override
	public JsonValueType getValueType() {
		return (JsonValueType) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String getStringValue() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

}
