package se.uu.ub.cora.fedora.spy;

import se.uu.ub.cora.fedora.ResourceMetadata;
import se.uu.ub.cora.fedora.internal.ResourceMetadataParser;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class ResourceMetadataParserSpy implements ResourceMetadataParser {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public ResourceMetadataParserSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("parse",
				() -> new ResourceMetadata("someFileSize", "someChecksum"));
	}

	@Override
	public ResourceMetadata parse(String jsonString) {
		return (ResourceMetadata) MCR.addCallAndReturnFromMRV("jsonString", jsonString);
	}

}
