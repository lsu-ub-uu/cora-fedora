package se.uu.ub.cora.fedora;

import se.uu.ub.cora.fedora.internal.FedoraAdapterImp;
import se.uu.ub.cora.httphandler.HttpHandlerFactoryImp;

public class FedoraFactoryImp implements FedoraFactory {

	private HttpHandlerFactoryImp httpHandlerFactory;
	private String fedoraUrl;

	public FedoraFactoryImp(String fedoraUrl) {
		this.fedoraUrl = fedoraUrl;
		httpHandlerFactory = new HttpHandlerFactoryImp();
	}

	@Override
	public FedoraAdapter factorFedoraAdapter() {
		return new FedoraAdapterImp(httpHandlerFactory, fedoraUrl);
	}

	public String onlyForTestGetBaseUrl() {
		return fedoraUrl;
	}

}
