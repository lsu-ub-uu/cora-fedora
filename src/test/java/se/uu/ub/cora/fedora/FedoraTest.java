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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FedoraTest {

	private HttpHandlerFactorySpy httpHandlerFactory;
	private FedoraWrapper fedora;
	private String baseUrl;

	@BeforeMethod
	public void setUp() {
		baseUrl = "http://localhost:38088/fcrepo/rest/";
		httpHandlerFactory = new HttpHandlerFactorySpy();
		fedora = new FedoraImp(httpHandlerFactory, baseUrl);
	}

	@Test
	public void testCreateOk() {
		String fedoraXML = "some new metadata xml to send to storage";
		String recordId = "someRecordId:001";

		String returnResponse = fedora.create(recordId, fedoraXML);

		assertEquals(httpHandlerFactory.url, baseUrl + recordId);

		HttpHandlerSpy factoredHttpHandler = httpHandlerFactory.factoredHttpHandler;
		assertEquals(factoredHttpHandler.requestMetod, "PUT");

		factoredHttpHandler.MCR.assertParameters("setRequestProperty", 0, "Content-Type",
				"text/plain;charset=utf-8");
		factoredHttpHandler.MCR.assertParameters("setOutput", 0, fedoraXML);

		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseCode");
		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseText");

		factoredHttpHandler.MCR.assertReturn("getResponseText", 0, returnResponse);

	}

	@Test
	public void testCreateNotFound() {
		String recordXML = "<somexml></somexml>";
		String recordId = "someRecordId:001";
		httpHandlerFactory.statusResponse = 500;

		try {
			fedora.create(recordId, recordXML);
		} catch (Exception exception) {
			assertTrue(exception instanceof FedoraException);
			assertEquals(exception.getMessage(),
					"Error storing record in Fedora, recordId: someRecordId:001");
		}

		//
		HttpHandlerSpy factoredHttpHandler = httpHandlerFactory.factoredHttpHandler;

		factoredHttpHandler.MCR.assertMethodWasCalled("getResponseCode");
		factoredHttpHandler.MCR.assertMethodNotCalled("getResponseText");

	}

	// TODO: Kolla om är detta vad vi vill att det ska hända när vi inte når URL. eller ska den
	// hanteras i Storage
	@Test
	public void testCreateConnectionRefused() {
		String recordXML = "<somexml></somexml>";
		String recordId = "someRecordId:001";

		httpHandlerFactory.throwExceptionRuntimeException = true;

		try {
			fedora.create(recordId, recordXML);
		} catch (Exception exception) {
			assertTrue(exception instanceof FedoraException);
			assertEquals(exception.getMessage(),
					"Error connecting to fedora, with url: " + baseUrl);
		}

	}
}
