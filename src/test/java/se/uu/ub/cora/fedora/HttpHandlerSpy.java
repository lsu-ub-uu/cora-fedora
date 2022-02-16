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

import java.io.InputStream;

import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class HttpHandlerSpy implements HttpHandler {

	public String requestMetod;

	MethodCallRecorder MCR = new MethodCallRecorder();

	public int statusResponse = 201;
	public boolean throwExceptionRuntimeException = false;
	public InputStream inputStreamToRead = new InputStreamSpy();

	@Override
	public void setRequestMethod(String requestMetod) {
		MCR.addCall("requestMetod", requestMetod);
		this.requestMetod = requestMetod;
	}

	@Override
	public String getResponseText() {
		MCR.addCall();
		String responseText = "some response text";
		MCR.addReturned(responseText);
		return responseText;
	}

	@Override
	public int getResponseCode() {
		MCR.addCall();
		MCR.addReturned(statusResponse);
		return statusResponse;
	}

	@Override
	public void setOutput(String outputString) {
		MCR.addCall("outputString", outputString);
		if (throwExceptionRuntimeException) {
			throw new RuntimeException("Some error from HttpHandlerSpy");
		}
	}

	@Override
	public void setRequestProperty(String key, String value) {
		MCR.addCall("key", key, "value", value);
		// TODO Auto-generated method stub
	}

	@Override
	public String getErrorText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStreamOutput(InputStream stream) {
		MCR.addCall("stream", stream);

	}

	@Override
	public String getHeaderField(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBasicAuthorization(String username, String password) {
		// TODO Auto-generated method stub

	}

	@Override
	public InputStream getResponseBinary() {
		MCR.addCall();

		MCR.addReturned(inputStreamToRead);
		return inputStreamToRead;
	}

}
