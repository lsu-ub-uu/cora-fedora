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
import java.text.MessageFormat;

import se.uu.ub.cora.fedora.FedoraAdapter;
import se.uu.ub.cora.fedora.FedoraConflictException;
import se.uu.ub.cora.fedora.FedoraException;
import se.uu.ub.cora.fedora.FedoraNotFoundException;
import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;

public class FedoraAdapterImp implements FedoraAdapter {

	private static final String RECORD = "record";
	private static final int NO_CONTENT = 204;
	private static final int OK = 200;
	private static final int CREATED = 201;
	private static final int NOT_FOUND = 404;
	private static final String RECORD_ERROR_MESSAGE = "Error storing record in Fedora, recordId: {0}";
	private static final String RECORD_NOT_FOUND_MESSAGE = "{0} with id: {1} does not exist in Fedora.";
	private static final String RECORD_CONFLICT_MESSAGE = "Record with id: {0} already exists in Fedora.";
	private static final String BINARY_ERROR_MESSAGE = "Error storing binary in Fedora, recordId: {0}";
	private HttpHandlerFactory httpHandlerFactory;
	private String baseUrl;

	public FedoraAdapterImp(HttpHandlerFactory httpHandlerFactory, String baseUrl) {
		this.httpHandlerFactory = httpHandlerFactory;
		this.baseUrl = baseUrl;
	}

	@Override
	public void createRecord(String recordId, String fedoraXML) {
		checkRecordNotExists(recordId);
		storeRecord(recordId, fedoraXML);
	}

	private void checkRecordNotExists(String recordId) {
		int headResponseCode = readHeadForRecord(recordId);
		if (headResponseCode == OK) {
			throw FedoraConflictException
					.withMessage(MessageFormat.format(RECORD_CONFLICT_MESSAGE, recordId));
		}
		if (headResponseCode != NOT_FOUND) {
			throw FedoraException.withMessage(createRecordStoreErrorMessage(recordId));
		}
	}

	private String createRecordStoreErrorMessage(String recordId) {
		return MessageFormat.format(RECORD_ERROR_MESSAGE, recordId);
	}

	private int readHeadForRecord(String recordId) {
		try {
			HttpHandler httpHandlerHead = factorHttpHandler(recordId, "HEAD");
			return callFedora(httpHandlerHead);
		} catch (Exception e) {
			throw FedoraException.withMessageAndException(createRecordStoreErrorMessage(recordId),
					e);
		}
	}

	private void storeRecord(String recordId, String fedoraXML) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStore(recordId, fedoraXML);
			int responseCode = callFedora(httpHandler);
			throwErrorIfCreateNotOk(responseCode, recordId);
		} catch (Exception e) {
			throw FedoraException.withMessageAndException(createRecordStoreErrorMessage(recordId),
					e);
		}
	}

	private void throwErrorIfCreateNotOk(int responseCode, String recordId) {
		if (responseCode != CREATED) {
			throw FedoraException.withMessage(createRecordStoreErrorMessage(recordId));
		}
	}

	private HttpHandler setupHttpHandlerForStore(String recordId, String fedoraXML) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "PUT");
		httpHandler.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
		httpHandler.setOutput(fedoraXML);
		return httpHandler;
	}

	@Override
	public String readRecord(String recordId) {
		HttpHandler httpHandler = setUpHttpHandlerForRead(recordId);
		int responseCode = callFedora(httpHandler);
		throwErrorIfReadNotOk(responseCode, recordId, RECORD);
		return httpHandler.getResponseText();
	}

	private void throwErrorIfReadNotOk(int responseCode, String recordId, String type) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(
					MessageFormat.format(RECORD_NOT_FOUND_MESSAGE, capitalize(type), recordId));
		}
		if (responseCode != OK) {
			throw FedoraException
					.withMessage("Error reading " + type + " from Fedora, recordId: " + recordId);
		}
	}

	private String capitalize(String string) {
		return string.substring(0, 1).toUpperCase() + string.substring(1);
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
	public void updateRecord(String recordId, String fedoraXML) {
		throwErrorIfRecordDoesNotExist(recordId);
		int responseCode = updateRecordInFedora(recordId, fedoraXML);
		throwErrorIfUpdateFailed(responseCode, recordId);
	}

	private void throwErrorIfRecordDoesNotExist(String recordId) {
		int headResponseCode = readHeadForRecord(recordId);
		if (headResponseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(
					MessageFormat.format(RECORD_NOT_FOUND_MESSAGE, "Record", recordId));
		}
		if (headResponseCode != OK) {
			throw FedoraException.withMessage(MessageFormat.format(RECORD_ERROR_MESSAGE, recordId));
		}
	}

	private int updateRecordInFedora(String recordId, String fedoraXML) {
		HttpHandler httpHandler = setupHttpHandlerForStore(recordId, fedoraXML);
		return callFedora(httpHandler);
	}

	private int callFedora(HttpHandler httpHandler) {
		return httpHandler.getResponseCode();
	}

	private void throwErrorIfUpdateFailed(int responseCode, String recordId) {
		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(createRecordStoreErrorMessage(recordId));
		}
	}

	@Override
	public void createBinary(String recordId, InputStream binary, String contentType) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "PUT");
		httpHandler.setRequestProperty("Content-Type", contentType);
		httpHandler.setStreamOutput(binary);

		int responseCode = callFedora(httpHandler);
		if (responseCode != CREATED) {
			throw FedoraException.withMessage(createBinaryStoreErrorMessage(recordId));
		}
	}

	private String createBinaryStoreErrorMessage(String recordId) {
		return MessageFormat.format(BINARY_ERROR_MESSAGE, recordId);
	}

	@Override
	public InputStream readBinary(String recordId) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "GET");
		int responseCode = httpHandler.getResponseCode();
		throwErrorIfReadBinaryNotOk(responseCode, recordId);
		return httpHandler.getResponseBinary();
	}

	private void throwErrorIfReadBinaryNotOk(int responseCode, String recordId) {
		throwErrorIfReadNotOk(responseCode, recordId, "binary");
	}

	public String onlyForTestGetBaseUrl() {
		return baseUrl;
	}

	public HttpHandlerFactory onlyForTestGetHttpHandlerFactory() {
		return httpHandlerFactory;
	}
}
