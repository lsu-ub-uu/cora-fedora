/*
 * Copyright 2022, 2023 Uppsala University Library
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
import java.util.HashMap;
import java.util.Map;

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

	private static final String MIME_TYPE_TEXT_PLAIN_UTF_8 = "text/plain;charset=utf-8";
	private static final String MIME_TYPE_OCTET_STREAM = "application/octet-stream";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String TOMBSTONE = "/fcr:tombstone";
	private static final String RECORD = "record";
	private static final String RESOURCE = "resource";
	private static final String RESPONSE_CODE = "responseCode";
	private static final String RESPONSE_BINARY = "responseBinary";
	private static final String RECORD_FOLDER = RECORD + "/";
	private static final String RESOURCE_FOLDER = RESOURCE + "/";
	private static final String CREATION = "Creation";
	private static final String UPDATING = "Updating";

	private static final String INTERNAL_ERROR_MESSAGE = "{2} error: an internal "
			+ "error has been thrown for {1} id {0}.";
	private static final String CREATE_CONFLICT_MESSAGE = "Creation error: {1} with id {0} "
			+ "already exists in Fedora.";
	private static final String CREATE_ERROR_MESSAGE = "Creation error: {2} id {0} could not be "
			+ "created due to error {1} returned from Fedora";
	private static final String READ_NOT_FOUND_MESSAGE = "Reading error: The {1} "
			+ "could not be read from Fedora. No {1} was found with the id {0}";
	private static final String READ_ERROR_MESSAGE = "Reading error: {2} id {0} could not be read"
			+ " due to error {1} returned from Fedora";
	private static final String UPDATE_NOT_FOUND_MESSAGE = "Updating error: The {1} "
			+ "could not be updated in Fedora. No {1} was found with the id {0}";
	private static final String UPDATE_ERROR_MESSAGE = "Updating error: {2} id {0} could not be "
			+ "updated due to error {1} returned from Fedora";
	private static final String DELETE_NOT_FOUND_MESSAGE = "Deletion Error: The resource "
			+ "could not be removed from Fedora. No resource was found with the id {0}";
	private static final String DELETE_ERROR_MESSAGE = "Deletion Error: {2} id {0} could not be "
			+ "deleted due to error {1} returned from Fedora";

	private HttpHandlerFactory httpHandlerFactory;
	private String baseUrl;

	public FedoraAdapterImp(HttpHandlerFactory httpHandlerFactory, String baseUrl) {
		this.httpHandlerFactory = httpHandlerFactory;
		this.baseUrl = baseUrl;
	}

	@Override
	public void createRecord(String dataDivider, String recordId, String fedoraXML) {
		String path = ensemblePathForRecord(dataDivider, recordId);
		ensureRecordNotExists(path, recordId);
		createRecordInFedora(path, recordId, fedoraXML);
	}

	private void ensureRecordNotExists(String path, String recordId) {
		int headResponseCode = readObjectFromFedora(path, recordId, RECORD, CREATION);
		thorwIfObjectsExistsOrAnyOtherError(recordId, headResponseCode, RECORD);
	}

	private void thorwIfObjectsExistsOrAnyOtherError(String recordId, int responseCode,
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

	private int readObjectFromFedora(String path, String recordId, String typeOfRecord,
			String typeOfAction) {
		try {
			HttpHandler httpHandlerHead = factorHttpHandler(path, "HEAD");
			return httpHandlerHead.getResponseCode();
		} catch (Exception e) {
			throw createFedoraException(recordId, e, typeOfRecord, typeOfAction);
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

	private void createRecordInFedora(String path, String recordId, String fedoraXML) {
		int responseCode = callFedoraStoreRecord(path, recordId, fedoraXML);
		throwErrorIfCreateNotOk(responseCode, recordId, RECORD);
	}

	private int callFedoraStoreRecord(String path, String recordId, String fedoraXML) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStoreRecord(path, fedoraXML);
			return httpHandler.getResponseCode();
		} catch (Exception e) {
			throw createFedoraException(recordId, e, RECORD, CREATION);
		}
	}

	private FedoraException createFedoraException(String id, Exception e, String typeOfRecord,
			String typeOfError) {
		String formatErrorMessage = MessageFormat.format(INTERNAL_ERROR_MESSAGE, id, typeOfRecord,
				typeOfError);
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
		httpHandler.setRequestProperty(CONTENT_TYPE, MIME_TYPE_TEXT_PLAIN_UTF_8);
		httpHandler.setOutput(fedoraXML);
		return httpHandler;
	}

	@Override
	public void createResource(String dataDivider, String resourceId, InputStream resource,
			String contentType) {
		String path = ensemblePathForResource(dataDivider, resourceId);
		ensureResourceNotExists(path, resourceId);
		int responseCode = callFedoraToStoreResource(path, resourceId, resource, contentType);
		throwErrorIfCreateNotOk(responseCode, resourceId, RESOURCE);
	}

	private void ensureResourceNotExists(String path, String resourceId) {
		int responseCode = readObjectFromFedora(path, resourceId, RESOURCE, CREATION);
		thorwIfObjectsExistsOrAnyOtherError(resourceId, responseCode, RESOURCE);
	}

	private int callFedoraToStoreResource(String path, String resourceId, InputStream resource,
			String contentType) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStoreResource(path, resource, contentType);
			return httpHandler.getResponseCode();
		} catch (Exception e) {
			throw createFedoraException(resourceId, e, RESOURCE, CREATION);
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
		Map<String, Object> response = callFedoraReadRecord(path, recordId);
		int responseCode = (int) response.get(RESPONSE_CODE);
		throwErrorIfNotOk(responseCode, recordId, RECORD);
		return (String) response.get("responseText");
	}

	private Map<String, Object> callFedoraReadRecord(String path, String recordId) {
		try {
			HttpHandler httpHandler = setUpHttpHandlerForRead(path);
			return createResponseForRecord(httpHandler);
		} catch (Exception e) {
			throw createFedoraException(recordId, e, RECORD, "Reading");
		}
	}

	private Map<String, Object> createResponseForRecord(HttpHandler httpHandler) {
		Map<String, Object> response = new HashMap<>();
		response.put(RESPONSE_CODE, httpHandler.getResponseCode());
		response.put("responseText", httpHandler.getResponseText());
		return response;
	}

	private void throwErrorIfNotOk(int responseCode, String recordId, String typeOfrecord) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(
					MessageFormat.format(READ_NOT_FOUND_MESSAGE, recordId, typeOfrecord));
		}
		if (responseCode != OK) {
			throw FedoraException.withMessage(
					MessageFormat.format(READ_ERROR_MESSAGE, recordId, responseCode, typeOfrecord));
		}
	}

	private HttpHandler setUpHttpHandlerForRead(String path) {
		HttpHandler httpHandler = factorHttpHandler(path, "GET");
		httpHandler.setRequestProperty("Accept", MIME_TYPE_TEXT_PLAIN_UTF_8);
		return httpHandler;
	}

	private HttpHandler setUpHttpHandlerForReadResource(String path) {
		HttpHandler httpHandler = factorHttpHandler(path, "GET");
		httpHandler.setRequestProperty("Accept", MIME_TYPE_OCTET_STREAM);
		return httpHandler;
	}

	@Override
	public InputStream readResource(String dataDivider, String resourceId) {
		String path = ensemblePathForResource(dataDivider, resourceId);
		Map<String, Object> response = callFedoraReadResource(path, resourceId);
		int responseCode = (int) response.get(RESPONSE_CODE);
		throwErrorIfReadResourceNotOk(responseCode, resourceId);
		return (InputStream) response.get(RESPONSE_BINARY);
	}

	private Map<String, Object> callFedoraReadResource(String path, String recordId) {
		try {
			HttpHandler httpHandler = setUpHttpHandlerForReadResource(path);
			return createResponseForResource(httpHandler);
		} catch (Exception e) {
			throw createFedoraException(recordId, e, RESOURCE, "Reading");
		}
	}

	private Map<String, Object> createResponseForResource(HttpHandler httpHandler) {
		Map<String, Object> response = new HashMap<>();
		response.put(RESPONSE_CODE, httpHandler.getResponseCode());
		response.put(RESPONSE_BINARY, httpHandler.getResponseBinary());
		return response;
	}

	private void throwErrorIfReadResourceNotOk(int responseCode, String recordId) {
		throwErrorIfNotOk(responseCode, recordId, RESOURCE);
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
		ensureRecordExistsForUpdate(path, recordId);
		updateRecordInFedora(path, recordId, fedoraXML);
	}

	private void updateRecordInFedora(String path, String recordId, String fedoraXML) {
		int responseCode = callFedoraForRecordUpdate(path, recordId, fedoraXML);
		throwErrorIfUpdateFailed(responseCode, recordId, RECORD);
	}

	private int callFedoraForRecordUpdate(String path, String recordId, String fedoraXML) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStoreRecord(path, fedoraXML);
			return httpHandler.getResponseCode();
		} catch (Exception e) {
			throw createFedoraException(recordId, e, RECORD, UPDATING);
		}
	}

	private void ensureRecordExistsForUpdate(String path, String recordId) {
		int headResponseCode = readObjectFromFedora(path, recordId, RECORD, UPDATING);
		throwErrorIfObjectDoesNotExist(recordId, headResponseCode, RECORD);
	}

	private void throwErrorIfObjectDoesNotExist(String recordId, int responseCode,
			String typeOfRecord) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(
					MessageFormat.format(UPDATE_NOT_FOUND_MESSAGE, recordId, typeOfRecord));
		}
		if (responseCode != OK) {
			throw FedoraException.withMessage(MessageFormat.format(UPDATE_ERROR_MESSAGE, recordId,
					responseCode, typeOfRecord));
		}
	}

	private void throwErrorIfUpdateFailed(int responseCode, String recordId, String typeOfRecord) {
		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(MessageFormat.format(UPDATE_ERROR_MESSAGE, recordId,
					responseCode, typeOfRecord));
		}
	}

	@Override
	public void updateResource(String dataDivider, String resourceId, InputStream resource,
			String mimeType) {
		String path = ensemblePathForResource(dataDivider, resourceId);
		ensureResourceExistsForUpdate(path, resourceId);
		updateResourceInFedora(path, resourceId, resource, mimeType);
	}

	private void updateResourceInFedora(String path, String resourceId, InputStream resource,
			String mimeType) {
		int responseCode = callFedoraForResourceUpdate(path, resourceId, resource, mimeType);
		throwErrorIfUpdateFailed(responseCode, resourceId, RESOURCE);
	}

	private int callFedoraForResourceUpdate(String path, String resourceId, InputStream resource,
			String mimeType) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStoreResource(path, resource, mimeType);
			return httpHandler.getResponseCode();
		} catch (Exception e) {
			throw createFedoraException(resourceId, e, RESOURCE, UPDATING);
		}
	}

	private void ensureResourceExistsForUpdate(String path, String resourceId) {
		int headResponseCode = readObjectFromFedora(path, resourceId, RESOURCE, UPDATING);
		throwErrorIfObjectDoesNotExist(resourceId, headResponseCode, RESOURCE);
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
