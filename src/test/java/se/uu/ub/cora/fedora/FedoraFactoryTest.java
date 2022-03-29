package se.uu.ub.cora.fedora;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.fedora.internal.FedoraAdapterImp;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;
import se.uu.ub.cora.httphandler.HttpHandlerFactoryImp;

public class FedoraFactoryTest {

	HttpHandlerFactory httpHandlerFactory;
	private String someFedoraUrl;
	private FedoraFactory factory;

	@BeforeMethod
	public void beforeMethod() {
		someFedoraUrl = "http://someFedoraUrl/";
		factory = new FedoraFactoryImp(someFedoraUrl);
	}

	@Test
	public void testInit() throws Exception {
		FedoraAdapterImp fedoraAdapter = (FedoraAdapterImp) factory.factorFedoraAdapter();

		assertNotNull(fedoraAdapter);
	}

	@Test
	public void testHttpHandlerFactoryCreatedAndSentToInstances() throws Exception {
		FedoraAdapterImp fedoraAdapter = (FedoraAdapterImp) factory.factorFedoraAdapter();
		HttpHandlerFactoryImp factoredHttpHandlerFactory = (HttpHandlerFactoryImp) fedoraAdapter
				.onlyForTestGetHttpHandlerFactory();
		assertNotNull(factoredHttpHandlerFactory);
		FedoraAdapterImp fedoraAdapter2 = (FedoraAdapterImp) factory.factorFedoraAdapter();
		HttpHandlerFactoryImp factoredHttpHandlerFactory2 = (HttpHandlerFactoryImp) fedoraAdapter2
				.onlyForTestGetHttpHandlerFactory();
		assertSame(factoredHttpHandlerFactory, factoredHttpHandlerFactory2);
	}

	@Test
	public void testFedoraUrlSentToInstances() throws Exception {
		FedoraAdapterImp fedoraAdapter = (FedoraAdapterImp) factory.factorFedoraAdapter();
		assertEquals(fedoraAdapter.onlyForTestGetBaseUrl(), someFedoraUrl);
	}
}
