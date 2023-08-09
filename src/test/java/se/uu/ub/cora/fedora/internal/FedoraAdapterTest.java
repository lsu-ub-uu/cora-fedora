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

import java.io.InputStream;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.FedoraAdapter;
import se.uu.ub.cora.fedora.FedoraConflictException;
import se.uu.ub.cora.fedora.FedoraException;
import se.uu.ub.cora.fedora.FedoraNotFoundException;
import se.uu.ub.cora.testspies.httphandler.HttpHandlerFactorySpy;
import se.uu.ub.cora.testspies.httphandler.HttpHandlerSpy;
import se.uu.ub.cora.testspies.httphandler.InputStreamSpy;

public class FedoraAdapterTest {

	private static final String TOMBSTONE = "/fcr:tombstone";
	private static final int CREATED = 201;
	private static final int OK = 200;
	private static final int INTERNAL_SERVER_ERROR = 500;
	private static final int NO_CONTENT = 204;
	private static final int NOT_FOUND = 404;
	private HttpHandlerFactorySpy httpHandlerFactory;
	private FedoraAdapter fedora;
	private String baseUrl;
	private String recordXML = "<somexml></somexml>";
	private String recordId = "someRecordId:001";
	private HttpHandlerSpy httpHandlerSpy0;
	private HttpHandlerSpy httpHandlerSpy1;

	@BeforeMethod
	public void setUp() {
		baseUrl = "http://localhost:38088/fcrepo/rest/";
		httpHandlerFactory = new HttpHandlerFactorySpy();

		httpHandlerSpy0 = new HttpHandlerSpy();
		httpHandlerSpy1 = new HttpHandlerSpy();
		httpHandlerFactory.MRV.setReturnValues("factor", List.of(httpHandlerSpy0, httpHandlerSpy1),
				baseUrl + recordId);

		fedora = new FedoraAdapterImp(httpHandlerFactory, baseUrl);
	}

