/*
 * Copyright 2023 Uppsala University Library
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
package se.uu.ub.cora.fedora.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.record.ResourceMetadata;
import se.uu.ub.cora.fedora.spy.JsonArraySpy;
import se.uu.ub.cora.fedora.spy.JsonObjectSpy;
import se.uu.ub.cora.fedora.spy.JsonParserSpy;
import se.uu.ub.cora.fedora.spy.JsonStringSpy;
import se.uu.ub.cora.json.parser.JsonParser;

public class ResourceMetadataParserTest {

	private static final String FILESIZE = "1865987";

	private static final String PARSE_STRING_AS_ARRAY = "parseStringAsArray";

	private static final String CHECKSUM = "71d0d4ffd68b9ce30008f5e01c54f75d1d9d016aa72b513e70c93"
			+ "21575a6add02e6f89790d2f38dbe4d09b4d6e07a51c9215b425ef381b1b785195d957e65fb0";

	private static final String METADATA_LD_JSON = """
			[
			   {
			       "@id": "http://localhost:38087/fcrepo/rest/systemOne/resource/binary:binary:3045607418632979-master",
			       "http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#hasMimeType": [
			           {
			               "@value": "application/octet-stream"
			           }
			       ],
			       "http://www.loc.gov/premis/rdf/v1#hasMessageDigest": [
			           {
			               "@id": "urn:sha-512:71d0d4ffd68b9ce30008f5e01c54f75d1d9d016aa72b513e70c9321575a6add02e6f89790d2f38dbe4d09b4d6e07a51c9215b425ef381b1b785195d957e65fb0"
			           }
			       ],
			       "http://www.loc.gov/premis/rdf/v1#hasSize": [
			           {
			               "@type": "http://www.w3.org/2001/XMLSchema#long",
			               "@value": "1865987"
			           }
			       ]
			   }
			   ]""";
	private ResourceMetadataParser parser;
	private JsonParserSpy jsonParser;

	private JsonObjectSpy surroundingObjectSpy;

	@BeforeMethod
	public void beforeMethod() {
		jsonParser = new JsonParserSpy();
		parser = ResourceMetadataParserImp.usingJsonParser(jsonParser);

		surroundingObjectSpy = setUpJsonSpiesWithSurroundingObject();
		setUpFileSizeSpy();
		setUpChecksumSpy();
	}

	@Test
	public void parseErrorThrowExceptionTest() throws Exception {
		jsonParser.MRV.setAlwaysThrowException(PARSE_STRING_AS_ARRAY,
				new RuntimeException("parse error"));
		try {
			parser.parse("");
			fail("It should throw an exception");
		} catch (Exception e) {
			assertTrue(e instanceof ResourceMetadataParserException);
			assertEquals(e.getMessage(), "Failed to parse resource metadata");
			assertEquals(e.getCause().getMessage(), "parse error");
		}
	}

	@Test
	public void parseChecksumTest() throws Exception {

		ResourceMetadata metadata = parser.parse(METADATA_LD_JSON);
		assertNotNull(metadata);
		assertEquals(metadata.checksumSHA512(), CHECKSUM);
	}

	private void setUpChecksumSpy() {
		String surroundingObjectKey = "http://www.loc.gov/premis/rdf/v1#hasMessageDigest";
		String innerObjectKey = "@id";
		String innerObjectValue = "urn:sha-512:" + CHECKSUM;
		setUpInnerObjectKeyValue(surroundingObjectKey, innerObjectKey, innerObjectValue);
	}

	@Test
	public void parseSizeTest() throws Exception {

		ResourceMetadata metadata = parser.parse(METADATA_LD_JSON);
		assertNotNull(metadata);
		assertEquals(metadata.fileSize(), FILESIZE);
	}

	@Test
	public void parserOnlyForTestGetJsonParserTest() throws Exception {
		ResourceMetadataParserImp parserImp = (ResourceMetadataParserImp) parser;
		JsonParser forTestParser = parserImp.onlyForTestGetJsonParser();
		assertEquals(forTestParser, jsonParser);
	}

	private void setUpFileSizeSpy() {
		String surroundingObjectKey = "http://www.loc.gov/premis/rdf/v1#hasSize";
		String innerObjectKey = "@value";
		String innerObjectValue = FILESIZE;
		setUpInnerObjectKeyValue(surroundingObjectKey, innerObjectKey, innerObjectValue);
	}

	private void setUpInnerObjectKeyValue(String surroundingObjectKey, String innerObjectKey,
			String innerObjectValue) {
		JsonArraySpy jsonArraySpy = new JsonArraySpy();
		JsonObjectSpy jsonObjectToReturn = new JsonObjectSpy();
		jsonArraySpy.MRV.setDefaultReturnValuesSupplier("getValueAsJsonObject",
				() -> jsonObjectToReturn);
		surroundingObjectSpy.MRV.setSpecificReturnValuesSupplier("getValueAsJsonArray",
				() -> jsonArraySpy, surroundingObjectKey);

		JsonStringSpy objectValueJsonStringSpy = new JsonStringSpy();
		objectValueJsonStringSpy.MRV.setDefaultReturnValuesSupplier("getStringValue",
				() -> innerObjectValue);

		jsonObjectToReturn.MRV.setSpecificReturnValuesSupplier("getValueAsJsonString",
				() -> objectValueJsonStringSpy, innerObjectKey);
	}

	private JsonObjectSpy setUpJsonSpiesWithSurroundingObject() {
		JsonArraySpy parentArraySpy = new JsonArraySpy();
		jsonParser.MRV.setDefaultReturnValuesSupplier(PARSE_STRING_AS_ARRAY, () -> {
			return parentArraySpy;
		});

		JsonObjectSpy surroundingObjectSpy = new JsonObjectSpy();

		parentArraySpy.MRV.setDefaultReturnValuesSupplier("getValueAsJsonObject", () -> {
			return surroundingObjectSpy;
		});
		return surroundingObjectSpy;
	}

}
