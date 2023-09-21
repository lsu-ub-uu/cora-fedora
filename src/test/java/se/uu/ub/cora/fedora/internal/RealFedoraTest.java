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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.FedoraAdapter;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpHandlerFactoryImp;

public class RealFedoraTest {

	private HttpHandlerFactory httpHandlerFactory;
	private FedoraAdapter fedora;
	private String baseUrl;

	@BeforeMethod
	public void setUp() {
		baseUrl = "http://systemone-fedora:8080/fcrepo/rest/";
		// httpHandlerFactory = new HttpHandlerFactorySpy();
		httpHandlerFactory = new HttpHandlerFactoryImp();
		fedora = new FedoraAdapterImp(httpHandlerFactory, baseUrl);
	}

	@Test(enabled = false)
	public void testCreateOk() {
		String fedoraXML = "<trying>hello</trying>";
		String recordId = "someRecordId:112";

		fedora.createRecord(null, recordId, fedoraXML);

	}

	@Test(enabled = false)
	public void testUpdateOk() {
		String fedoraXML = "<trying>helloUpdated</trying>";
		String recordId = "someRecordId:114";

		fedora.updateRecord(null, recordId, fedoraXML);

	}

	@Test(enabled = false)
	public void testReadOk() {
		String recordId = "someRecordId:022";

		String read = fedora.readRecord(null, recordId);
		assertEquals(read, "");

	}

	@Test(enabled = false)
	public void testReadResouce() throws IOException {
		String recordId = "binary:binary:24583449702428-master";
		File targetFile = new File("/home/pere/workspace/gokuForever.jpg");
		OutputStream outStream = new FileOutputStream(targetFile);

		InputStream resouce = fedora.readResource("testSystem", recordId);

		resouce.transferTo(outStream);

	}

	@Test(enabled = false)
	public void testCreateResouceOk() {
		String recordId = "someRecordId:030";

		try {
			// File initialFile = new File("/home/madde/workspace/bild.jpg");
			File initialFile = new File("/home/marcus/workspace/bg.jpg");
			InputStream resouce = new FileInputStream(initialFile);
			fedora.createResource(null, recordId, resouce, "image/jpeg");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test(enabled = false)
	public void testDeleteResouceOK() throws Exception {
		String recordId = "someRecordId:0242";

		// try {
		fedora.deleteRecord(null, recordId);

		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

	}

	@Test(enabled = false)
	public void testUpdateResouceOK() throws Exception {
		String recordId = "someRecordId:363";

		try {
			// File initialFile = new File("/home/madde/workspace/bild.jpg");
			File initialFile = new File("/home/marcus/workspace/ghandi.jpg");
			InputStream resouce = new FileInputStream(initialFile);
			fedora.updateResource(null, recordId, resouce, "image/jpeg");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
