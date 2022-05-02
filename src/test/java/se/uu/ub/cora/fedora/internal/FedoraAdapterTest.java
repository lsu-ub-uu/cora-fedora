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
	public void testCreateOk() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(404));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(201));

		fedora.create(recordId, recordXML);

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
	public void testCreateDuplicateError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(200));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(201));

		try {
			fedora.create(recordId, recordXML);
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
	public void testCreateAnyOtherError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(404));
		httpHandlerSpy1.MRV.setReturnValues("getResponseCode", List.of(500));

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
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(500));
		fedora.create(recordId, recordXML);
	}

	@Test
	public void testCreateAnyOtherErrorOnFactor() {
		httpHandlerFactory.MRV.setThrowException("factor", new RuntimeException("errorFromSpy"),
				baseUrl + recordId);
		try {
			fedora.create(recordId, recordXML);
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
	public void testCreateAnyOtherErrorAddXmlToHttpHandler() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(404));

		httpHandlerSpy1.MRV.setAlwaysThrowException("setOutput",
				new RuntimeException("errorFromSpy"));
		fedora.create(recordId, recordXML);
	}

	@Test
	public void testUpdateOk() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(204));
		fedora.update(recordId, recordXML);

		httpHandlerFactory.MCR.assertParameters("factor", 0, baseUrl + recordId);
		httpHandlerSpy0.MCR.assertParameters("setRequestMethod", 0, "PUT");
		httpHandlerSpy0.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"text/plain;charset=utf-8");
		httpHandlerSpy0.MCR.assertParameters("setOutput", 0, recordXML);
		httpHandlerSpy0.MCR.assertMethodWasCalled("getResponseCode");
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error storing record in Fedora, recordId: someRecordId:001")
	public void testUpdateError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(500));
		fedora.update(recordId, recordXML);
	}

	@Test
	public void testRead() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(200));

		String recordFromFedora = fedora.read(recordId);

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
	public void testReadRecordNotFound() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(404));
		fedora.read(recordId);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading record from Fedora, recordId: someRecordId:001")
	public void testReadErrorReading() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(500));
		fedora.read(recordId);
	}

	@Test
	public void testCreateBinary() throws Exception {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(201));

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
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(500));
		InputStream binary = new InputStreamSpy();
		fedora.createBinary(recordId, binary, "image/jpg");
	}

	@Test
	public void testReadBinaryOk() throws Exception {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(200));

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
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(404));

		fedora.readBinary(recordId);
	}

	@Test(expectedExceptions = FedoraException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading binary from Fedora, recordId: someRecordId:001")
	public void testReadBinaryOtherError() {
		httpHandlerSpy0.MRV.setReturnValues("getResponseCode", List.of(500));

		fedora.readBinary(recordId);
	}

}
