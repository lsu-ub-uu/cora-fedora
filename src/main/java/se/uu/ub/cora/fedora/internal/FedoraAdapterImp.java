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

	private static final String CREATE_INTERNAL_ERROR_MESSAGE = "Creation Error: {1} id {0} could not be created due to an internal error.";
	private static final int OK = 200;
	private static final int CREATED = 201;
	private static final int NO_CONTENT = 204;
	private static final int NOT_FOUND = 404;

	private static final String TEXT_PLAIN_UTF_8 = "text/plain;charset=utf-8";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String TOMBSTONE = "/fcr:tombstone";
	private static final String RECORD = "record";
	private static final String RESOURCE = "resource";
	private static final String RECORD_FOLDER = RECORD + "/";
	private static final String RESOURCE_FOLDER = RESOURCE + "/";

	private static final String CREATE_CONFLICT_MESSAGE = "Creation error: {1} with id {0} "
			+ "already exists in Fedora.";
	private static final String CREATE_ERROR_MESSAGE = "Creation Error: {2} id {0} could not be created due to error {1} returned from Fedora";

	private static final String RECORD_ERROR_MESSAGE = "Error storing record in Fedora, recordId: {0}";
	private static final String READ_ERROR_MESSAGE = "Error reading {0} from Fedora, {0}Id: {1}";
	private static final String RECORD_NOT_FOUND_MESSAGE = "{0} with id: {1} does not exist in Fedora.";
	private static final String RESOURCE_ERROR_MESSAGE = "Error storing resource in Fedora, recordId: {0}";
	private static final String RESOURCE_UPDATE_ERROR_MESSAGE = "Error updating resource in Fedora, resourceId: {0}";
	private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource with id: {0} does not exist in Fedora.";
	private static final String DELETE_NOT_FOUND_MESSAGE = "Deletion Error: The resource "
			+ "could not be removed from Fedora. No resource was found with the id {0}";
	private static final String DELETE_ERROR_MESSAGE = "Deletion Error: {2} id {0} could not be deleted due to error {1} returned from Fedora";

	private HttpHandlerFactory httpHandlerFactory;
	private String baseUrl;

	public FedoraAdapterImp(HttpHandlerFactory httpHandlerFactory, String baseUrl) {
		this.httpHandlerFactory = httpHandlerFactory;
		this.baseUrl = baseUrl;
	}

	@Override
	public void createRecord(String dataDivider, String recordId, String fedoraXML) {
		String path = ensemblePathForRecord(dataDivider, recordId);
		checkRecordNotExists(path, recordId);
		storeRecord(path, recordId, fedoraXML);
	}

	private void checkRecordNotExists(String path, String recordId) {
		int headResponseCode = readFromFedora(path, recordId, RECORD);
		thorwIfConflictOrAnyOtherError(recordId, headResponseCode, RECORD);
	}

	private void thorwIfConflictOrAnyOtherError(String recordId, int responseCode,
			String typeOfRecord) {
		if (responseCode == OK) {
			throw FedoraConflictException.withMessage(
					MessageFormat.format(CREATE_CONFLICT_MESSAGE, recordId, typeOfRecord));
		}
		if (responseCode != NOT_FOUND) {
			throw FedoraException.withMessage(MessageFormat.format(CREATE_ERROR_MESSAGE, recordId,
					responseCode, typeOfRecord));
		}
	}

	private int readFromFedora(String path, String recordId, String typeOfRecord) {
		try {
			HttpHandler httpHandlerHead = factorHttpHandler(path, "HEAD");
			return httpHandlerHead.getResponseCode();
		} catch (Exception e) {
			throw FedoraException.withMessageAndException(
					MessageFormat.format(CREATE_INTERNAL_ERROR_MESSAGE, recordId, typeOfRecord), e);
		}
	}

	private HttpHandler factorHttpHandler(String path, String requestMethod) {
		HttpHandler httpHandler = httpHandlerFactory.factor(path);
		httpHandler.setRequestMethod(requestMethod);
		return httpHandler;
	}

	private String ensemblePathForRecord(String dataDivider, String recordId) {
		return baseUrl + dataDivider + "/" + RECORD_FOLDER + recordId;
	}

	private String ensemblePathForResource(String dataDivider, String recordId) {
		return baseUrl + dataDivider + "/" + RESOURCE_FOLDER + recordId;
	}

	private void storeRecord(String path, String recordId, String fedoraXML) {
		int responseCode = callFedoraStoreRecord(path, recordId, fedoraXML);
		throwErrorIfCreateNotOk(responseCode, recordId, RECORD);
	}

	private int callFedoraStoreRecord(String path, String recordId, String fedoraXML) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStoreRecord(path, fedoraXML);
			return httpHandler.getResponseCode();
		} catch (Exception e) {
			throw createFedoraExceptionForInternalError(recordId, e, RECORD);
		}
	}

	private FedoraException createFedoraExceptionForInternalError(String id, Exception e,
			String typeOfRecord) {
		String formatErrorMessage = MessageFormat.format(CREATE_INTERNAL_ERROR_MESSAGE, id,
				typeOfRecord);
		return FedoraException.withMessageAndException(formatErrorMessage, e);
	}

	private void throwErrorIfCreateNotOk(int responseCode, String recordId, String typeOfRecord) {
		if (responseCode != CREATED) {
			throw FedoraException.withMessage(MessageFormat.format(CREATE_ERROR_MESSAGE, recordId,
					responseCode, typeOfRecord));
		}
	}

	private HttpHandler setupHttpHandlerForStoreRecord(String path, String fedoraXML) {

		HttpHandler httpHandler = factorHttpHandler(path, "PUT");
		httpHandler.setRequestProperty(CONTENT_TYPE, TEXT_PLAIN_UTF_8);
		httpHandler.setOutput(fedoraXML);
		return httpHandler;
	}

	@Override
	public void createResource(String dataDivider, String resourceId, InputStream resource,
			String contentType) {
		String path = ensemblePathForResource(dataDivider, resourceId);
		checkResourceNotExists(path, resourceId);
		int responseCode = callFedoraToStoreResource(path, resourceId, resource, contentType);
		throwErrorIfCreateNotOk(responseCode, resourceId, RESOURCE);
	}

	private void checkResourceNotExists(String path, String resourceId) {
		int responseCode = readFromFedora(path, resourceId, RESOURCE);
		thorwIfConflictOrAnyOtherError(resourceId, responseCode, RESOURCE);
	}

	private int callFedoraToStoreResource(String path, String resourceId, InputStream resource,
			String contentType) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStoreResource(path, resource, contentType);
			return httpHandler.getResponseCode();
		} catch (Exception e) {
			throw createFedoraExceptionForInternalError(resourceId, e, RESOURCE);
		}
	}

	private HttpHandler setupHttpHandlerForStoreResource(String path, InputStream resource,
			String mimeType) {
		HttpHandler httpHandler = factorHttpHandler(path, "PUT");
		httpHandler.setRequestProperty(CONTENT_TYPE, mimeType);
		httpHandler.setStreamOutput(resource);
		return httpHandler;
	}

	@Override
	public String readRecord(String dataDivider, String recordId) {
		String path = ensemblePathForRecord(dataDivider, recordId);
		HttpHandler httpHandler = setUpHttpHandlerForRead(path, recordId);
		int responseCode = httpHandler.getResponseCode();
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

	private HttpHandler setUpHttpHandlerForRead(String path, String recordId) {
		HttpHandler httpHandler = factorHttpHandler(path, "GET");
		httpHandler.setRequestProperty("Accept", TEXT_PLAIN_UTF_8);
		return httpHandler;
	}

	@Override
	public InputStream readResource(String dataDivider, String resourceId) {
		String path = ensemblePathForResource(dataDivider, resourceId);
		HttpHandler httpHandler = factorHttpHandler(path, "GET");
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
	public void updateRecord(String dataDivider, String recordId, String fedoraXML) {
		String path = ensemblePathForRecord(dataDivider, recordId);
		ensureRecordExists(path, recordId);
		updateRecordInFedora(path, recordId, fedoraXML);
	}

	private void updateRecordInFedora(String path, String recordId, String fedoraXML) {
		HttpHandler httpHandler = setupHttpHandlerForStoreRecord(path, fedoraXML);
		int responseCode = httpHandler.getResponseCode();
		throwErrorIfUpdateFailed(responseCode, recordId);
	}

	private void ensureRecordExists(String path, String recordId) {
		int headResponseCode = readFromFedora(path, recordId, null);
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
			throw FedoraException.withMessage(MessageFormat.format(RECORD_ERROR_MESSAGE, recordId));
		}
	}

	@Override
	public void updateResource(String dataDivider, String resourceId, InputStream resource,
			String mimeType) {
		String path = ensemblePathForResource(dataDivider, resourceId);
		ensureResourceExists(path, resourceId);
		updateResourceInFedora(path, resourceId, resource, mimeType);
	}

	private void updateResourceInFedora(String path, String resourceId, InputStream resource,
			String mimeType) {
		HttpHandler httpHandler = setupHttpHandlerForStoreResource(path, resource, mimeType);
		int responseCode = httpHandler.getResponseCode();
		throwExceptionIfErrorOnUpdateResource(resourceId, responseCode);
	}

	private void ensureResourceExists(String path, String resourceId) {
		int headResponseCode = readFromFedora(path, resourceId, null);
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
	public void deleteRecord(String dataDivider, String recordId) {
		String path = ensemblePathForRecord(dataDivider, recordId);
		deleteRecordInFedora(path, recordId);
		purgeRecordInFedora(path, recordId);
	}

	private void deleteRecordInFedora(String path, String id) {
		callFedoraForDelete(path, id, RECORD);
	}

	private void purgeRecordInFedora(String path, String id) {
		callFedoraForDelete(path + TOMBSTONE, id, RECORD);
	}

	private void callFedoraForDelete(String path, String id, String typeOfRecord) {
		HttpHandler httpHandler = factorHttpHandler(path, "DELETE");
		int responseCode = httpHandler.getResponseCode();
		throwExceptionIfDeleteNotOk(responseCode, id, typeOfRecord);
	}

	private void throwExceptionIfDeleteNotOk(int responseCode, String resourceId,
			String typeOfRecord) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(
					MessageFormat.format(DELETE_NOT_FOUND_MESSAGE, resourceId, typeOfRecord));
		}
		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(MessageFormat.format(DELETE_ERROR_MESSAGE, resourceId,
					responseCode, typeOfRecord));
		}
	}

	@Override
	public void deleteResource(String dataDivider, String resourceId) {
		String path = ensemblePathForResource(dataDivider, resourceId);
		deleteResourceInFedora(path, resourceId);
		purgeResourceInFedora(path, resourceId);
	}

	private void deleteResourceInFedora(String path, String id) {
		callFedoraForDelete(path, id, RESOURCE);
	}

	private void purgeResourceInFedora(String path, String id) {
		callFedoraForDelete(path + TOMBSTONE, id, RESOURCE);
	}
}
