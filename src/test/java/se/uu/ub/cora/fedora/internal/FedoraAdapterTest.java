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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.FedoraAdapter;
import se.uu.ub.cora.fedora.FedoraConflictException;
import se.uu.ub.cora.fedora.FedoraException;
import se.uu.ub.cora.fedora.spy.HttpHandlerFactorySpy;
import se.uu.ub.cora.fedora.spy.HttpHandlerSpy;
import se.uu.ub.cora.fedora.spy.InputStreamSpy;

public class FedoraAdapterTest {

	private HttpHandlerFactorySpy httpHandlerFactory;
	private FedoraAdapter fedora;
	private String baseUrl;
	private String recordXML = "<somexml></somexml>";
	private String recordId = "someRecordId:001";

	@BeforeMethod
	public void setUp() {
		baseUrl = "http://localhost:38088/fcrepo/rest/";
		httpHandlerFactory = new HttpHandlerFactorySpy();
		fedora = new FedoraAdapterImp(httpHandlerFactory, baseUrl);
	}

	@Test
	public void testCreateOk() {
		httpHandlerFactory.statusResponses.add(404);
		httpHandlerFactory.statusResponses.add(201);
		fedora.create(recordId, recordXML);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		HttpHandlerSpy factoredHttpHandlerExistsCall = (HttpHandlerSpy) httpHandlerFactory.MCR
				.getReturnValue("factor", 0);
		factoredHttpHandlerExistsCall.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		factoredHttpHandlerExistsCall.MCR.assertMethodWasCalled("getResponseCode");

		HttpHandlerSpy factoredHttpHandler = (HttpHandlerSpy) httpHandlerFactory.MCR
				.getReturnValue("factor", 1);
		assertEquals(factoredHttpHandler.requestMetod, "PUT");
		factoredHttpHandler.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"text/plain;charset=utf-8");
		factoredHttpHandler.MCR.assertParameters("setOutput", 0, recordXML);
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseCode");
	}

	@Test
	public void testCreateDuplicateError() {
		httpHandlerFactory.statusResponses.add(200);
		httpHandlerFactory.statusResponses.add(201);
		try {
			fedora.create(recordId, recordXML);
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraConflictException);
			assertEquals(e.getMessage(),
					"Record with id: " + recordId + " already exists in Fedora.");
		}

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		HttpHandlerSpy factoredHttpHandlerExistsCall = (HttpHandlerSpy) httpHandlerFactory.MCR
				.getReturnValue("factor", 0);
		factoredHttpHandlerExistsCall.MCR.assertParameters("setRequestMethod", 0, "HEAD");
		factoredHttpHandlerExistsCall.MCR.assertMethodWasCalled("getResponseCode");

		httpHandlerFactory.MCR.assertNumberOfCallsToMethod("factor", 1);

	}

	@Test
	public void testCreateAnyOtherError() {
		httpHandlerFactory.statusResponses.add(404);
		httpHandlerFactory.statusResponses.add(500);
		try {
			fedora.create(recordId, recordXML);
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
	public void testCreateAnyOtherError2() {
		httpHandlerFactory.statusResponses.add(500);
		fedora.create(recordId, recordXML);
	}

	@Test
	public void testCreateAnyOtherErrorOnFactor() {
		httpHandlerFactory.throwExceptionRuntimeException = true;
		try {
			fedora.create(recordId, recordXML);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof FedoraException);
			assertEquals(e.getMessage(),
					"Error storing record in Fedora, recordId: someRecordId:001");
			assertTrue(e.getCause() instanceof IndexOutOfBoundsException);
		}
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing record in Fedora, recordId: someRecordId:001")
	public void testCreateAnyOtherErrorAddXmlToHttpHandler() {
		httpHandlerFactory.statusResponses.add(404);
		httpHandlerFactory.statusResponses.add(201);
		httpHandlerFactory.throwExceptionRuntimeException = true;
		fedora.create(recordId, recordXML);
	}

	@Test
	public void testUpdateOk() {
		httpHandlerFactory.statusResponses.add(204);
		fedora.update(recordId, recordXML);

		assertEquals(httpHandlerFactory.url, baseUrl + recordId);
		HttpHandlerSpy factoredHttpHandler = httpHandlerFactory.factoredHttpHandler;
		assertEquals(factoredHttpHandler.requestMetod, "PUT");
		factoredHttpHandler.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"text/plain;charset=utf-8");
		factoredHttpHandler.MCR.assertParameters("setOutput", 0, recordXML);
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseCode");
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing record in Fedora, recordId: someRecordId:001")
	public void testUpdateError() {
		httpHandlerFactory.statusResponses.add(500);
		fedora.update(recordId, recordXML);
	}

	@Test
	public void testRead() {
		httpHandlerFactory.statusResponses.add(200);

		String recordFromFedora = fedora.read(recordId);

		assertEquals(httpHandlerFactory.url, baseUrl + recordId);
		HttpHandlerSpy factoredHttpHandler = httpHandlerFactory.factoredHttpHandler;
		assertEquals(factoredHttpHandler.requestMetod, "GET");
		factoredHttpHandler.MCR.assertParameters("setRequestProperty", 0, "Accept",
				"text/plain;charset=utf-8");
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseCode");
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseText");
		factoredHttpHandler.MCR.assertReturn("getResponseText", 0, recordFromFedora);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading record from Fedora, recordId: someRecordId:001")
	public void testReadRecordNotFound() {
		httpHandlerFactory.statusResponses.add(404);
		fedora.read(recordId);
	}

	@Test
	public void testCreateBinary() throws Exception {

		httpHandlerFactory.statusResponses.add(201);
		InputStream binary = new InputStreamSpy();
		String binaryContentType = "image/jpg";

		fedora.createBinary(recordId, binary, binaryContentType);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		HttpHandlerSpy factoredHttpHandler = (HttpHandlerSpy) httpHandlerFactory.MCR
				.getReturnValue("factor", 0);

		factoredHttpHandler.MCR.assertParameters("setRequestMethod", 0, "PUT");
		factoredHttpHandler.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				binaryContentType);
		factoredHttpHandler.MCR.assertParameters("setStreamOutput", 0, binary);
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseCode");

	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing binary in Fedora, recordId: someRecordId:001")
	public void testCreateBinaryErrorWhileStoring() {
		httpHandlerFactory.statusResponses.add(500);
		InputStream binary = new InputStreamSpy();
		fedora.createBinary(recordId, binary, "image/jpg");
	}

	@Test
	public void testReadBinaryOk() throws Exception {
		httpHandlerFactory.statusResponses.add(200);

		InputStream binaryFromFedora = fedora.readBinary(recordId);

		assertEquals(httpHandlerFactory.url, baseUrl + recordId);
		HttpHandlerSpy factoredHttpHandler = httpHandlerFactory.factoredHttpHandler;
		assertEquals(factoredHttpHandler.requestMetod, "GET");
		// factoredHttpHandler.MCR.assertParameters("setRequestProperty", 0, "Accept",
		// "text/plain;charset=utf-8");
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseCode");
		factoredHttpHandler.MCR.assertMethodNotCalled("getResponseText");
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseBinary");
		factoredHttpHandler.MCR.assertReturn("getResponseBinary", 0, binaryFromFedora);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading binary from Fedora, recordId: someRecordId:001")
	public void testReadBinaryNotFound() {
		httpHandlerFactory.statusResponses.add(404);

		fedora.readBinary(recordId);
	}

}
