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
package se.uu.ub.cora.fedora;

import java.io.IOException;
import java.io.InputStream;

import se.uu.ub.cora.httphandler.HttpMultiPartUploader;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class HttpMultiPartUploaderSpy implements HttpMultiPartUploader {

	MethodCallRecorder MCR = new MethodCallRecorder();
	private int responseCode = 200;
	private String responseText = "response text from spy";

	@Override
	public String getResponseText() {
		MCR.addCall();

		MCR.addReturned(responseText);
		return responseText;
	}

	@Override
	public int getResponseCode() {
		MCR.addCall();

		MCR.addReturned(responseCode);
		return responseCode;
	}

	@Override
	public String getErrorText() {
		MCR.addCall();

		String returnedValue = "error from spy";
		MCR.addReturned(returnedValue);
		return returnedValue;
	}

	@Override
	public void addFormField(String name, String value) {
		MCR.addCall("name", name, "value", value);

	}

	@Override
	public void addFilePart(String fieldName, String fileName, InputStream stream)
			throws IOException {
		MCR.addCall("fieldName", fieldName, "fileName", fileName, "stream", stream);

	}

	@Override
	public void addHeaderField(String name, String value) {
		MCR.addCall("name", name, "value", value);

	}

	@Override
	public void done() throws IOException {
		MCR.addCall();

	}

	@Override
	public void setRequestMethod(String requestMethod) {
		MCR.addCall("requestMethod", requestMethod);

	}

}
