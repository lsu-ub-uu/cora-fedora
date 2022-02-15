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

import static org.testng.Assert.assertEquals;

import java.io.InputStream;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FedoraTest {

	private HttpHandlerFactorySpy httpHandlerFactory;
	private FedoraWrapper fedora;
	private String baseUrl;
	private String recordXML = "<somexml></somexml>";
	private String recordId = "someRecordId:001";

	@BeforeMethod
	public void setUp() {
		baseUrl = "http://localhost:38088/fcrepo/rest/";
		httpHandlerFactory = new HttpHandlerFactorySpy();
		fedora = new FedoraImp(httpHandlerFactory, baseUrl);
	}

	@Test
	public void testCreateOk() {

		fedora.create(recordId, recordXML);

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
	public void testCreateNotFound() {
		httpHandlerFactory.statusResponse = 500;
		fedora.create(recordId, recordXML);

	}

	@Test
	public void testUpdateOk() {
		httpHandlerFactory.statusResponse = 204;
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
		httpHandlerFactory.statusResponse = 500;
		fedora.update(recordId, recordXML);
	}

	@Test
	public void testRead() {
		httpHandlerFactory.statusResponse = 200;

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
		httpHandlerFactory.statusResponse = 404;
		fedora.read(recordId);
	}

	@Test
	public void testCreateBinary() throws Exception {

		InputStream binary = new InputStreamSpy();

		fedora.createBinary(recordId, binary);

		httpHandlerFactory.MCR.assertParameters("factorHttpMultiPartUploader", 0,
				baseUrl + recordId);

		HttpMultiPartUploaderSpy httpMultiPartUploader = (HttpMultiPartUploaderSpy) httpHandlerFactory.MCR
				.getReturnValue("factorHttpMultiPartUploader", 0);

		httpMultiPartUploader.MCR.assertParameters("setRequestMethod", 0, "PUT");
		// TODO: Vad är som skickas som parameter till addFilePart.
		httpMultiPartUploader.MCR.assertParameters("addFilePart", 0, "file", "someFileName",
				binary);

		httpMultiPartUploader.MCR.assertMethodWasCalled("getResponseCode");
	}
	// TODO Below is the content store on file in Fedora
	// Vad är skillnad mellan name och filename???
	// Content-Disposition: form-data; name="file"; filename="someFileName"
	// Content-Type: null
	// Content-Transfer-Encoding: binary

}
