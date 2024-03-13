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

import se.uu.ub.cora.fedora.record.ResourceMetadata;
import se.uu.ub.cora.json.parser.JsonArray;
import se.uu.ub.cora.json.parser.JsonObject;
import se.uu.ub.cora.json.parser.JsonParser;

public class ResourceMetadataParserImp implements ResourceMetadataParser {
	private static final String FILESIZE_INNER_KEY = "@value";
	private static final String FILESIZE_OUTER_KEY = "http://www.loc.gov/premis/rdf/v1#hasSize";
	private static final String CHECKSUM_PREFIX = "urn:sha-512:";
	private static final String CHECKSUM_INNER_KEY = "@id";
	private static final String CHECKSUM_OUTER_KEY = "http://www.loc.gov/premis/rdf/v1#hasMessageDigest";
	private JsonParser jsonParser;

	public static ResourceMetadataParserImp usingJsonParser(JsonParser jsonParser) {
		return new ResourceMetadataParserImp(jsonParser);
	}

	private ResourceMetadataParserImp(JsonParser jsonParser) {
		this.jsonParser = jsonParser;
	}

	@Override
	public ResourceMetadata parse(String jsonString) {
		try {
			return tryToParse(jsonString);
		} catch (Exception e) {
			throw ResourceMetadataParserException
					.withMessageAndException("Failed to parse resource metadata", e);
		}
	}

	private ResourceMetadata tryToParse(String jsonString) {
		JsonObject surroundingObject = getSurroundingObject(jsonString);
		String fileSize = getFileSize(surroundingObject);
		String checksum = getChecksum(surroundingObject);

		return new ResourceMetadata(fileSize, checksum);
	}

	private String getFileSize(JsonObject surroundingObject) {
		return getInnerObjectValueByOuterAndInnerKey(surroundingObject, FILESIZE_INNER_KEY,
				FILESIZE_OUTER_KEY);
	}

	private String getChecksum(JsonObject surroundingObject) {
		String checksum = getInnerObjectValueByOuterAndInnerKey(surroundingObject,
				CHECKSUM_INNER_KEY, CHECKSUM_OUTER_KEY);
		return checksum.substring(CHECKSUM_PREFIX.length());
	}

	private String getInnerObjectValueByOuterAndInnerKey(JsonObject surroundingObject,
			String innerObjectkey, String outerObjectKey) {
		JsonObject checksumObject = getInnerObjectByKey(surroundingObject, outerObjectKey);

		return checksumObject.getValueAsJsonString(innerObjectkey).getStringValue();
	}

	private JsonObject getInnerObjectByKey(JsonObject surroundingObject, String key) {
		JsonArray checksumArray = surroundingObject.getValueAsJsonArray(key);
		return checksumArray.getValueAsJsonObject(0);
	}

	private JsonObject getSurroundingObject(String jsonString) {
		JsonArray parentArray = jsonParser.parseStringAsArray(jsonString);
		return parentArray.getValueAsJsonObject(0);
	}

	public JsonParser onlyForTestGetJsonParser() {
		return jsonParser;
	}
}
