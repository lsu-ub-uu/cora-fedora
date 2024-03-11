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
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.FedoraAdapter;
import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpHandlerFactoryImp;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;

public class RealFedoraTest {

	private HttpHandlerFactory httpHandlerFactory;
	private FedoraAdapter fedora;
	private String baseUrl;

	@BeforeMethod
	public void setUp() {
		baseUrl = "http://systemone-fedora:8080/fcrepo/rest/";
		httpHandlerFactory = new HttpHandlerFactoryImp();
		OrgJsonParser orgJsonParser = new OrgJsonParser();
		ResourceMetadataParserImp resourceMetadataParser = ResourceMetadataParserImp
				.usingJsonParser(orgJsonParser);
		fedora = new FedoraAdapterImp(httpHandlerFactory, baseUrl, resourceMetadataParser);
	}

	@Test(enabled = false)
	public void testCreateOk() {
		String recordId = "someRecordId:200001";
		String fedoraXML = "test";

		int numOfCalls = 2;
		List<Thread> threadList = new ArrayList<>();

		for (int i = 0; i < numOfCalls; i++) {
			threadList.add(new Thread(new CallFedoraUsingFedoraAdapter(recordId + i, fedoraXML)));
			// threadList.add(new Thread(new CallFedoraUsingHttpHandler(recordId + i, fedoraXML)));
		}

		for (Thread thread : threadList) {
			thread.start();
		}

		try {
			for (Thread thread : threadList) {
				thread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Both createRecord calls completed.");

	}

	static class CallFedoraUsingFedoraAdapter implements Runnable {
		private String recordId;
		private String fedoraXML;
		private FedoraAdapter fedoraAdapter;

		public CallFedoraUsingFedoraAdapter(String recordId, String fedoraXML) {
			this.recordId = recordId;
			this.fedoraXML = fedoraXML;
		}

		@Override
		public void run() {
			createNewFedoraAdapter();
			System.out.println("Creating record with ID " + recordId);
			fedoraAdapter.createRecord("aDataDivider", recordId, fedoraXML);
		}

		private void createNewFedoraAdapter() {
			String baseUrl = "http://systemone-fedora:8080/fcrepo/rest/";
			HttpHandlerFactory httpHandlerFactory = new HttpHandlerFactoryImp();
			OrgJsonParser orgJsonParser = new OrgJsonParser();
			ResourceMetadataParserImp resourceMetadataParser = ResourceMetadataParserImp
					.usingJsonParser(orgJsonParser);
			fedoraAdapter = new FedoraAdapterImp(httpHandlerFactory, baseUrl,
					resourceMetadataParser);
		}
	}

	static class CallFedoraUsingHttpHandler implements Runnable {
		private String recordId;
		private String fedoraXML;
		private static final String CONTENT_TYPE = "Content-Type";

		private static final String MIME_TYPE_TEXT_PLAIN_UTF_8 = "text/plain;charset=utf-8";
		private HttpHandlerFactory httpHandlerFactory;
		String baseUrl = "http://systemone-fedora:8080/fcrepo/rest/";

		public CallFedoraUsingHttpHandler(String recordId, String fedoraXML) {
			this.recordId = recordId;
			this.fedoraXML = fedoraXML;
			httpHandlerFactory = new HttpHandlerFactoryImp();
		}

		@Override
		public void run() {
			System.out.println("Creating record with ID " + recordId);
			String path = ensemblePathForRecord("aDataDivider", recordId);
			HttpHandler httpHandler = setupHttpHandlerForStoreRecord(path, fedoraXML);
			int responseCode = httpHandler.getResponseCode();

			System.out.println("Code " + responseCode + ", path: " + path + ", errorText: "
					+ httpHandler.getErrorText());
		}

		private String ensemblePathForRecord(String dataDivider, String recordId) {
			// return baseUrl + dataDivider + "/" + "record/" + recordId;
			return baseUrl + recordId;
		}

		private HttpHandler setupHttpHandlerForStoreRecord(String path, String fedoraXML) {
			HttpHandler httpHandler = httpHandlerFactory.factor(path);
			httpHandler.setRequestMethod("PUT");
			httpHandler.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
			httpHandler.setOutput("someStuff");
			return httpHandler;
		}

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
