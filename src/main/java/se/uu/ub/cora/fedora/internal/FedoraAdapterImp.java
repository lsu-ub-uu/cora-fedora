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
import se.uu.ub.cora.fedora.record.ResourceMetadata;
import se.uu.ub.cora.fedora.record.ResourceMetadataToUpdate;
import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;

public class FedoraAdapterImp implements FedoraAdapter {

	private static final String DELETE = "DELETE";
	private static final String GET = "GET";
	private static final String PUT = "PUT";
	private static final String HEAD = "HEAD";
	private static final String PATCH = "PATCH";

	private static final int OK = 200;
	private static final int CREATED = 201;
	private static final int NO_CONTENT = 204;
	private static final int NOT_FOUND = 404;

	private static final String ACCEPT = "Accept";
	private static final String CONTENT_TYPE = "Content-Type";

	private static final String MIME_TYPE_TEXT_PLAIN_UTF_8 = "text/plain;charset=utf-8";
	private static final String FCR_TOMBSTONE = "/fcr:tombstone";
	private static final String FCR_METADATA = "/fcr:metadata";
	private static final String RECORD = "record";
	private static final String RESOURCE = "resource";
	private static final String RESPONSE_CODE = "responseCode";
	private static final String RESPONSE_BODY = "responseBody";

	private static final String CREATING = "creating";
	private static final String READING = "reading";
	private static final String READING_METADATA = "reading metadata";
	private static final String UPDATING_METADATA = "updating metadata for";
	private static final String UPDATING = "updating";
	private static final String DELETING = "deleting";

	private static final String ERR_MSG_INTERNAL_ERROR = "Error {0} a {1}. An internal "
			+ "error has been thrown for {1} id {2}.";
	private static final String ERR_MSG_FEDORA_ERROR = "Error {0} in Fedora: {1} id {2} "
			+ "failed due to error {3} returned from Fedora";
	private static final String ERR_MSG_CREATE_CONFLICT = "Error creating in Fedora:: {1} with id {0} "
			+ "already exists in Fedora.";
	private static final String ERR_MSG_NOT_FOUND_IN_FEDORA = "Error {0} in Fedora: {1} id "
			+ "{2} was not found in Fedora.";

	private HttpHandlerFactory httpHandlerFactory;
	private String baseUrl;
	private ResourceMetadataParser resourceMetadataParser;

	public FedoraAdapterImp(HttpHandlerFactory httpHandlerFactory, String baseUrl,
			ResourceMetadataParser resourceMetadataParser) {
		this.httpHandlerFactory = httpHandlerFactory;
		this.baseUrl = baseUrl;
		this.resourceMetadataParser = resourceMetadataParser;
	}

	@Override
	public void createRecord(String dataDivider, String recordId, String fedoraXML) {
		String path = assemblePathForRecord(dataDivider, recordId);
		ensureRecordNotExists(path, recordId);
		createRecordInFedora(path, recordId, fedoraXML);
	}

	private void ensureRecordNotExists(String path, String recordId) {
		int headResponseCode = readObjectFromFedora(path, recordId, RECORD, CREATING);
		thorwIfObjectsExistsOrAnyOtherError(recordId, headResponseCode, RECORD);
	}

