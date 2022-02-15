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

import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpMultiPartUploader;

public class HttpHandlerFactorySpy implements HttpHandlerFactory {

	public String url;
	public HttpHandlerSpy factoredHttpHandler;

	public int statusResponse = 201;

	@Override
	public HttpHandler factor(String url) {
		this.url = url;
		factoredHttpHandler = new HttpHandlerSpy();
		factoredHttpHandler.statusResponse = statusResponse;
		return factoredHttpHandler;
	}

	@Override
	public HttpMultiPartUploader factorHttpMultiPartUploader(String url) {
		// TODO Auto-generated method stub
		return null;
	}

}
