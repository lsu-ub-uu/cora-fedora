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

import java.io.InputStream;

import se.uu.ub.cora.fedora.FedoraException;
import se.uu.ub.cora.fedora.FedoraWrapper;
import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;

public class FedoraImp implements FedoraWrapper {

	private static final int OK = 200;
	private HttpHandlerFactory httpHandlerFactory;
	private String baseUrl;

	public FedoraImp(HttpHandlerFactory httpHandlerFactory, String baseUrl) {
		this.httpHandlerFactory = httpHandlerFactory;
		this.baseUrl = baseUrl;
	}

	@Override
	public void create(String recordId, String fedoraXML) {
		HttpHandler httpHandler = setupHttpHandlerForStore(recordId, fedoraXML);
		int responseCode = callFedora(httpHandler);
		throwErrorIfCreateNotOk(responseCode, recordId, "record");
	}

	private void throwErrorIfCreateNotOk(int responseCode, String recordId, String type) {
		if (responseCode != 201) {
			throw FedoraException
					.withMessage("Error storing " + type + " in Fedora, recordId: " + recordId);
		}
	}

	private HttpHandler setupHttpHandlerForStore(String recordId, String fedoraXML) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "PUT");
		httpHandler.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
		httpHandler.setOutput(fedoraXML);
		return httpHandler;
	}

	@Override
	public String read(String recordId) {
		HttpHandler httpHandler = setUpHttpHandlerForRead(recordId);
		int responseCode = callFedora(httpHandler);
		throwErrorIfReadNotOk(responseCode, recordId, "record");
		return httpHandler.getResponseText();
	}

	private void throwErrorIfReadNotOk(int responseCode, String recordId, String type) {
		if (responseCode != OK) {
			throw FedoraException
					.withMessage("Error reading " + type + " from Fedora, recordId: " + recordId);
		}
	}

	private HttpHandler setUpHttpHandlerForRead(String recordId) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "GET");
		httpHandler.setRequestProperty("Accept", "text/plain;charset=utf-8");
		return httpHandler;
	}

	private HttpHandler factorHttpHandler(String recordId, String requestMethod) {
		HttpHandler httpHandler = httpHandlerFactory.factor(baseUrl + recordId);
		httpHandler.setRequestMethod(requestMethod);
		return httpHandler;
	}

	@Override
	public void update(String recordId, String fedoraXML) {
		HttpHandler httpHandler = setupHttpHandlerForStore(recordId, fedoraXML);
		int responseCode = callFedora(httpHandler);
		throwErrorIfUpdateNotOk(responseCode, recordId);
	}

	private int callFedora(HttpHandler httpHandler) {
		return httpHandler.getResponseCode();
	}

	private void throwErrorIfUpdateNotOk(int responseCode, String recordId) {
		if (responseCode != 204) {
			throw FedoraException
					.withMessage("Error storing record in Fedora, recordId: " + recordId);
		}
	}

	@Override
	public void createBinary(String recordId, InputStream binary, String contentType) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "PUT");
		httpHandler.setRequestProperty("Content-Type", contentType);
		httpHandler.setStreamOutput(binary);

		int responseCode = callFedora(httpHandler);
		throwErrorIfCreateBinaryNotOk(recordId, responseCode);

	}

	private void throwErrorIfCreateBinaryNotOk(String recordId, int responseCode) {
		throwErrorIfCreateNotOk(responseCode, recordId, "binary");
	}

	@Override
	public InputStream readBinary(String recordId) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "GET");
		// httpHandler.setRequestProperty("Accept", "text/plain;charset=utf-8");
		int responseCode = httpHandler.getResponseCode();
		throwErrorIfReadBinaryNotOk(responseCode, recordId);
		return httpHandler.getResponseBinary();

	}

	private void throwErrorIfReadBinaryNotOk(int responseCode, String recordId) {
		throwErrorIfReadNotOk(responseCode, recordId, "binary");
	}

}
