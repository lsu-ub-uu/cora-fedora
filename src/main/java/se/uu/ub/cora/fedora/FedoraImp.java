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

import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpMultiPartUploader;

public class FedoraImp implements FedoraWrapper {

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
		throwErrorIfCreateNotOk(responseCode, recordId);
	}

	private void throwErrorIfCreateNotOk(int responseCode, String recordId) {
		if (responseCode != 201) {
			throw FedoraException
					.withMessage("Error storing record in Fedora, recordId: " + recordId);
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
		throwErrorIfReadNotOk(responseCode, recordId);
		return httpHandler.getResponseText();
	}

	private void throwErrorIfReadNotOk(int responseCode, String recordId) {
		if (responseCode != 200) {
			throw FedoraException
					.withMessage("Error reading record from Fedora, recordId: " + recordId);
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
	public void createBinary(String recordId, InputStream binary) {
		HttpMultiPartUploader httpHandler = httpHandlerFactory
				.factorHttpMultiPartUploader(baseUrl + recordId);
		httpHandler.setRequestMethod("PUT");

		try {
			httpHandler.addFilePart("file", "someFileName", binary);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int responseCode = httpHandler.getResponseCode();
	}

}