	@Test
	public void testCreateRecordOk() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(CREATED));

		fedora.createRecord(recordId, recordXML);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1, baseUrl + recordId);
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
			fedora.createRecord(recordId, recordXML);
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraConflictException);
			assertEquals(e.getMessage(),
					"Record with id: " + recordId + " already exists in Fedora.");
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test
	public void testCreateRecordAnyOtherError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.createRecord(recordId, recordXML);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					"Error storing record in Fedora, recordId: someRecordId:001");
			assertTrue(e.getCause() instanceof FedoraException);
		}
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing record in Fedora, recordId: someRecordId:001")
	public void testCreateRecordAnyOtherError2() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));
		fedora.createRecord(recordId, recordXML);
	}

	@Test
	public void testCreateRecordAnyOtherErrorOnFactor() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				baseUrl + recordId);
		try {
			fedora.createRecord(recordId, recordXML);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					"Error storing record in Fedora, recordId: someRecordId:001");
			assertEquals(e.getCause().getMessage(), "errorFromSpy");
		}
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing record in Fedora, recordId: someRecordId:001")
	public void testCreateRecordAnyOtherErrorAddXmlToHttpHandler() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		httpHandlerSpy1.MRV.setAlwaysThrowException("setOutput",
				new RuntimeException("errorFromSpy"));
		fedora.createRecord(recordId, recordXML);
	}

	@Test
	public void testUpdateOk() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(NO_CONTENT));

		fedora.updateRecord(recordId, recordXML);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1, baseUrl + recordId);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "PUT");
		httpHandlerSpy1.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"text/plain;charset=utf-8");
		httpHandlerSpy1.MCR.assertParameters("setOutput", 0, recordXML);
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
	}

	@Test
	public void testUpdateNoRecordExistsWithId() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		try {
			fedora.updateRecord(recordId, recordXML);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraNotFoundException);
			assertEquals(e.getMessage(),
					"Record with id: someRecordId:001 does not exist in Fedora.");
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test
	public void testUpdateErrorCheckingIfRecordExists() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		try {
			fedora.updateRecord(recordId, recordXML);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					"Error storing record in Fedora, recordId: someRecordId:001");
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing record in Fedora, recordId: someRecordId:001")
	public void testUpdateError() {
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));
		fedora.updateRecord(recordId, recordXML);
	}

	@Test
	public void testReadRecord() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));

		String recordFromFedora = fedora.readRecord(recordId);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "GET");
		httpHandlerSpy0.MCR.assertParameters("setRequestProperty", 0, "Accept",
				"text/plain;charset=utf-8");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseText");
		httpHandlerSpy0.MCR.assertReturn("getResponseText", 0, recordFromFedora);
	}

	@Test(expectedExceptions = FedoraNotFoundException.class, expectedExceptionsMessageRegExp = ""
			+ "Record with id: someRecordId:001 does not exist in Fedora.")
	public void testReadRecordRecordNotFound() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));
		fedora.readRecord(recordId);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading record from Fedora, recordId: someRecordId:001")
	public void testReadRecordErrorReading() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));
		fedora.readRecord(recordId);
	}

	@Test
	public void testCreateBinary() throws Exception {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(CREATED));

		InputStream binary = new InputStreamSpy();
		String binaryContentType = "image/jpg";

		fedora.createBinary(recordId, binary, binaryContentType);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "PUT");
		httpHandlerSpy0.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				binaryContentType);
		httpHandlerSpy0.MCR.assertParameters("setStreamOutput", 0, binary);
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing binary in Fedora, recordId: someRecordId:001")
	public void testCreateBinaryErrorWhileStoring() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));
		InputStream binary = new InputStreamSpy();
		fedora.createBinary(recordId, binary, "image/jpg");
	}

	@Test
	public void testReadBinaryOk() throws Exception {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(OK));

		InputStream binaryFromFedora = fedora.readBinary(recordId);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "GET");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy0.MCR.assertMethodNotCalled("getResponseText");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseBinary");
		httpHandlerSpy0.MCR.assertReturn("getResponseBinary", 0, binaryFromFedora);
	}

	@Test(expectedExceptions = FedoraNotFoundException.class, expectedExceptionsMessageRegExp = ""
			+ "Binary with id: someRecordId:001 does not exist in Fedora.")
	public void testReadBinaryNotFound() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(NOT_FOUND));

		fedora.readBinary(recordId);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading binary from Fedora, recordId: someRecordId:001")
	public void testReadBinaryOtherError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(INTERNAL_SERVER_ERROR));

		fedora.readBinary(recordId);
	}

	@Test
	public void testDeleteOk() throws Exception {
		httpHandlerFactory.MRV.setSpecificReturnValuesSupplier("factor", () -> httpHandlerSpy1,
				baseUrl + recordId + TOMBSTONE);

		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);

		fedora.delete(recordId);
		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 2);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "DELETE");
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertParameters("factor", 1, baseUrl + recordId + TOMBSTONE);
		httpHandlerSpy1.MCR.assertParameters("setRequestMethod", 0, "DELETE");
		httpHandlerSpy1.MCR.assertMethodWasCalled("getResponseCode");
		httpHandlerSpy1.MCR.assertReturn("getResponseCode", 0, NO_CONTENT);
	}

	@Test(expectedExceptions = FedoraNotFoundException.class, expectedExceptionsMessageRegExp = ""
			+ "Unable to delete record or binary from fedora. Resource not found with recordId: someRecordId:001")
	public void testDeleteRecordNotFound() throws Exception {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NOT_FOUND);

		fedora.delete(recordId);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error deleting record or binary in Fedora, recordId: someRecordId:001")
	public void testDeleteRecordAnyOtherError() {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode",
				() -> INTERNAL_SERVER_ERROR);
		fedora.delete(recordId);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error deleting record or binary in Fedora, recordId: someRecordId:001")
	public void testDeleteRecordErrorOnPurge() {
		httpHandlerSpy0.MRV.setDefaultReturnValuesSupplier("getResponseCode", () -> NO_CONTENT);
		httpHandlerSpy1.MRV.setDefaultReturnValuesSupplier("getResponseCode",
				() -> INTERNAL_SERVER_ERROR);
		fedora.delete(recordId);
	}
}
