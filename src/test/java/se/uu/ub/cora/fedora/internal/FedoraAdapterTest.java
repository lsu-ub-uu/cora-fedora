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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.FedoraAdapter;
import se.uu.ub.cora.fedora.FedoraConflictException;
import se.uu.ub.cora.fedora.FedoraException;
import se.uu.ub.cora.fedora.FedoraNotFoundException;
import se.uu.ub.cora.fedora.record.ResourceMetadata;
import se.uu.ub.cora.fedora.record.ResourceMetadataToUpdate;
import se.uu.ub.cora.fedora.spy.ResourceMetadataParserSpy;
import se.uu.ub.cora.testspies.httphandler.HttpHandlerFactorySpy;
import se.uu.ub.cora.testspies.httphandler.HttpHandlerSpy;
import se.uu.ub.cora.testspies.httphandler.InputStreamSpy;

public class FedoraAdapterTest {

	private static final String SOME_RESOURCE_ID = "someResourceId:001";
	private static final String SOME_RECORD_ID = "someRecordId:001";
	private static final String FCR_TOMBSTONE = "/fcr:tombstone";
	private static final String FCR_METADATA = "/fcr:metadata";
	private static final int CREATED = 201;
	private static final int OK = 200;
	private static final int INTERNAL_SERVER_ERROR = 500;
	private static final int NO_CONTENT = 204;
	private static final int NOT_FOUND = 404;

	private String baseUrl = "http://localhost:38088/fcrepo/rest/";
	private String dataDivider = "someDataDivider";
	private String mimeType = "image/jpg";
	String expectedRecordPath = baseUrl + dataDivider + "/record/";
	String expectedResourcePath = baseUrl + dataDivider + "/resource/";
	private String recordXML = "<somexml></somexml>";

	private HttpHandlerFactorySpy httpHandlerFactory;
	private FedoraAdapter fedora;
	private HttpHandlerSpy httpHandlerSpy0;
	private HttpHandlerSpy httpHandlerSpy1;
	private ResourceMetadataParserSpy resourceMetadataParser;
	private InputStreamSpy resource;
	ResourceMetadataToUpdate metadataResourceToUpdate = new ResourceMetadataToUpdate(
			"someOriginalFileName", "someMimeType");

	private static final String RECORD = "record";
	private static final String RESOURCE = "resource";

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

	@BeforeMethod
	public void setUp() {
		httpHandlerFactory = new HttpHandlerFactorySpy();

		httpHandlerSpy0 = new HttpHandlerSpy();
		httpHandlerSpy1 = new HttpHandlerSpy();
		setDefaultValuesForHttpHandlerFactory();
		setTombstoneSpecificValuesHttpHandlerFactory();
		setMetadataSpecificValuesHttpHandlerFactory();

		resourceMetadataParser = new ResourceMetadataParserSpy();
		fedora = new FedoraAdapterImp(httpHandlerFactory, baseUrl, resourceMetadataParser);
		resource = new InputStreamSpy();
	}

	private void setDefaultValuesForHttpHandlerFactory() {
		httpHandlerFactory.MRV.setReturnValues("factor", List.of(httpHandlerSpy0, httpHandlerSpy1),
				expectedRecordPath + SOME_RECORD_ID);
		httpHandlerFactory.MRV.setReturnValues("factor", List.of(httpHandlerSpy0, httpHandlerSpy1),
				expectedResourcePath + SOME_RESOURCE_ID);
	}

	private void setTombstoneSpecificValuesHttpHandlerFactory() {
		httpHandlerFactory.MRV.setSpecificReturnValuesSupplier("factor", () -> httpHandlerSpy1,
				expectedRecordPath + SOME_RECORD_ID + FCR_TOMBSTONE);
		httpHandlerFactory.MRV.setSpecificReturnValuesSupplier("factor", () -> httpHandlerSpy1,
				expectedResourcePath + SOME_RESOURCE_ID + FCR_TOMBSTONE);
	}

