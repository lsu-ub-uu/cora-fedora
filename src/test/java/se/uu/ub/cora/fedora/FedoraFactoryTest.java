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
package se.uu.ub.cora.fedora;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.internal.FedoraAdapterImp;
import se.uu.ub.cora.fedora.internal.ResourceMetadataParserImp;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpHandlerFactoryImp;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;

public class FedoraFactoryTest {

	HttpHandlerFactory httpHandlerFactory;
	private String someFedoraUrl;
	private FedoraFactory factory;
	private FedoraAdapterImp fedoraAdapter;

	@BeforeMethod
	public void beforeMethod() {
		someFedoraUrl = "http://someFedoraUrl/";
		factory = new FedoraFactoryImp(someFedoraUrl);
		fedoraAdapter = (FedoraAdapterImp) factory.factorFedoraAdapter();
	}

	@Test
	public void testInit() throws Exception {
		assertNotNull(fedoraAdapter);
	}

	@Test
	public void testHttpHandlerFactoryCreatedAndSentToInstances() throws Exception {
		HttpHandlerFactoryImp factoredHttpHandlerFactory = (HttpHandlerFactoryImp) fedoraAdapter
				.onlyForTestGetHttpHandlerFactory();
		assertNotNull(factoredHttpHandlerFactory);
		FedoraAdapterImp fedoraAdapter = (FedoraAdapterImp) factory.factorFedoraAdapter();
		HttpHandlerFactoryImp factoredHttpHandlerFactory2 = (HttpHandlerFactoryImp) fedoraAdapter
				.onlyForTestGetHttpHandlerFactory();
		assertSame(factoredHttpHandlerFactory, factoredHttpHandlerFactory2);
	}

	@Test
	public void testJsonParserCreatedAndSentToInstances() throws Exception {
		// OrgJsonParser jsonParser = (OrgJsonParser) fedoraAdapter.onlyForTestGetJsonParser();
		// assertNotNull(factoredHttpHandlerFactory);
		FedoraAdapterImp fedoraAdapter2 = (FedoraAdapterImp) factory.factorFedoraAdapter();

		ResourceMetadataParserImp parser = (ResourceMetadataParserImp) fedoraAdapter2
				.onlyForTestGetResourceMetadataParser();

		assertTrue(parser.onlyForTestGetJsonParser() instanceof OrgJsonParser);

		// HttpHandlerFactoryImp factoredHttpHandlerFactory2 = (HttpHandlerFactoryImp)
		// fedoraAdapter2
		// .onlyForTestGetHttpHandlerFactory();
		// assertSame(factoredHttpHandlerFactory, factoredHttpHandlerFactory2);
	}

	@Test
	public void testFedoraUrlSentToInstances() throws Exception {
		assertEquals(fedoraAdapter.onlyForTestGetBaseUrl(), someFedoraUrl);
	}

	@Test
	public void testGetFedoraUrl() throws Exception {
		assertEquals(((FedoraFactoryImp) factory).onlyForTestGetBaseUrl(), someFedoraUrl);
	}
}
