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
package se.uu.ub.cora.fedora.spy;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpMultiPartUploader;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class HttpHandlerFactorySpy implements HttpHandlerFactory {

	public String url;
	public HttpHandlerSpy factoredHttpHandler;

	public List<Integer> statusResponses = new ArrayList<>();
	public boolean throwExceptionRuntimeException = false;

	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public HttpHandler factor(String url) {
		MCR.addCall("url", url);
		this.url = url;
		factoredHttpHandler = new HttpHandlerSpy();
		factoredHttpHandler.statusResponse = statusResponses
				.get(MCR.getNumberOfCallsToMethod("factor") - 1);
		factoredHttpHandler.throwExceptionRuntimeException = throwExceptionRuntimeException;
		MCR.addReturned(factoredHttpHandler);
		return factoredHttpHandler;
	}

	@Override
	public HttpMultiPartUploader factorHttpMultiPartUploader(String url) {
		MCR.addCall("url", url);

		MCR.addReturned(null);
		return null;
	}

}
