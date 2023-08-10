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

	private static final int OK = 200;
	private static final int CREATED = 201;
	private static final int NO_CONTENT = 204;
	private static final int NOT_FOUND = 404;

	private static final String TEXT_PLAIN_UTF_8 = "text/plain;charset=utf-8";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String TOMBSTONE = "/fcr:tombstone";
	private static final String RECORD = "record";

	private static final String RECORD_ERROR_MESSAGE = "Error storing record in Fedora, recordId: {0}";
	private static final String RECORD_NOT_FOUND_MESSAGE = "{0} with id: {1} does not exist in Fedora.";
	private static final String RECORD_CONFLICT_MESSAGE = "Record with id: {0} already exists in Fedora.";
	private static final String RESOURCE_ERROR_MESSAGE = "Error storing resource in Fedora, recordId: {0}";
	private static final String RESOURCE_UPDATE_ERROR_MESSAGE = "Error updating resource in Fedora, resourceId: {0}";
	private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource with id: {0} does not exist in Fedora.";
	private static final String DELETE_NOT_FOUND_MESSAGE = "Unable to delete record or resource from fedora. Resource not found with id: {0}";
	private static final String DELETE_ERROR_MESSAGE = "Error deleting record or resource in Fedora, id: {0}";
	private static final String READ_ERROR_MESSAGE = "Error reading {0} from Fedora, {0}Id: {1}";

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

	private int readHeadForRecord(String recordId) {
		try {
			HttpHandler httpHandlerHead = factorHttpHandler(recordId, "HEAD");
			return callFedora(httpHandlerHead);
		} catch (Exception e) {
			throw FedoraException.withMessageAndException(createRecordStoreErrorMessage(recordId),
					e);
		}
	}

	private HttpHandler factorHttpHandler(String recordId, String requestMethod) {
		HttpHandler httpHandler = httpHandlerFactory.factor(baseUrl + recordId);
		httpHandler.setRequestMethod(requestMethod);
		return httpHandler;
	}

	private int callFedora(HttpHandler httpHandler) {
		return httpHandler.getResponseCode();
	}

	private String createRecordStoreErrorMessage(String recordId) {
		return MessageFormat.format(RECORD_ERROR_MESSAGE, recordId);
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
		httpHandler.setRequestProperty(CONTENT_TYPE, TEXT_PLAIN_UTF_8);
		httpHandler.setOutput(fedoraXML);
		return httpHandler;
	}

	@Override
	public void createResource(String resourceId, InputStream resource, String contentType) {
		HttpHandler httpHandler = httpPutResource(resourceId, resource, contentType);

		int responseCode = callFedora(httpHandler);
		if (responseCode != CREATED) {
			throw FedoraException.withMessage(createResourceStoreErrorMessage(resourceId));
		}
	}

	private HttpHandler httpPutResource(String recordId, InputStream resource, String mimeType) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "PUT");
		httpHandler.setRequestProperty(CONTENT_TYPE, mimeType);
		httpHandler.setStreamOutput(resource);
		return httpHandler;
	}

	private String createResourceStoreErrorMessage(String resourceId) {
		return MessageFormat.format(RESOURCE_ERROR_MESSAGE, resourceId);
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
					.withMessage(MessageFormat.format(READ_ERROR_MESSAGE, type, recordId));
		}
	}

	private String capitalize(String string) {
		return string.substring(0, 1).toUpperCase() + string.substring(1);
	}

	private HttpHandler setUpHttpHandlerForRead(String recordId) {
		HttpHandler httpHandler = factorHttpHandler(recordId, "GET");
		httpHandler.setRequestProperty("Accept", TEXT_PLAIN_UTF_8);
		return httpHandler;
	}

	@Override
	public InputStream readResource(String resourceId) {
		HttpHandler httpHandler = factorHttpHandler(resourceId, "GET");
		int responseCode = httpHandler.getResponseCode();
		throwErrorIfReadResourceNotOk(responseCode, resourceId);
		return httpHandler.getResponseBinary();
	}

	private void throwErrorIfReadResourceNotOk(int responseCode, String recordId) {
		throwErrorIfReadNotOk(responseCode, recordId, "resource");
	}

	public String onlyForTestGetBaseUrl() {
		return baseUrl;
	}

	public HttpHandlerFactory onlyForTestGetHttpHandlerFactory() {
		return httpHandlerFactory;
	}

	@Override
	public void updateRecord(String recordId, String fedoraXML) {
		ensureRecordExists(recordId);
		updateRecordInFedora(recordId, fedoraXML);
	}

	private void updateRecordInFedora(String recordId, String fedoraXML) {
		HttpHandler httpHandler = setupHttpHandlerForStore(recordId, fedoraXML);
		int responseCode = callFedora(httpHandler);
		throwErrorIfUpdateFailed(responseCode, recordId);
	}

	private void ensureRecordExists(String recordId) {
		int headResponseCode = readHeadForRecord(recordId);
		throwErrorIfRecordDoesNotExist(recordId, headResponseCode);
	}

	private void throwErrorIfRecordDoesNotExist(String recordId, int headResponseCode) {
		if (headResponseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(
					MessageFormat.format(RECORD_NOT_FOUND_MESSAGE, "Record", recordId));
		}
		if (headResponseCode != OK) {
			throw FedoraException.withMessage(MessageFormat.format(RECORD_ERROR_MESSAGE, recordId));
		}
	}

	private void throwErrorIfUpdateFailed(int responseCode, String recordId) {
		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(createRecordStoreErrorMessage(recordId));
		}
	}

	@Override
	public void updateResource(String resourceId, InputStream resource, String mimeType) {
		ensureResourceExists(resourceId);
		updateResourceInFedora(resourceId, resource, mimeType);

	}

	private void updateResourceInFedora(String resourceId, InputStream resource, String mimeType) {
		HttpHandler httpHandler = httpPutResource(resourceId, resource, mimeType);
		int responseCode = callFedora(httpHandler);
		throwExceptionIfErrorOnUpdateResource(resourceId, responseCode);
	}

	private void ensureResourceExists(String resourceId) {
		int headResponseCode = readHeadForRecord(resourceId);
		throwErrorIfResourceIfErrorInFedora(resourceId, headResponseCode);
	}

	private void throwErrorIfResourceIfErrorInFedora(String resourceId, int responseCode) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException
					.withMessage(MessageFormat.format(RESOURCE_NOT_FOUND_MESSAGE, resourceId));
		}
		if (responseCode != OK) {
			throw FedoraException
					.withMessage(MessageFormat.format(RESOURCE_UPDATE_ERROR_MESSAGE, resourceId));
		}
	}

	private void throwExceptionIfErrorOnUpdateResource(String resourceId, int responseCode) {
		if (responseCode != NO_CONTENT) {
			throw FedoraException
					.withMessage(MessageFormat.format(RESOURCE_UPDATE_ERROR_MESSAGE, resourceId));
		}
	}

	@Override
	public void delete(String id) {
		deleteInFedora(id);
		purgeInFedora(id);
	}

	private void purgeInFedora(String id) {
		HttpHandler httpHandler = factorHttpHandler(id + TOMBSTONE, "DELETE");
		int responseCode = httpHandler.getResponseCode();

		throwExceptionIfDeleteNotOk(responseCode, id);
	}

	private void deleteInFedora(String id) {
		HttpHandler httpHandler = factorHttpHandler(id, "DELETE");
		int responseCode = httpHandler.getResponseCode();

		throwExceptionIfDeleteNotOk(responseCode, id);
	}

	private void throwExceptionIfDeleteNotOk(int responseCode, String recordId) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException
					.withMessage(MessageFormat.format(DELETE_NOT_FOUND_MESSAGE, recordId));
		}

		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(MessageFormat.format(DELETE_ERROR_MESSAGE, recordId));
		}
	}
}