	private void thorwIfObjectsExistsOrAnyOtherError(String recordId, int responseCode,
			String typeOfRecord) {
		if (responseCode == OK) {
			throw FedoraConflictException.withMessage(
					MessageFormat.format(ERR_MSG_CREATE_CONFLICT, recordId, typeOfRecord));
		}
		if (responseCode != NOT_FOUND) {
			throw FedoraException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR, CREATING,
					recordId, typeOfRecord, responseCode));
		}
	}

	private int readObjectFromFedora(String path, String recordId, String typeOfRecord,
			String typeOfAction) {
		try {
			HttpHandler httpHandlerHead = factorHttpHandler(path, HEAD);
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

	private String assemblePathForRecord(String dataDivider, String recordId) {
		return baseUrl + dataDivider + ":" + recordId;
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
			throw createFedoraException(recordId, e, RECORD, CREATING);
		}
	}

	private FedoraException createFedoraException(String id, Exception e, String typeOfRecord,
			String typeOfError) {
		String formatErrorMessage = MessageFormat.format(ERR_MSG_INTERNAL_ERROR, typeOfError,
				typeOfRecord, id);
		return FedoraException.withMessageAndException(formatErrorMessage, e);
	}

	private void throwErrorIfCreateNotOk(int responseCode, String recordId, String typeOfRecord) {
		if (responseCode != CREATED) {
			throw FedoraException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR, CREATING,
					recordId, typeOfRecord, responseCode));
		}
	}

	private HttpHandler setupHttpHandlerForStoreRecord(String path, String fedoraXML) {

		HttpHandler httpHandler = factorHttpHandler(path, PUT);
		httpHandler.setRequestProperty(CONTENT_TYPE, MIME_TYPE_TEXT_PLAIN_UTF_8);
		httpHandler.setOutput(fedoraXML);
		return httpHandler;
	}

	@Override
	public void createResource(String dataDivider, String resourceId, InputStream resource,
			String contentType) {
		String path = assemblePathForRecord(dataDivider, resourceId);
		ensureResourceNotExists(path, resourceId);
		int responseCode = callFedoraToStoreResource(path, resourceId, resource, contentType);
		throwErrorIfCreateNotOk(responseCode, resourceId, RESOURCE);
	}

	private void ensureResourceNotExists(String path, String resourceId) {
		int responseCode = readObjectFromFedora(path, resourceId, RESOURCE, CREATING);
		thorwIfObjectsExistsOrAnyOtherError(resourceId, responseCode, RESOURCE);
	}

	private int callFedoraToStoreResource(String path, String resourceId, InputStream resource,
			String contentType) {
		try {
			HttpHandler httpHandler = setupHttpHandlerForStoreResource(path, resource, contentType);
			return httpHandler.getResponseCode();
		} catch (Exception e) {
			throw createFedoraException(resourceId, e, RESOURCE, CREATING);
		}
	}

	private HttpHandler setupHttpHandlerForStoreResource(String path, InputStream resource,
			String mimeType) {
		HttpHandler httpHandler = factorHttpHandler(path, PUT);
		httpHandler.setRequestProperty(CONTENT_TYPE, mimeType);
		httpHandler.setStreamOutput(resource);
		return httpHandler;
	}

	@Override
	public String readRecord(String dataDivider, String recordId) {
		String path = assemblePathForRecord(dataDivider, recordId);
		Map<String, Object> response = callFedoraReadRecord(path, recordId);
		int responseCode = (int) response.get(RESPONSE_CODE);
		throwErrorIfNotOk(responseCode, recordId, RECORD, READING);
		return (String) response.get("responseText");
	}

	private Map<String, Object> callFedoraReadRecord(String path, String recordId) {
		try {
			HttpHandler httpHandler = setUpHttpHandlerForRead(path);
			return createResponseForRecord(httpHandler);
		} catch (Exception e) {
			throw createFedoraException(recordId, e, RECORD, READING);
		}
	}

	private Map<String, Object> createResponseForRecord(HttpHandler httpHandler) {
		Map<String, Object> response = new HashMap<>();
		response.put(RESPONSE_CODE, httpHandler.getResponseCode());
		response.put("responseText", httpHandler.getResponseText());
		return response;
	}

	private void throwErrorIfNotOk(int responseCode, String recordId, String typeOfRecord,
			String action) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(MessageFormat
					.format(ERR_MSG_NOT_FOUND_IN_FEDORA, action, typeOfRecord, recordId));
		}
		if (responseCode != OK) {
			throw FedoraException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR, READING,
					recordId, typeOfRecord, responseCode));
		}
	}

	private HttpHandler setUpHttpHandlerForRead(String path) {
		HttpHandler httpHandler = factorHttpHandler(path, GET);
		httpHandler.setRequestProperty(ACCEPT, MIME_TYPE_TEXT_PLAIN_UTF_8);
		return httpHandler;
	}

	private HttpHandler setUpHttpHandlerForReadResource(String path) {
		return factorHttpHandler(path, GET);
	}

	@Override
	public InputStream readResource(String dataDivider, String resourceId) {
		String path = assemblePathForRecord(dataDivider, resourceId);
		Map<String, Object> response = callFedoraReadResource(path, resourceId);
		int responseCode = (int) response.get(RESPONSE_CODE);
		throwErrorIfNotOk(responseCode, resourceId, RESOURCE, READING);
		return (InputStream) response.get(RESPONSE_BODY);
	}

	private Map<String, Object> callFedoraReadResource(String path, String recordId) {
		try {
			HttpHandler httpHandler = setUpHttpHandlerForReadResource(path);
			return createResponseForResource(httpHandler);
		} catch (Exception e) {
			throw createFedoraException(recordId, e, RESOURCE, READING);
		}
	}

	private Map<String, Object> createResponseForResource(HttpHandler httpHandler) {
		Map<String, Object> response = new HashMap<>();
		response.put(RESPONSE_CODE, httpHandler.getResponseCode());
		response.put(RESPONSE_BODY, httpHandler.getResponseBinary());
		return response;
	}

	@Override
	public ResourceMetadata readResourceMetadata(String dataDivider, String resourceId) {
		String path = assemblePathForRecordMetadata(dataDivider, resourceId);
		Map<String, Object> response = callFedoraReadResourceMetadata(path, resourceId);
		int responseCode = (int) response.get(RESPONSE_CODE);
		throwErrorIfNotOk(responseCode, resourceId, RESOURCE, READING_METADATA);
		return (ResourceMetadata) response.get(RESPONSE_BODY);
	}

	private String assemblePathForRecordMetadata(String dataDivider, String recordId) {
		return assemblePathForRecord(dataDivider, recordId) + FCR_METADATA;
	}

	private Map<String, Object> callFedoraReadResourceMetadata(String path, String resourceId) {
		try {
			HttpHandler httpHandler = setUpHttpHandlerForReadResourceMetadata(path);
			return getResponseCallReadResourceMetadata(httpHandler);
		} catch (Exception e) {
			throw createFedoraException(resourceId, e, RESOURCE, READING_METADATA);
		}
	}

	private Map<String, Object> getResponseCallReadResourceMetadata(HttpHandler httpHandler) {
		ResourceMetadata resourceMetadata = extractResourceMetadataFromJson(httpHandler);
		int responseCode = httpHandler.getResponseCode();
		return buildResponse(resourceMetadata, responseCode);
	}

	private Map<String, Object> buildResponse(ResourceMetadata resourceMetadata, int responseCode) {
		HashMap<String, Object> response = new HashMap<>();
		response.put(RESPONSE_CODE, responseCode);
		response.put(RESPONSE_BODY, resourceMetadata);
		return response;
	}

	private ResourceMetadata extractResourceMetadataFromJson(HttpHandler httpHandler) {
		String jsonString = httpHandler.getResponseText();
		return resourceMetadataParser.parse(jsonString);
	}

	private HttpHandler setUpHttpHandlerForReadResourceMetadata(String path) {
		HttpHandler httpHandler = factorHttpHandler(path, GET);
		httpHandler.setRequestProperty(ACCEPT, "application/ld+json");
		return httpHandler;
	}

	@Override
	public void updateRecord(String dataDivider, String recordId, String fedoraXML) {
		String path = assemblePathForRecord(dataDivider, recordId);
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
			throw FedoraNotFoundException.withMessage(MessageFormat
					.format(ERR_MSG_NOT_FOUND_IN_FEDORA, UPDATING, typeOfRecord, recordId));
		}
		if (responseCode != OK) {
			throw FedoraException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR, UPDATING,
					recordId, typeOfRecord, responseCode));
		}
	}

	private void throwErrorIfUpdateFailed(int responseCode, String recordId, String typeOfRecord) {
		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR, UPDATING,
					recordId, typeOfRecord, responseCode));
		}
	}

	@Override
	public void updateResourceMetadata(String dataDivider, String resourceId,
			ResourceMetadataToUpdate resourceMetadataToUpdate) {
		tryToUpdateResourceMetadata(dataDivider, resourceId, resourceMetadataToUpdate);
	}

	private void tryToUpdateResourceMetadata(String dataDivider, String resourceId,
			ResourceMetadataToUpdate resourceMetadataToUpdate) {
		int responseCode = callFedoraForUpdateResourceMetadata(dataDivider, resourceId,
				resourceMetadataToUpdate);
		throwExceptionForUpdateResourceMetadataIfNotOk(responseCode, resourceId);
	}

	private int callFedoraForUpdateResourceMetadata(String dataDivider, String resourceId,
			ResourceMetadataToUpdate resourceMetadataToUpdate) {
		try {
			return setUpHttpHandlerForUpdateResourcesMetadata(dataDivider, resourceId,
					resourceMetadataToUpdate);
		} catch (Exception e) {
			throw createFedoraException(resourceId, e, RESOURCE, UPDATING_METADATA);
		}
	}

	private int setUpHttpHandlerForUpdateResourcesMetadata(String dataDivider, String resourceId,
			ResourceMetadataToUpdate resourceMetadataToUpdate) {
		HttpHandler httpHandler = setHttpHandlerForPatch(dataDivider, resourceId);
		String body = createBodyForUpdateResourceMetadata(resourceMetadataToUpdate);
		httpHandler.setOutput(body);
		return httpHandler.getResponseCode();
	}

	private HttpHandler setHttpHandlerForPatch(String dataDivider, String resourceId) {
		String path = assemblePathForRecordMetadata(dataDivider, resourceId);
		HttpHandler httpHandler = factorHttpHandler(path, PATCH);
		httpHandler.setRequestProperty(CONTENT_TYPE, "application/sparql-update");
		return httpHandler;
	}

	private String createBodyForUpdateResourceMetadata(
			ResourceMetadataToUpdate resourceMetadataToUpdate) {
		String updateResponseBody = """
				PREFIX ebucore: <http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#>
				INSERT '{'<> ebucore:filename "{0}" . <> ebucore:hasMimeType "{1}" .'}'
				WHERE '{}'
				""";
		return MessageFormat.format(updateResponseBody, resourceMetadataToUpdate.originalFileName(),
				resourceMetadataToUpdate.mimeType());
	}

	private void throwExceptionForUpdateResourceMetadataIfNotOk(int responseCode,
			String resourceId) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR,
					UPDATING_METADATA, resourceId, RESOURCE, responseCode));
		}
		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR,
					UPDATING_METADATA, resourceId, RESOURCE, responseCode));
		}
	}

	@Override
	public void updateResource(String dataDivider, String resourceId, InputStream resource,
			String mimeType) {
		String path = assemblePathForRecord(dataDivider, resourceId);
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
		String path = assemblePathForRecord(dataDivider, recordId);
		deleteRecordInFedora(path, recordId);
		purgeRecordInFedora(path, recordId);
	}

	private void deleteRecordInFedora(String path, String id) {
		callFedoraForDelete(path, id, RECORD);
	}

	private void purgeRecordInFedora(String path, String id) {
		callFedoraForDelete(path + FCR_TOMBSTONE, id, RECORD);
	}

	private void callFedoraForDelete(String path, String id, String typeOfRecord) {
		HttpHandler httpHandler = factorHttpHandler(path, DELETE);
		int responseCode = httpHandler.getResponseCode();
		throwExceptionIfDeleteNotOk(responseCode, id, typeOfRecord);
	}

	private void throwExceptionIfDeleteNotOk(int responseCode, String id, String typeOfRecord) {
		if (responseCode == NOT_FOUND) {
			throw FedoraNotFoundException.withMessage(
					MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA, DELETING, typeOfRecord, id));
		}
		if (responseCode != NO_CONTENT) {
			throw FedoraException.withMessage(MessageFormat.format(ERR_MSG_FEDORA_ERROR, DELETING,
					id, typeOfRecord, responseCode));
		}
	}

	@Override
	public void deleteResource(String dataDivider, String resourceId) {
		String path = assemblePathForRecord(dataDivider, resourceId);
		deleteResourceInFedora(path, resourceId);
		purgeResourceInFedora(path, resourceId);
	}

	private void deleteResourceInFedora(String path, String id) {
		callFedoraForDelete(path, id, RESOURCE);
	}

	private void purgeResourceInFedora(String path, String id) {
		callFedoraForDelete(path + FCR_TOMBSTONE, id, RESOURCE);
	}

	public String onlyForTestGetBaseUrl() {
		return baseUrl;
	}

	public HttpHandlerFactory onlyForTestGetHttpHandlerFactory() {
		return httpHandlerFactory;
	}

	public ResourceMetadataParser onlyForTestGetResourceMetadataParser() {
		return resourceMetadataParser;
	}

}
