/*
 * Copyright 2022 Uppsala University Library
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

public class ResourceMetadataParserException extends RuntimeException {

	private static final long serialVersionUID = -255261285196817577L;

	private ResourceMetadataParserException(String message) {
		super(message);
	}

	private ResourceMetadataParserException(String message, Exception e) {
		super(message, e);
	}

	public static ResourceMetadataParserException withMessage(String message) {
		return new ResourceMetadataParserException(message);
	}

	public static ResourceMetadataParserException withMessageAndException(String message, Exception e) {
		return new ResourceMetadataParserException(message, e);
	}

}
