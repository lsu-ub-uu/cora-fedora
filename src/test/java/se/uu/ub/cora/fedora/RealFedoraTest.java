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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpHandlerFactoryImp;

public class RealFedoraTest {

	private HttpHandlerFactory httpHandlerFactory;
	private FedoraWrapper fedora;
	private String baseUrl;

	@BeforeMethod
	public void setUp() {
		baseUrl = "http://alvin-docker-fedora:8080/fcrepo/rest/";
		// httpHandlerFactory = new HttpHandlerFactorySpy();
		httpHandlerFactory = new HttpHandlerFactoryImp();
		fedora = new FedoraImp(httpHandlerFactory, baseUrl);
	}

	@Test(enabled = false)
	public void testCreateOk() {
		String fedoraXML = "<trying></trying>";
		String recordId = "someRecordId:010";

		fedora.create(recordId, fedoraXML);

	}

	@Test(enabled = false)
	public void testReadOk() {
		String recordId = "someRecordId:010";

		String read = fedora.read(recordId);
		assertEquals(read, "");

	}

	@Test(enabled = false)
	public void testReadBinary() throws IOException {
		String recordId = "someRecordId:020";
		File targetFile = new File("/home/pere/workspace/castle2.jpg");
		OutputStream outStream = new FileOutputStream(targetFile);

		InputStream binary = fedora.readBinary(recordId);

		binary.transferTo(outStream);

	}

	@Test(enabled = false)
	public void testCreateBinaryOk() {
		String recordId = "someRecordId:020";

		try {
			// File initialFile = new File("/home/madde/workspace/bild.jpg");
			File initialFile = new File("/home/pere/workspace/castle.jpg");
			InputStream binary = new FileInputStream(initialFile);
			fedora.createBinary(recordId, binary, "image/jpeg");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