	private void setMetadataSpecificValuesHttpHandlerFactory() {
		httpHandlerFactory.MRV.setSpecificReturnValuesSupplier("factor", () -> httpHandlerSpy1,
				expectedRecordPath + SOME_RECORD_ID + FCR_METADATA);
		httpHandlerFactory.MRV.setSpecificReturnValuesSupplier("factor", () -> httpHandlerSpy1,
				expectedResourcePath + SOME_RESOURCE_ID + FCR_METADATA);
	}

	@Test
	public void testOnlyForTest() throws Exception {
		FedoraAdapterImp fedoraImp = (FedoraAdapterImp) fedora;
		assertEquals(fedoraImp.onlyForTestGetBaseUrl(), baseUrl);
		assertEquals(fedoraImp.onlyForTestGetHttpHandlerFactory(), httpHandlerFactory);
		assertEquals(fedoraImp.onlyForTestGetResourceMetadataParser(), resourceMetadataParser);
	}

	@Test
	public void testCreateRecordOk() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(CREATED));

		fedora.createRecord(dataDivider, SOME_RECORD_ID, recordXML);

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "PUT");
		httpHandlerSpy1.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"text/plain;charset=utf-8");
		httpHandlerSpy1.MCR.assertParameters("setOutput", 0, recordXML);
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
	}

	@Test
	public void testCreateRecordDuplicateError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(CREATED));

		try {
			fedora.createRecord(dataDivider, SOME_RECORD_ID, recordXML);
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraConflictException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MSG_CREATE_CONFLICT, SOME_RECORD_ID, RECORD));
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test
	public void testCreateRecordAnyOtherErrorOnConflictCheck() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.createRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, CREATING,
					SOME_RECORD_ID, RECORD, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testCreateRecordOnStoringError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.createRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, CREATING,
					SOME_RECORD_ID, RECORD, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testCreateRecordAnyOtherErrorOnFactor() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				expectedRecordPath + SOME_RECORD_ID);
		try {
			fedora.createRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MSG_INTERNAL_ERROR, CREATING, RECORD, SOME_RECORD_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testCreateRecordAnyOtherErrorAddXmlToHttpHandler() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setAlwaysThrowException("setOutput",
				new RuntimeException("errorFromSpy"));

		try {
			fedora.createRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MSG_INTERNAL_ERROR, CREATING, RECORD, SOME_RECORD_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testCreateResource() throws Exception {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(CREATED));

		fedora.createResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "PUT");
		httpHandlerSpy1.MCR.assertParameters("setRequestProperty", 0, "Content-Type", mimeType);
		httpHandlerSpy1.MCR.assertParameters("setStreamOutput", 0, resource);
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");

	}

	@Test
	public void testCreateResourceConflictErrorCheck() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));

		try {
			fedora.createResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraConflictException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MSG_CREATE_CONFLICT, SOME_RESOURCE_ID, RESOURCE));
		}
	}

	@Test
	public void testCreateResourceConflictCheckFailed() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.createResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, CREATING,
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testCreateResourceErrorOnStoring() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.createResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, CREATING,
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testCreateResourceAnyOtherErrorOnFactor() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				expectedResourcePath + SOME_RESOURCE_ID);
		try {
			fedora.createResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_INTERNAL_ERROR, CREATING,
					RESOURCE, SOME_RESOURCE_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testCreateResourceAnyOtherErrorAddXmlToHttpHandler() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setAlwaysThrowException("setStreamOutput",
				new RuntimeException("errorFromSpy"));

		try {
			fedora.createResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_INTERNAL_ERROR, CREATING,
					RESOURCE, SOME_RESOURCE_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testReadRecord() {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseText",
				() -> "someResponseText");
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));

		String recordFromFedora = fedora.readRecord(dataDivider, SOME_RECORD_ID);

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "GET");
		httpHandlerSpy0.MCR.assertParameters("setRequestProperty", 0, "Accept",
				"text/plain;charset=utf-8");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseText");
		httpHandlerSpy0.MCR.assertReturn("getResponseText", 0, recordFromFedora);
	}

	@Test
	public void testReadRecordRecordNotFound() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		try {
			fedora.readRecord(dataDivider, SOME_RECORD_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA, READING,
					RECORD, SOME_RECORD_ID));
		}
	}

	@Test
	public void testReadRecordErrorReadingFromFedora() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.readRecord(dataDivider, SOME_RECORD_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);

			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, READING,
					SOME_RECORD_ID, RECORD, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testReadRecordErrorOnHttpHandler() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				expectedRecordPath + SOME_RECORD_ID);
		try {
			fedora.readRecord(dataDivider, SOME_RECORD_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MSG_INTERNAL_ERROR, READING, RECORD, SOME_RECORD_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testReadResourceOk() throws Exception {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));

		InputStream resourceFromFedora = fedora.readResource(dataDivider, SOME_RESOURCE_ID);

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "GET");
		httpHandlerSpy0.MCR.assertParameters("setRequestProperty", 0, "Accept",
				"application/octet-stream");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy0.MCR.assertMethodNotCalled("getResponseText");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseBinary");
		httpHandlerSpy0.MCR.assertReturn("getResponseBinary", 0, resourceFromFedora);
	}

	@Test
	public void testReadResourceMetadataSetupHttpCall() {
		fedora.readResourceMetadata(dataDivider, SOME_RESOURCE_ID);

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID + FCR_METADATA);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "GET");
		httpHandlerSpy1.MCR.assertParameters("setRequestProperty", 0, "Accept",
				"application/ld+json");

	}

	@Test
	public void testReadResourceMetadataParseJson() {
		ResourceMetadata readResourceMetadata = fedora.readResourceMetadata(dataDivider,
				SOME_RESOURCE_ID);

		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseText");
		var jsonString = httpHandlerSpy1.MCR.getReturnValue("getResponseText", 0);

		resourceMetadataParser.MCR.assertParameters("parse", 0, jsonString);
		resourceMetadataParser.MCR.assertReturn("parse", 0, readResourceMetadata);

	}

	@Test
	public void testReadResourceMetadataNotFound() {
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		try {
			fedora.readResourceMetadata(dataDivider, SOME_RESOURCE_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA,
					READING_METADATA, RESOURCE, SOME_RESOURCE_ID));
		}
	}

	@Test
	public void testReadResourceMetadataInternalServerError() {
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.readResourceMetadata(dataDivider, SOME_RESOURCE_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, READING,
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testReadResourceMetadataHttpHandlerException() throws Exception {
		httpHandlerSpy1.MRV.setAlwaysThrowException("getResponseText",
				new RuntimeException("someError"));

		try {
			fedora.readResourceMetadata(dataDivider, SOME_RESOURCE_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_INTERNAL_ERROR,
					READING_METADATA, RESOURCE, SOME_RESOURCE_ID));
			assertEquals(e.getCause().getMessage(), "someError");
		}
	}

	@Test
	public void testReadResourceNotFound() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		try {
			fedora.readResource(dataDivider, SOME_RESOURCE_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA, READING,
					RESOURCE, SOME_RESOURCE_ID));
		}
	}

	@Test
	public void testReadResourceErrorReadingFromFedora() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.readResource(dataDivider, SOME_RESOURCE_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, READING,
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testReadResourceErrorOnHttpHandler() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				expectedResourcePath + SOME_RESOURCE_ID);
		try {
			fedora.readResource(dataDivider, SOME_RESOURCE_ID);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_INTERNAL_ERROR, READING,
					RESOURCE, SOME_RESOURCE_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testUpdateRecordOk() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(NO_CONTENT));

		fedora.updateRecord(dataDivider, SOME_RECORD_ID, recordXML);

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "PUT");
		httpHandlerSpy1.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"text/plain;charset=utf-8");
		httpHandlerSpy1.MCR.assertParameters("setOutput", 0, recordXML);
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
	}

	@Test
	public void testUpdateRecordNoRecordExistsWithId() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		try {
			fedora.updateRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA,
					"updating", RECORD, SOME_RECORD_ID));
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test
	public void testUpdateRecordErrorCheckingIfRecordExists() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.updateRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, "updating",
					SOME_RECORD_ID, RECORD, INTERNAL_SERVER_ERROR));
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test
	public void testUpdateRecordErrorUpdatingInFedora() {
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.updateRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, "updating",
					SOME_RECORD_ID, RECORD, INTERNAL_SERVER_ERROR));
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 2);
	}

	@Test
	public void testUpdateRecordErrorOnHttpHandler() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				expectedRecordPath + SOME_RECORD_ID);
		try {
			fedora.updateRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MSG_INTERNAL_ERROR, UPDATING, RECORD, SOME_RECORD_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testUpdateRecordErrorOnSecondHttpHandler() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));
		httpHandlerSpy1.MRV.setAlwaysThrowException("setOutput",
				new RuntimeException("errorFromSpy"));

		try {
			fedora.updateRecord(dataDivider, SOME_RECORD_ID, recordXML);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MSG_INTERNAL_ERROR, UPDATING, RECORD, SOME_RECORD_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testUpdateResourceOk() {
		httpHandlerSpy0.MRV.setSpecificReturnValuesSupplier("getResponseCode", () -> OK);
		httpHandlerSpy1.MRV.setSpecificReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);

		fedora.updateResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "PUT");
		httpHandlerSpy1.MCR.assertParameters("setRequestProperty", 0, "Content-Type", mimeType);
		httpHandlerSpy1.MCR.assertParameters("setStreamOutput", 0, resource);
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
	}

	@Test
	public void testUpdateResourceNoRecordExistsWithId() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		try {
			fedora.updateResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA,
					"updating", RESOURCE, SOME_RESOURCE_ID));
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test
	public void testUpdateResourceErrorCheckingIfRecordExists() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.updateResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, "updating",
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));

		}

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test
	public void testUpdateResourceErrorUpdatingInFedora() {
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.updateResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, "updating",
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 2);
	}

	@Test
	public void testUpdateResourceErrorOnHttpHandler() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				expectedResourcePath + SOME_RESOURCE_ID);
		try {
			fedora.updateResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_INTERNAL_ERROR, UPDATING,
					RESOURCE, SOME_RESOURCE_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testUpdateResourceErrorOnSecondHttpHandler() {
		httpHandlerSpy0.MRV.setSpecificReturnValuesSupplier("getResponseCode", () -> OK);
		httpHandlerSpy1.MRV.setAlwaysThrowException("setStreamOutput",
				new RuntimeException("errorFromSpy"));

		try {
			fedora.updateResource(dataDivider, SOME_RESOURCE_ID, resource, mimeType);
			fail("It failed");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_INTERNAL_ERROR, UPDATING,
					RESOURCE, SOME_RESOURCE_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test
	public void testDeleteRecordOk() throws Exception {
		httpHandlerFactory.MRV.setSpecificReturnValuesSupplier("factor", () -> httpHandlerSpy1,
				expectedRecordPath + SOME_RECORD_ID + FCR_TOMBSTONE);

		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);

		fedora.deleteRecord(dataDivider, SOME_RECORD_ID);
		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 2);

		httpHandlerFactory.MCR.assertParameters("factor", 0, expectedRecordPath + SOME_RECORD_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "DELETE");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1,
				expectedRecordPath + SOME_RECORD_ID + FCR_TOMBSTONE);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "DELETE");
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy1.MCR.assertReturn("getResponseCode", 0, NO_CONTENT);
	}

	@Test
	public void testDeleteRecordNotFound() throws Exception {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NOT_FOUND);
		try {
			fedora.deleteRecord(dataDivider, SOME_RECORD_ID);
			assertTrue(false, "It should have triggered an exception");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA, DELETING,
					RECORD, SOME_RECORD_ID));
		}
	}

	@Test
	public void testDeleteRecordAnyOtherError() throws Exception {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode",
				() -> INTERNAL_SERVER_ERROR);
		try {
			fedora.deleteRecord(dataDivider, SOME_RECORD_ID);
			assertTrue(false, "It should have triggered an exception");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, DELETING,
					SOME_RECORD_ID, RECORD, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testDeleteRecordErrorOnPurge() throws Exception {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode",
				() -> INTERNAL_SERVER_ERROR);
		try {
			fedora.deleteRecord(dataDivider, SOME_RECORD_ID);
			assertTrue(false, "It should have triggered an exception");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, DELETING,
					SOME_RECORD_ID, RECORD, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testDeleteResourceOk() throws Exception {
		httpHandlerFactory.MRV.setSpecificReturnValuesSupplier("factor", () -> httpHandlerSpy1,
				expectedResourcePath + SOME_RESOURCE_ID + FCR_TOMBSTONE);

		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);

		fedora.deleteResource(dataDivider, SOME_RESOURCE_ID);

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 2);

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "DELETE");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1,
				expectedResourcePath + SOME_RESOURCE_ID + FCR_TOMBSTONE);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "DELETE");
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy1.MCR.assertReturn("getResponseCode", 0, NO_CONTENT);
	}

	@Test
	public void testDeleteResourceNotFound() throws Exception {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NOT_FOUND);
		try {
			fedora.deleteResource(dataDivider, SOME_RESOURCE_ID);
			assertTrue(false, "It should have triggered an exception");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_NOT_FOUND_IN_FEDORA, DELETING,
					RESOURCE, SOME_RESOURCE_ID));
		}
	}

	@Test
	public void testDeleteResourceAnyOtherError() throws Exception {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode",
				() -> INTERNAL_SERVER_ERROR);
		try {
			fedora.deleteResource(dataDivider, SOME_RESOURCE_ID);
			assertTrue(false, "It should have triggered an exception");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, DELETING,
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testDeleteResourceErrorOnPurge() throws Exception {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode",
				() -> INTERNAL_SERVER_ERROR);
		try {
			fedora.deleteResource(dataDivider, SOME_RESOURCE_ID);
			assertTrue(false, "It should have triggered an exception");
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR, DELETING,
					SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testUpdateResourceMetadata() throws Exception {
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);

		fedora.updateResourceMetadata(dataDivider, SOME_RESOURCE_ID, metadataResourceToUpdate);

		httpHandlerFactory.MCR.assertParameters("factor", 0,
				expectedResourcePath + SOME_RESOURCE_ID + FCR_METADATA);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "PATCH");
		httpHandlerSpy1.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"application/sparql-update");

		String body = """
				PREFIX ebucore: <http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#>
				INSERT {<> ebucore:filename "someOriginalFileName" . <> ebucore:hasMimeType "someMimeType" .}
				WHERE {}
				""";

		httpHandlerSpy1.MCR.assertParameters("setOutput", 0, body);
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy1.MCR.assertReturn("getResponseCode", 0, 204);
	}

	@Test
	public void testUpdateResourceMetadataNotFound() throws Exception {
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NOT_FOUND);

		try {
			fedora.updateResourceMetadata(dataDivider, SOME_RESOURCE_ID, metadataResourceToUpdate);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR,
					UPDATING_METADATA, SOME_RESOURCE_ID, RESOURCE, NOT_FOUND));
		}
	}

	@Test
	public void testUpdateResourceMetadataExceptionOnFedora() throws Exception {
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode",
				() -> INTERNAL_SERVER_ERROR);

		try {
			fedora.updateResourceMetadata(dataDivider, SOME_RESOURCE_ID, metadataResourceToUpdate);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_FEDORA_ERROR,
					UPDATING_METADATA, SOME_RESOURCE_ID, RESOURCE, INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	public void testUpdateResourceMetadataInternalException() throws Exception {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				expectedResourcePath + SOME_RESOURCE_ID + FCR_METADATA);
		try {
			fedora.updateResourceMetadata(dataDivider, SOME_RESOURCE_ID, metadataResourceToUpdate);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_INTERNAL_ERROR,
					UPDATING_METADATA, RESOURCE, SOME_RESOURCE_ID));
			assertEquals(e.getCause().getMessage(), "errorFromSpy");

		}
	}

}
